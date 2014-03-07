/**
 * Defines the main Triplifier class along with a few global configuration variables and generic functions.
 * Initializes the Triplifier system once the DOM is loaded.
 **/

// The global VocabularyManager and Triplifier objects.
var vocabularyManager, triplifier;

// The relation predicates supported by the Triplifier.
var relationPredicates = ["ro:derives_from", "bsc:depends_on", "bsc:alias_of", "bsc:related_to"];

var biscicolUrl = "http://biscicol.org/";
var triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here
//	biscicolUrl = "http://geomuseblade.colorado.edu/biscicol/",
//	triplifierUrl = "http://geomuseblade.colorado.edu/triplifier/";
//	biscicolUrl = "http://johns-macbook-air-2.local:8080/biscicol/",
//	triplifierUrl = "http://johns-macbook-air-2.local:8080/triplifier/";
//	Unfortunately, "localhost" doesn't appear to work with the "same origin" script policy (in Firefox, anyway).
//	triplifierUrl = "http://localhost:8080/triplifier/";


/**
 * This is the main function that sets everything up.  It is called once the DOM is loaded.
 **/
$(function() {
	// Create the VocabularyManager.
	var vocabkey = "triplifier.vocabularies"	// The local storage key for vocabularies.
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), vocabkey, alertError);

	// Create the main Triplifier object.
	triplifier = new Triplifier();
});


/**
 * The main Triplifier class.  Initializes and manages all of the principal components of the Triplifier.
 * The constructor creates all of the ProjectSection objects, creates the ProjectManager and ProjectUI,
 * sets up the main navigation buttons, and initializes the contextual help.
 **/
