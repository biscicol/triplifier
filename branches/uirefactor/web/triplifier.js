
// Define a few global variables.  There are far fewer globals in the system than previously,
// but nearly all globals could be eliminated by defining a central Triplifier class.  That
// will probably be a final step in refactoring the code, but it is not crucial at the moment.

// The currently-open project.
var mainproject;

// ProjectSection objects.  Each of these corresponds to one of the main sections in the UI.
var dSsection, joinsPS, entitiesPS, attributesPS, relationsPS, triplifyPS;
// SectionManager object.  This coordinates the various ProjectSection objects, making sure
// that their overall presentation in the UI remains consistent (e.g., ensuring that only one
// ProjectSection can be active at a time).
var sectionmgr;
// VocabularyManager object.
var vocabularyManager;

// Define the relation predicates supported by the Triplifier.
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
 * It creates all of the ProjectSection objects, creates the ProjectManager and ProjectUI,
 * sets up the main navigation buttons, and initializes the contextual help.
 **/
$(function() {
	// Create the VocabularyManager.
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), getStorageKey("vocabularies"), alertError);

	// Create the main project sections.
	joinsPS = new JoinsTable($("#joinDiv"));
	entitiesPS = new EntitiesTable($("#entityDiv"));
	attributesPS = new AttributesTable($("#attributeDiv"));
	relationsPS = new RelationsTable($("#relationDiv"));
	triplifyPS = new ProjectSection($("#triplifyDiv"));
	dSsection = new DataSourceSection($('#dsDiv'), triplifyPS);

	// Set up the SectionManager.
	sectionmgr = new SectionManager();
	sectionmgr.addSections(dSsection, joinsPS, entitiesPS, attributesPS, relationsPS, triplifyPS);

	// Assign event handlers for the "triplify" section.
	$("#getMapping").click(function() { triplify("rest/getMapping", downloadFile); });
	$("#getTriples").click(function() { triplify("rest/getTriples", downloadFile); });
	$("#sendToBiSciCol").click(function() { triplify("rest/getTriples", sendToBiSciCol); });
	// The Publish Component here is meant to assign a DOI to the triplified dataset, and store on server
	//$("#publishDataset").click(function() { triplify("rest/getTriples", sendToBiSciCol); });


	$("#vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// Set event handlers for the navigation buttons.
	// Notice that we also explicitly set the buttons not to be disabled.  This shouldn't be necessary, but it
	// seems that Firefox will occasionally disable some of these buttons for no apparent reason.  Setting the
	// disabled property here seems to fix the problem.
	$("#dsDiv input.next").click(function() { joinsPS.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.back').click(function() { dSsection.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.next').click(function() { entitiesPS.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.back').click(function() { joinsPS.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.next').click(function() { attributesPS.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.back').click(function() { entitiesPS.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.next').click(function() { relationsPS.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.back').click(function() { attributesPS.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.next').click(function() { triplifyPS.setActive(true); }).prop("disabled", false);
	$('#triplifyDiv input.back').click(function() { relationsPS.setActive(true); }).prop("disabled", false);

	// Create a ProjectManager and associate it with a ProjectUI.
	var projman = new ProjectManager();
	var projUI = new ProjectUI($("#projects"), projman);

	// Provide an observer for selection changes in the ProjectUI.
	obsobj = { projectSelectionChanged: projectSelectionChanged };
	projUI.registerObserver(obsobj);

	// Set the default project selection in the UI.
	projUI.selectDefaultProject();
	//console.log(mainproject);

	// Set up the contextual popup help.
	var helpmgr = new ContextHelpManager('helpmsg');
	defineHelpMessages(helpmgr);
});

/**
 * Define the contextual help messages.
 **/
function defineHelpMessages(helpmgr) {
	// Data source.
	helpmgr.setHelpItem('datasource_help', '<p>The data source is where your original data is located.  It can be a database, such as PostgreSQL, or a data file.</p><p>The Triplifier supports a number of common data file formats, including Excel and OpenOffice spreadsheets, CSV files, and Darwin Core Archives.</p>');
	// Joins.
	helpmgr.setHelpItem('joins_help', '<p>If any of the tables in your source data should be connected through joins, you can define the joins here.  The "foreign key" in one table should match the "primary key" in another table.</p>');
	// Concepts.
	helpmgr.setHelpItem('concepts_help', '<p>This section links identifiers in your data with a well-known representation of reality (“concept”).  Identifiers are unique and represent physical material, digital representatives of physical material (such as photographs), processes, or metadata.  Concepts are defined through a standardized terminology.  You can think of concepts as indicating what "kind" of information your identifiers represent.</p>' +
	'<p>The Triplifier utilizes Darwin Core concepts.  The simple Darwin Core includes six core classes or concepts: <em>Occurrence</em>, <em>Event</em>, <em>Location</em>, <em>GeologicalContext</em>, <em>Identification</em>, and <em>Taxon</em>.  <em>Occurrence</em> is information pertaining to evidence of occurence in nature, in a collection or dataset, such as physical specimens.  <em>Event</em> describes the information related to actions (e.g. collecting) during a specific time and at a particular spot. <em>Location</em> is information describing spatial region or named place.  <em>GeologicalContext</em> refers to information about location and time for fossil (and modern) material such as stratigraphy.  <em>Identification</em> refers to information about how taxonomic designations were made.  <em>Taxon</em> refers to the information pertaining to taxon names, usage and concepts.  If your data is not already in Darwin Core format, you will need to choose which identifiers in your dataset map to which Darwin Core concepts.</p>');
	// Concept identifiers.
	helpmgr.setHelpItem('IDs_help', '<p>For the Triplifier (and the Semantic Web) to work properly, it is very important that each subject of an RDF triple has its own, unique identifier.  The "ID" columns commonly used in databases and other storage formats often do not meet this requirement, because ID numbers can be repeated from table to table.</p>' +
	'<p>If the table rows in your source data already have "globally" unique identifiers, you can check the checkbox in this column and the Triplifier will use your identifiers directly.</p>' +
	'<p>If your identifiers are not guaranteed to be unique, then you should leave this checkbox unchecked.  In this case, the Triplifier will use the table name and ID column name to construct a unique identifier for each row of the data.  If you are unsure, leave the box unchecked.</p>');
	// Attributes.
	helpmgr.setHelpItem('attributes_help', '<p>Attributes provide information about the concepts you defined in step 3.  For example, if you defined an "Occurence" concept for physical specimens, then you might define attributes that describe each specimen\'s sex, life stage, and when it was collected.  In general, attributes attach properties, or metadata, to concepts.</p>' +
	'<p>In the Triplifier, attributes are Darwin Core terms that describe Darwin Core classes or concepts.  The current, ratified Darwin Core has 159 total terms.  As with concepts, if your data is not already in Darwin Core format, you will need to decide how the columns in your data map to Darwin Core terms.  This can be challenging, and we refer you to <a href="http://rs.tdwg.org/dwc/terms/">http://rs.tdwg.org/dwc/terms/</a> for a full list of terms.  You do not need to map every column in your data to a Darwin Core term.  Columns that you do not map will simply "pass through" the system and not be triplified.</p>');
	// Concept relations.
	helpmgr.setHelpItem('relations_help', '<p>Concept relations are key to make the semantic web and linked data work.  Concept relations define how the different concepts in your data are connected to one another.<p>' +
	'<p>BiSciCol allows two different relationships between terms to be expressed: <em>isSourceOf</em> and <em>isRelatedTo</em>.  <em>IsSourceOf</em> is a unidirectional relationship that is used whenever the physical material of one concept is derived in some way from the physical material of another concept.  For example, a specimen <em>isSourceOf</em> a tissue.  <em>IsRelatedTo</em> is bidirectional and is used for all other relationships between concepts.  For example, a collector <em>isRelatedTo</em> a collecting event.</p>');
	// Triplify.
	helpmgr.setHelpItem('triplify_help', '<p>Triplify is the fun part, because you finally get to see what your data looks like as RDF triples.  (That does sound fun, doesn\'t it?)</p>' +
	'<p>You have several options here.  "Get Mapping" lets you download a <a href="http://d2rq.org/">D2RQ</a> mapping file that includes all of the technical details about how your source data maps to an RDF representation.  Unless you use D2RQ for other purposes or need to tweak the mapping by hand, you will probably not be interested in this.</p>' +
	'<p>"Get Triples" lets you download an N3-format file of your entire data set as RDF triples.  If your goal was to convert your data to RDF, this is the button you want to click.</p>' +
	'<p>We\'re still figuring out exactly what the last two buttons will do, but the general idea is that they will allow you to send your data directly to the BiSciCol system so that they become searchable and linkable with millions of other pieces of biological data.</p>');
}

/**
 * Respond to project selection changes from the ProjectUI.  When a new project is selected
 * in the ProjectUI, this method makes sure that the rest of the UI is updated to work with
 * the newly-selected project.
 **/
function projectSelectionChanged(project) {
	//alert("selection changed: " + project.getName());

	//alert('main project set');
	mainproject = project;

	// Very few of the sections are strictly required in order to triplify input data, but at the very
	// least, the user needs to provide a data source and define one concept.  So, we need to check if
	// the project has a valid data source and if any concepts have been defined, and disable the "Next"
	// buttons if necessary.
	if (!mainproject.schema.length) {
		$("#dsDiv input.next").prop('disabled', true);
		sectionmgr.setSectionsEnabled(false, joinsPS, entitiesPS, attributesPS, relationsPS, triplifyPS);
	}
	if (!mainproject.entities.length) {
		$('#entityDiv input.next').prop('disabled', true);
		sectionmgr.setSectionsEnabled(false, attributesPS, relationsPS, triplifyPS);
	}

	// We want to be notified of project changes so we can update the state of the concepts "Next"
	// button as needed.  We need to create an object to act as a project observer.
	obsobj = { projectPropertyChanged: projectPropertyChanged };
	mainproject.registerObserver(obsobj);

	updateProjectSections();
}

/**
 * Responds to property changes in the currently-open project.  When a property of mainproject is
 * modified, this method checks which property was modified and then disables or enables user
 * access to project sections as needed.
 *
 * Very few of the sections are strictly required in order to triplify input data, but at the very
 * least, the user needs to provide a data source and define one concept.  So, if no data source
 * is specified, then the remaining sections will be inaccessible, and if no concepts are specified,
 * then sections 4-6 will be inaccessible.
 **/
function projectPropertyChanged(project, propname) {
	//alert("changed: " + propname);
	
	if (propname == 'entities') {
		// If concepts (entities) were changed, update the "Next" button state accordingly, and
		// set which sections are enabled.
		if (!mainproject.entities.length) {
			$('#entityDiv input.next').prop('disabled', true);
			sectionmgr.setSectionsEnabled(false, attributesPS, relationsPS, triplifyPS);
		}
		else {
			$('#entityDiv input.next').prop('disabled', false);
			sectionmgr.setSectionsEnabled(true, attributesPS, relationsPS, triplifyPS);
		}
	} else if (propname == 'schema') {
		// If the data source was changed, update the "Next" button state accordingly, and set
		// which sections are enabled.
		if (!mainproject.schema.length) {
			$("#dsDiv input.next").prop('disabled', true);
			sectionmgr.setSectionsEnabled(false, joinsPS, entitiesPS, attributesPS, relationsPS, triplifyPS);
		}
		else {
			$("#dsDiv input.next").prop('disabled', false);
			sectionmgr.setSectionsEnabled(true, joinsPS, entitiesPS);
			if (mainproject.entities.length)
				sectionmgr.setSectionsEnabled(true, attributesPS, relationsPS, triplifyPS);
		}
	}	
}

function updateProjectSections() {	
	dSsection.setProject(mainproject);
	joinsPS.setProject(mainproject, 'joins');
	// update joins, delete invalid (not in schema)
	//joinsPS.removeMatching(function(join) {
	//	return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	//});
	entitiesPS.setProject(mainproject, 'entities');
	attributesPS.setProject(mainproject, 'attributes');
	relationsPS.setProject(mainproject, 'relations');

	// Activate/deactivate each section depending on the project state.  Note the use of "!!" to ensure
	// we have a true boolean value.
	dSsection.setActive(!mainproject.schema.length); 
	joinsPS.setActive(!!mainproject.schema.length && !mainproject.entities.length && !mainproject.relations.length);
	entitiesPS.setActive(!!mainproject.entities.length && !mainproject.attributes.length && !mainproject.relations.length)
	attributesPS.setActive(!!mainproject.attributes.length && !mainproject.relations.length)
	relationsPS.setActive(!!mainproject.relations.length)
	triplifyPS.setActive(false);
}

function alertError(xhr, status, error) {
	setStatus("");
	alert(status + ': ' + error + '\n' + 'response status: ' + xhr.status + '\n' + xhr.responseText)
	//alert(status + (xhr.status==500 ? ":\n\n"+xhr.responseText : (error ? ": "+error : "")));
}

/**
 * Opens a new window displaying the results of a successful REST call.
 **/
function downloadFile(url) {
	setStatus("");
	window.open(url);
}

/**
 * Sends the current project's data to the REST method at the specified URL.
 *
 * @param url The REST method to call.
 * @param successFn Function to call after receiving a success response from the server.
 **/
function triplify(url, successFn) {
	setStatus("Triplifying Data Source...");

	// Set the dataseturi to link to top level object on the server
	var dataseturi = {};
	dataseturi.name = dSsection.getDataSourceName();

	$.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify({
		    connection: mainproject.connection,
		    joins: mainproject.joins,
		    entities: mainproject.getCombinedEntitiesAndAttributes(),
		    relations: mainproject.relations,
		    dataseturi:dataseturi
		}),
		contentType: "application/json; charset=utf-8",
		dataType: "text",
		success: successFn,
		error: alertError
	});
}

/**
 * After a successful call to the getTriples REST method, this function will attempt to
 * send the resulting triples URL to the BiSciCol system for display.  This function should
 * by called as a result of a call to the triplify() method.
 **/
function sendToBiSciCol(url) {
	var sendToBiSciColForm = document.getElementById("sendToBiSciColForm");
	// sendToBiSciColForm.url.value = "http://" + location.host + location.pathname.substr(0, location.pathname.lastIndexOf("/")) + "/" + url;

	// [hack] When file on triplifier is accessed from biscicol on the same server then port
	// forwarding won't work so the port is set here.
	sendToBiSciColForm.url.value = triplifierUrl + url;
	$("#uploadTarget").one("load", afterBiSciCol);
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
function afterBiSciCol() {
	setStatus("");

	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data))
		window.open(biscicolUrl + "?model=" + data.substr(1, data.length-2) + "&id=" + getDataSourceName());
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
}

