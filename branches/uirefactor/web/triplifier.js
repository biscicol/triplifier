
// The currently-open project.
var mainproject;

// ProjectSection objects.
var dSsection, joinsPT, entitiesPT, attributesPT, relationsPT, triplifyPT;
// SectionManager object.
var sectionmgr;

var	vocabularyManager,
	relationPredicates = ["ma:isSourceOf", "ma:isRelatedTo"],
	biscicolUrl = "http://biscicol.org/",
	triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here

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
	// VocabularyManager must be created before FlexTables
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), getStorageKey("vocabularies"), alertError);

	// Create the main project sections.
	dSsection = new DataSourceSection($('#dsDiv'));
	joinsPT = new JoinsTable($("#joinDiv"));
	entitiesPT = new EntitiesTable($("#entityDiv"));
	attributesPT = new AttributesTable($("#attributeDiv"));
	relationsPT = new RelationsTable($("#relationDiv"));
	triplifyPT = new ProjectSection($("#triplifyDiv"));

	// Set up the SectionManager.
	sectionmgr = new SectionManager();
	sectionmgr.addSections(dSsection, joinsPT, entitiesPT, attributesPT, relationsPT, triplifyPT);

	// Assign event handlers for the "triplify" section.
	$("#getMapping").click(function() { triplify("rest/getMapping", downloadFile); });
	$("#getTriples").click(function() { triplify("rest/getTriples", downloadFile); });
	$("#sendToBiSciCol").click(function() { triplify("rest/getTriples", sendToBiSciCol); });
	// The Publish Component here is meant to assign a DOI to the triplified dataset, and store on server
	$("#publishDataset").click(function() { triplify("rest/getTriples", sendToBiSciCol); });


	$("#vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// Set event handlers for the navigation buttons.
	// Notice that we also explicitly set the buttons not to be disabled.  This shouldn't be necessary, but it
	// seems that Firefox will occasionally disable some of these buttons for no apparent reason.  Setting the
	// disabled property here seems to fix the problem.
	$("#dsDiv input.next").click(function() { joinsPT.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.back').click(function() { dSsection.setActive(true); }).prop("disabled", false);
	$('#joinDiv input.next').click(function() { entitiesPT.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.back').click(function() { joinsPT.setActive(true); }).prop("disabled", false);
	$('#entityDiv input.next').click(function() { attributesPT.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.back').click(function() { entitiesPT.setActive(true); }).prop("disabled", false);
	$('#attributeDiv input.next').click(function() { relationsPT.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.back').click(function() { attributesPT.setActive(true); }).prop("disabled", false);
	$('#relationDiv input.next').click(function() { triplifyPT.setActive(true); }).prop("disabled", false);
	$('#triplifyDiv input.back').click(function() { relationsPT.setActive(true); }).prop("disabled", false);

	// Create a ProjectManager and associate it with a ProjectUI.
	var projman = new ProjectManager();
	var projUI = new ProjectUI($("#projects"), projman);

	// Provide an observer for selection changes in the ProjectUI.
	obsobj = { projectSelectionChanged: projectSelectionChanged };
	projUI.registerObserver(obsobj);

	// Set the default project selection in the UI.
	projUI.selectDefaultProject();

	// Set up the contextual popup help.
	var helpmgr = new ContextHelpManager('helpmsg');
	defineHelpMessages(helpmgr);
});

/**
 * Define the contextual help messages.
 **/
function defineHelpMessages(helpmgr) {
	helpmgr.setHelpItem('datasource_help', '<p>The data source is where your original data is located.  It can be a database, such as PostgreSQL, or a data file.</p><p>The Triplifier supports a number of popular data file formats, including Excel and OpenOffice spreadsheets, CSV files, and Darwin Core Archives.</p>');
	helpmgr.setHelpItem('joins_help', '<p>If any of the tables in your source data should be connected through joins, you can define the joins here.  The "foreign key" in one table should match the "primary key" in another table.</p>');
	helpmgr.setHelpItem('concepts_help', '<p>This section links specific Identifiers with a well known representation of reality (“Concept”).  Identifiers are unique and represent physical material, digital surrogates for physical material, processes, or metadata.  Concepts are defined through either a standardized terminology (encoded in RDF) or defined by a structured Ontology. </p>');
	helpmgr.setHelpItem('attributes_help', '<p>Attributes attach properties, or metadata, to concepts defined in the Concept section (e.g. Specimen has “lifeStage” first instar)</p>');
	helpmgr.setHelpItem('relations_help', '<p>Concepts can be related to other concepts by one of two relationship types: “isSourceOf” and “isRelatedTo”.  “isSourceOf” is used whenever two concepts refer to physical material that is substantially derived from some other physical material (e.g. Tissue derived from a Specimen).  “isRelatedTo” is used for all other relationships between concepts (e.g. Photograph of a Specimen).</p>');
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
		sectionmgr.setSectionsEnabled(false, joinsPT, entitiesPT, attributesPT, relationsPT, triplifyPT);
	}
	if (!mainproject.entities.length) {
		$('#entityDiv input.next').prop('disabled', true);
		sectionmgr.setSectionsEnabled(false, attributesPT, relationsPT, triplifyPT);
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
			sectionmgr.setSectionsEnabled(false, attributesPT, relationsPT, triplifyPT);
		}
		else {
			$('#entityDiv input.next').prop('disabled', false);
			sectionmgr.setSectionsEnabled(true, attributesPT, relationsPT, triplifyPT);
		}
	} else if (propname == 'schema') {
		// If the data source was changed, update the "Next" button state accordingly, and set
		// which sections are enabled.
		if (!mainproject.schema.length) {
			$("#dsDiv input.next").prop('disabled', true);
			sectionmgr.setSectionsEnabled(false, joinsPT, entitiesPT, attributesPT, relationsPT, triplifyPT);
		}
		else {
			$("#dsDiv input.next").prop('disabled', false);
			sectionmgr.setSectionsEnabled(true, joinsPT, entitiesPT);
			if (mainproject.entities.length)
				sectionmgr.setSectionsEnabled(true, attributesPT, relationsPT, triplifyPT);
		}
	}	
}

function updateProjectSections() {	
	dSsection.setProject(mainproject);
	joinsPT.setProject(mainproject, 'joins');
	// update joins, delete invalid (not in schema)
	//joinsPT.removeMatching(function(join) {
	//	return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	//});
	entitiesPT.setProject(mainproject, 'entities');
	attributesPT.setProject(mainproject, 'attributes');
	relationsPT.setProject(mainproject, 'relations');

	// Activate/deactivate each section depending on the project state.  Note the use of "!!" to ensure
	// we have a true boolean value.
	dSsection.setActive(!mainproject.schema.length); 
	joinsPT.setActive(!!mainproject.schema.length && !mainproject.entities.length && !mainproject.relations.length);
	entitiesPT.setActive(!!mainproject.entities.length && !mainproject.attributes.length && !mainproject.relations.length)
	attributesPT.setActive(!!mainproject.attributes.length && !mainproject.relations.length)
	relationsPT.setActive(!!mainproject.relations.length)
	triplifyPT.setActive(false);
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
	dataseturi.name = getDataSourceName();

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