function Triplifier() {
	// The currently-open project.
	this.mainproject = null;

	// Create the project sections.  Each of these corresponds to one of the main sections in the UI.
	this.joinsPS = new JoinsTable($("#joinDiv"));
	this.entitiesPS = new EntitiesTable($("#entityDiv"));
	this.attributesPS = new AttributesTable($("#attributeDiv"));
	this.relationsPS = new RelationsTable($("#relationDiv"));
	this.triplifyPS = new ProjectSection($("#triplifyDiv"));
	this.dSsection = new DataSourceSection($('#dsDiv'), this.triplifyPS);

	// Set up the SectionManager object.  This coordinates the various ProjectSection objects, making
	// sure that their overall presentation in the UI remains consistent (e.g., ensuring that only one
	// ProjectSection can be active at a time).
	this.sectionmgr = new SectionManager();
	this.sectionmgr.addSections(this.dSsection, this.joinsPS, this.entitiesPS,
			this.attributesPS, this.relationsPS, this.triplifyPS);

	// Assign event handlers for the "triplify" section.
	var self = this;
	$("#getMapping").click(function() { self.sendProjData("rest/getMapping", "downloadFile", 'Generating mapping file...'); });
	$("#getTriples").click(function() { self.triplify("rest/getTriples", "downloadFile"); });
	// This was originally for sending the RDF directly to BiSciCol, but is disabled for now.
	//$("#sendToBiSciCol").click(function() { self.triplify("rest/getTriples", "sendToBiSciCol"); });
	// The Publish Component here is meant to assign a DOI to the triplified dataset, and store on server.
	//$("#publishDataset").click(function() { triplify("rest/getTriples", sendToBiSciCol); });

	$("#vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// Set event handlers for the navigation buttons.
	// Notice that we also explicitly set the buttons not to be disabled.  This shouldn't be necessary, but it
	// seems that Firefox will occasionally disable some of these buttons for no apparent reason.  Setting the
	// disabled property here seems to fix the problem.
	$("#dsDiv input.next").click(function() { self.joinsPS.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.back').click(function() { self.dSsection.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.next').click(function() { self.entitiesPS.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.back').click(function() { self.joinsPS.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.next').click(function() { self.attributesPS.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.back').click(function() { self.entitiesPS.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.next').click(function() { self.relationsPS.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.back').click(function() { self.attributesPS.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.next').click(function() { self.triplifyPS.setActive(true); }).prop("disabled", false);
	$('#triplifyDiv input.back').click(function() { self.relationsPS.setActive(true); }).prop("disabled", false);

	// Create a ProjectManager and associate it with a ProjectUI.
	var projman = new ProjectManager();
	this.projUI = new ProjectUI($("#projects"), projman);

	// Provide an observer for selection changes in the ProjectUI.  The ProjectUI will call the method
	// projectSelectionChanged() to notify us when the project selection changes.
	this.projUI.registerObserver(this);

	// Set the default project selection in the UI.
	this.projUI.selectDefaultProject();
	//console.log(this.mainproject);

	// Set up the contextual popup help.
	this.helpmgr = new ContextHelpManager('helpmsg');
	this.defineHelpMessages();
}

/**
 * Define the contextual help messages.
 **/
Triplifier.prototype.defineHelpMessages = function() {
	// Data source.
	this.helpmgr.setHelpItem('datasource_help', '<p>The data source is where your original data is located.  It can be a database, such as PostgreSQL, or a data file.</p><p>The Triplifier supports a number of common data file formats, including Excel and OpenOffice spreadsheets, CSV files, and Darwin Core Archives.</p>');
	// Joins.
	this.helpmgr.setHelpItem('joins_help', '<p>If any of the tables in your source data should be connected through joins, you can define the joins here.  The "foreign key" in one table should match the "primary key" in another table.</p>');
	// Concepts.
	this.helpmgr.setHelpItem('concepts_help', '<p>This section links identifiers in your data with a well-known representation of reality (“concept”).  Identifiers are unique and represent physical material, digital representatives of physical material (such as photographs), processes, or metadata.  Concepts are defined through a standardized vocabulary.  You can think of concepts as indicating what "kind" of information your identifiers represent.</p>' +
	'<p>The Triplifier utilizes Darwin Core concepts.  The simple Darwin Core includes six core classes or concepts: <em>Occurrence</em>, <em>Event</em>, <em>Location</em>, <em>GeologicalContext</em>, <em>Identification</em>, and <em>Taxon</em>.  <em>Occurrence</em> is information pertaining to evidence of occurence in nature, in a collection or dataset, such as physical specimens.  <em>Event</em> describes the information related to actions (e.g. collecting) during a specific time and at a particular spot. <em>Location</em> is information describing spatial region or named place.  <em>GeologicalContext</em> refers to information about location and time for fossil (and modern) material such as stratigraphy.  <em>Identification</em> refers to information about how taxonomic designations were made.  <em>Taxon</em> refers to the information pertaining to taxon names, usage and concepts.  If your data is not already in Darwin Core format, you will need to choose which identifiers in your dataset map to which Darwin Core concepts.</p>');
	// Concept identifiers.
	this.helpmgr.setHelpItem('IDs_help', '<p>For the Triplifier (and the Semantic Web) to work properly, it is very important that each subject of an RDF triple has its own, unique identifier.  The "ID" columns commonly used in databases and other storage formats often do not meet this requirement, because ID numbers can be repeated from table to table.</p>' +
	'<p>If the table rows in your source data already have "globally" unique identifiers, you can check the checkbox in this column and the Triplifier will use your identifiers directly.</p>' +
	'<p>If your identifiers are not guaranteed to be unique, then you should leave this checkbox unchecked.  In this case, the Triplifier will use the table name and ID column name to construct a unique identifier for each row of the data.  If you are unsure, leave the box unchecked.</p>');
	// Attributes.
	this.helpmgr.setHelpItem('attributes_help', '<p>Attributes provide information about the concepts you defined in step 3.  For example, if you defined an "Occurence" concept for physical specimens, then you might define attributes that describe each specimen\'s sex, life stage, and when it was collected.  In general, attributes attach properties, or metadata, to concepts.</p>' +
	'<p>In the Triplifier, attributes are Darwin Core terms that describe Darwin Core classes or concepts.  The current, ratified Darwin Core has 159 total terms.  As with concepts, if your data is not already in Darwin Core format, you will need to decide how the columns in your data map to Darwin Core terms.  This can be challenging, and we refer you to <a href="http://rs.tdwg.org/dwc/terms/">http://rs.tdwg.org/dwc/terms/</a> for a full list of terms.  You do not need to map every column in your data to a Darwin Core term.  Columns that you do not map will simply "pass through" the system and not be triplified.</p>');
	// Concept relations.
	this.helpmgr.setHelpItem('relations_help', '<p>Concept relations are key to make the Semantic Web and linked data work.  Concept relations define how the different concepts in your data are connected to one another.<p>' +
	'<p>The Triplifier allows four different relationships between concepts to be expressed: <em>derives_from</em>, <em>depends_on</em>, <em>related_to</em>, and <em>alias_of</em>.  The first three are by far the most commonly used.  What do these terms mean?  Briefly, derives_from indicates that one thing was derived from another (e.g., a tissue sample derives_from a specimen), depends_on indicates that one thing could not exist without another (e.g., an identification depends_on a particular specimen), related_to indicates a non-dependent relationship (e.g., a specimen is related_to a taxon), and alias_of indicates that two things are really the same thing.');
	// Triplify.
	this.helpmgr.setHelpItem('triplify_help', '<p>Triplify is the fun part, because you finally get to see what your data look like as RDF triples.  (That does sound fun, doesn\'t it?)</p>' +
	'<p>You have several options here.  "Get Mapping" lets you download a <a href="http://d2rq.org/">D2RQ</a> mapping file that includes all of the technical details about how your source data maps to an RDF representation.  Unless you use D2RQ for other purposes or need to tweak the mapping by hand, you will probably not be interested in this.  However, these mapping files can also be used with the command-line version of the Triplifier and so might be useful to you in that context.</p>' +
	'<p>"Get Triples" lets you download a file of your entire data set as RDF triples.  If your goal was to convert your data to RDF, this is the button you want to click.</p>' +
	'<p>You can also choose the output format for your triples file.   You can choose N-Triples or Turtle format, which are both formats for RDF data, or the Graphvis DOT format, which is useful for producing visualizations of your data.  Generating N-Triples is very fast, so it is the default output format and recommended for larger datasets.  Turtle output is more human-friendly and recommended for smaller datasets where you\'d like to be able to read the output.  If you want to use Graphviz to create a graphical representation of your data, choose the DOT format.</p>');
	//'<p>We\'re still figuring out exactly what the last two buttons will do, but the general idea is that they will allow you to send your data directly to the BiSciCol system so that they become searchable and linkable with millions of other pieces of biological data.</p>');
}

/**
 * Respond to project selection changes from the ProjectUI.  When a new project is selected
 * in the ProjectUI, this method makes sure that the rest of the UI is updated to work with
 * the newly-selected project.
 **/
Triplifier.prototype.projectSelectionChanged = function(project) {
	//alert("selection changed: " + project.getName());

	//alert('main project set');
	this.mainproject = project;

	// Very few of the sections are strictly required in order to triplify input data, but at the very
	// least, the user needs to provide a data source and define one concept.  So, we need to check if
	// the project has a valid data source and if any concepts have been defined, and disable the "Next"
	// buttons if necessary.
	if (!this.mainproject.schema.length) {
		// Disable all sections (except for the Data Source section).
		$("#dsDiv input.next").prop('disabled', true);
		this.sectionmgr.setSectionsEnabled(false, this.joinsPS, this.entitiesPS, this.attributesPS, this.relationsPS, this.triplifyPS);
	} else if (!this.mainproject.entities.length) {
		// Disable Attributes, Concept Relations, and Triplify.
		$("#dsDiv input.next").prop('disabled', false);
		$('#entityDiv input.next').prop('disabled', true);
		this.sectionmgr.setSectionsEnabled(false, this.attributesPS, this.relationsPS, this.triplifyPS);
		this.sectionmgr.setSectionsEnabled(true, this.joinsPS, this.entitiesPS);
	} else {
		// Enable all sections.
		$("#dsDiv input.next").prop('disabled', false);
		$('#entityDiv input.next').prop('disabled', false);
		this.sectionmgr.setSectionsEnabled(true, this.joinsPS, this.entitiesPS, this.attributesPS, this.relationsPS, this.triplifyPS);
	}

	// We want to be notified of project changes so we can update the state of the project sections as
	// needed.  Projects will call the projectPropertyChanged() method to notify us of changes.
	this.mainproject.registerObserver(this);

	this.updateProjectSections();
}

/**
 * Responds to property changes in the currently-open project.  When a property of this.mainproject is
 * modified, this method checks which property was modified and then disables or enables user
 * access to project sections as needed.
 *
 * Very few of the sections are strictly required in order to triplify input data, but at the very
 * least, the user needs to provide a data source and define one concept.  So, if no data source
 * is specified, then the remaining sections will be inaccessible, and if no concepts are specified,
 * then sections 4-6 will be inaccessible.
 **/
Triplifier.prototype.projectPropertyChanged = function(project, propname) {
	//alert("changed: " + propname);
	
	if (propname == 'entities') {
		// If concepts (entities) were changed, update the "Next" button state accordingly, and
		// set which sections are enabled.
		if (!this.mainproject.entities.length) {
			$('#entityDiv input.next').prop('disabled', true);
			this.sectionmgr.setSectionsEnabled(false, this.attributesPS, this.relationsPS, this.triplifyPS);
		}
		else {
			$('#entityDiv input.next').prop('disabled', false);
			this.sectionmgr.setSectionsEnabled(true, this.attributesPS, this.relationsPS, this.triplifyPS);
		}
	} else if (propname == 'schema') {
		// If the data source was changed, update the "Next" button state accordingly, and set
		// which sections are enabled.
		if (!this.mainproject.schema.length) {
			$("#dsDiv input.next").prop('disabled', true);
			this.sectionmgr.setSectionsEnabled(false, this.joinsPS, this.entitiesPS, this.attributesPS, this.relationsPS, this.triplifyPS);
		}
		else {
			$("#dsDiv input.next").prop('disabled', false);
			this.sectionmgr.setSectionsEnabled(true, this.joinsPS, this.entitiesPS);
			if (this.mainproject.entities.length)
				this.sectionmgr.setSectionsEnabled(true, this.attributesPS, this.relationsPS, this.triplifyPS);
		}
	}	
}

/**
 * Updates each of the ProjectSections to work with the main project and activates/deactivates sections
 * as needed depending on the project state.
 **/
Triplifier.prototype.updateProjectSections  = function() {	
	this.dSsection.setProject(this.mainproject);
	this.joinsPS.setProject(this.mainproject, 'joins');
	// update joins, delete invalid (not in schema)
	//this.joinsPS.removeMatching(function(join) {
	//	return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	//});
	this.entitiesPS.setProject(this.mainproject, 'entities');
	this.attributesPS.setProject(this.mainproject, 'attributes');
	this.relationsPS.setProject(this.mainproject, 'relations');

	// Activate/deactivate each section depending on the project state.  Note the use of "!!" to ensure
	// we have a true boolean value.
	this.dSsection.setActive(!this.mainproject.schema.length); 
	this.joinsPS.setActive(!!this.mainproject.schema.length && !this.mainproject.entities.length && !this.mainproject.relations.length);
	this.entitiesPS.setActive(!!this.mainproject.entities.length && !this.mainproject.attributes.length && !this.mainproject.relations.length)
	this.attributesPS.setActive(!!this.mainproject.attributes.length && !this.mainproject.relations.length)
	this.relationsPS.setActive(!!this.mainproject.relations.length)
	this.triplifyPS.setActive(false);
}

/**
 * Sends the current project's data to the REST method for generating RDF triples.
 *
 * @param url The REST method to call.
 * @param successFn The name of a method to call after receiving a success response from the server.
 **/
Triplifier.prototype.triplify = function(url, successFn) {
	setStatus('Triplifying data source...', true);

	// Set the dataseturi to link to top level object on the server
	var dataseturi = {};
	dataseturi.name = this.dSsection.getDataSourceName();

	// Get the output format.
	var outformat = $("select[name='rdfFormat']").val();

	var self = this;
	$.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify({
		    mapping: {
		      connection: this.mainproject.connection,
		      joins: this.mainproject.joins,
		      entities: this.mainproject.getCombinedEntitiesAndAttributes(),
		      relations: this.mainproject.relations,
		      dataseturi: dataseturi
		    },
		    outputformat: outformat
		}),
		contentType: "application/json; charset=utf-8",
		dataType: "text",
		success: function(url) { self[successFn](url); },
		error: alertError
	});
}

/**
 * Sends the current project's data to the REST method at the specified URL.
 *
 * @param url The REST method to call.
 * @param successFn The name of a method to call after receiving a success response from the server.
 * @param waitmsg The status message to display while waiting for the request to return.
 **/
Triplifier.prototype.sendProjData = function(url, successFn, waitmsg) {
	setStatus(waitmsg, false);

	// Set the dataseturi to link to top level object on the server
	var dataseturi = {};
	dataseturi.name = this.dSsection.getDataSourceName();

	// Get the output format.
	var outformat = $("input[type='radio'][name='rdfFormat']:checked").val();

	var self = this;
	$.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify({
		    connection: this.mainproject.connection,
		    joins: this.mainproject.joins,
		    entities: this.mainproject.getCombinedEntitiesAndAttributes(),
		    relations: this.mainproject.relations,
		    dataseturi: dataseturi
		}),
		contentType: "application/json; charset=utf-8",
		dataType: "text",
		success: function(url) { self[successFn](url); },
		error: alertError
	});
}

/**
 * Opens a new window displaying the results of a successful REST call.
 **/
Triplifier.prototype.downloadFile = function(url) {
	setStatus("");
	window.open(url);
}

/**
 * After a successful call to the getTriples REST method, this function will attempt to
 * send the resulting triples URL to the BiSciCol system for display.  This function should
 * be called as a result of a call to the triplify() method.
 **/
Triplifier.prototype.sendToBiSciCol = function(url) {
	var sendToBiSciColForm = document.getElementById("sendToBiSciColForm");
	// sendToBiSciColForm.url.value = "http://" + location.host + location.pathname.substr(0, location.pathname.lastIndexOf("/")) + "/" + url;

	// [hack] When file on triplifier is accessed from biscicol on the same server then port
	// forwarding won't work so the port is set here.
	sendToBiSciColForm.url.value = triplifierUrl + url;
	var self = this;
	$("#uploadTarget").one("load", function() { self.afterBiSciCol(); });
	sendToBiSciColForm.submit();
}

/**
 * Determines if an attempt to upload triples to the BiSciCol system was successful and
 * displays an appropriate status message.  Note that this function will fail if the location
 * of the triplifier (as specified by the global triplifierUrl) and the UI page are on
 * different domains.  In that case, because of the "same origin" policy (to prevent cross-site
 * scripting attacks), the browser will throw an error when attempting to access the
 * uploadTarget frame's DOM.
 **/
Triplifier.prototype.afterBiSciCol = function() {
	setStatus("");

	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data))
		window.open(biscicolUrl + "?model=" + data.substr(1, data.length-2) + "&id=" + getDataSourceName());
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
}