function setStatus(status) {
	$("#status").html(status);
	$("#status, #overlay").fadeToggle(status);
}

/*function readMapping(inspection) {
	setStatus("");
	mainproject.setProperty('dateTime', inspection["dateTime"]);
	mainproject.setProperty('connection', inspection["connection"]);
	mainproject.setProperty('schema', inspection["schema"]);
	if (!mainproject.joins || !mainproject.joins.length)
		mainproject.setProperty('joins', inspection["joins"]);
	if (!mainproject.entities || !mainproject.entities.length)
		mainproject.setProperty('entities', inspection["entities"]);
	if (!mainproject.relations || !mainproject.relations.length)
		mainproject.setProperty('relations', inspection["relations"]);

	displayMapping();
}*/

/*function displayMapping() {
	if (!mainproject.connection) {
		mainproject.dateTime = "";
		mainproject.connection = {};
		mainproject.schema = [];
		mainproject.joins = [];
		mainproject.entities = [];
		mainproject.relations = [];
	}

	updateSchemaUI();

	updateFlexTables();
}*/

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

function isJson(data) {
	if (!data)
		return false;
	var firstChar = data.charAt(0),
		lastChar = data.charAt(data.length-1);
	return firstChar=='{' && lastChar=='}'
			|| firstChar=='[' && lastChar==']'
			|| firstChar=='"' && lastChar=='"';
}
	
function getStorageKey(key, prj) {
	return "triplifier." + key + (prj ? "." + prj : "");
}
	
Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
};

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return isJson(value) ? JSON.parse(value) : value;
};

jQuery.prototype.fadeToggle = function(fadeIn) {
	if (fadeIn)
		this.fadeIn();
	else
		this.fadeOut();
};