// Define a few methods to extend the Storage class.
Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
};

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return isJson(value) ? JSON.parse(value) : value;
};

// Define a simple visibility toggle method to extend JQuery.
jQuery.prototype.fadeToggle = function(fadeIn) {
	if (fadeIn)
		this.fadeIn();
	else
		this.fadeOut();
};


// The remaining functions are all generic, global functions that provide system-wide utilities.

/**
 * A generic function to display a status message to the user.  If status is a non-empty
 * string, then the status message is displayed.  Otherwise, the status message area is
 * hidden from view.  If showspinner is true, a "spinner" image is displayed.
 **/
function setStatus(statusmsg, showspinner) {
	var html = '<p>' + statusmsg + '</p>';
	if (showspinner)
		html = '<div><img src="images/spinner.gif" /></div>' + html;

	$("#status").html(html);
	$("#status, #overlay").fadeToggle(statusmsg);
}

/**
 * A generic function to display an error message following an AJAX request.
 **/
function alertError(xhr, status, error) {
	setStatus("");
	alert(status + ': ' + error + '\n' + 'response status: ' + xhr.status + '\n' + xhr.responseText)
	//alert(status + (xhr.status==500 ? ":\n\n"+xhr.responseText : (error ? ": "+error : "")));
}

/**
 * Searches for an element in an array with a specified property that has a given value.
 * Can optionally also require that a second property has a specific value.
 **/
function indexOf(array, property, value, property2, value2) { 
	var result = -1;
	$.each(array, function(i, element) {
		if (element[property] == value && (!property2 || !value2 || element[property2] == value2)) {
			result = i;
			return false;
		}
	});
	return result;
}

/**
 * Verifies that a string of text is valid JSON.  This is not meant to be a thorough validator; it just
 * does some basic integrity checking.
 **/
function isJson(data) {
	if (!data)
		return false;
	var firstChar = data.charAt(0),
		lastChar = data.charAt(data.length-1);
	return firstChar=='{' && lastChar=='}'
			|| firstChar=='[' && lastChar==']'
			|| firstChar=='"' && lastChar=='"';
}

