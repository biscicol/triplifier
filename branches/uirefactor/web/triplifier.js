
var mainproject;	// The currently-open project.

var joinsPT, entitiesPT, attributesPT, relationsPT, triplifyPT;	// EditableTable objects.

var	vocabularyManager,
	dbSourceTrTemplate,
	relationPredicates = ["ma:isSourceOf", "ma:isRelatedTo"],
	biscicolUrl = "http://biscicol.org/",
	triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here

//	biscicolUrl = "http://geomuseblade.colorado.edu/biscicol/",
//	triplifierUrl = "http://geomuseblade.colorado.edu/triplifier/";
//	biscicolUrl = "http://johns-macbook-air-2.local:8080/biscicol/",
//	triplifierUrl = "http://johns-macbook-air-2.local:8080/triplifier/";
//	Unfortunately, "localhost" doesn't appear to work with the "same origin" script policy (in Firefox, anyway).
//	triplifierUrl = "http://localhost:8080/triplifier/";

// execute once the DOM has loaded
$(function() {
	dbSourceTrTemplate = $("#schemaTable > tbody").children(":last").remove();
   	
	// VocabularyManager must be created before FlexTables
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), getStorageKey("vocabularies"), alertError);

	// Create the project tables (this also removes blank DOM elements).
	joinsPT = new JoinsTable($("#joinDiv"));
	entitiesPT = new EntitiesTable($("#entityDiv"));
	attributesPT = new AttributesTable($("#attributeDiv"));
	relationsPT = new RelationsTable($("#relationDiv"));
	// Use an EditableTable for the triplifier <div>, too, although this is merely to get styles
	// and activate/deactivate capabilities.  There is no table inside the div, so the authoring
	// functionality of EditableTable is unavailable.
	triplifyPT = new EditableTable($("#triplifyDiv"));

	// assign event handlers
	$("#dbForm").submit(inspect);
	$("#uploadForm").submit(uploadData);

	// Assign event handlers for the "triplify" section.
	$("#getMapping").click(function() { triplify("rest/getMapping", downloadFile); });
	$("#getTriples").click(function() { triplify("rest/getTriples", downloadFile); });
	$("#sendToBiSciCol").click(function() { triplify("rest/getTriples", sendToBiSciCol); });

	$("#dbForm, #uploadForm, #dsDiv > input.next, #vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// Set event handlers for the navigation buttons.
	// Notice that we also explicitly set the buttons not to be disabled.  This shouldn't be necessary, but it
	// seems that Firefox will occasionally disable some of these buttons for no apparent reason.  Setting the
	// disabled property here seems to fix the problem.
	$("#dsDiv > input.next").click(dSNextButtonClicked).prop("disabled", false);
	$('#joinDiv input.back').click(joinsBackButtonClicked).prop("disabled", false);
	$('#joinDiv input.next').click(function() { navButtonClicked(entitiesPT, joinsPT); }).prop("disabled", false);
	$('#entityDiv input.back').click(function() { navButtonClicked(joinsPT, entitiesPT); }).prop("disabled", false);
	$('#entityDiv input.next').click(function() { navButtonClicked(attributesPT, entitiesPT); }).prop("disabled", false);
	$('#attributeDiv input.back').click(function() { navButtonClicked(entitiesPT, attributesPT); }).prop("disabled", false);
	$('#attributeDiv input.next').click(function() { navButtonClicked(relationsPT, attributesPT); }).prop("disabled", false);
	$('#relationDiv input.back').click(function() { navButtonClicked(attributesPT, relationsPT); }).prop("disabled", false);
	$('#relationDiv input.next').click(function() { navButtonClicked(triplifyPT, relationsPT); }).prop("disabled", false);
	$('#triplifyDiv input.back').click(function() { navButtonClicked(relationsPT, triplifyPT); }).prop("disabled", false);

	// Create a ProjectManager and associate it with a ProjectUI.
	var projman = new ProjectManager();
	var projUI = new ProjectUI($("#projects"), projman);
});

/**
 * Set the currently open project.
 **/
function setMainProject(project) {
	//alert('main project set');
	mainproject = project;

	// Very few of the sections are strictly required in order to triplify input data, but at the very
	// least, the user needs to define one concept.  So, we need to check if any concepts have been
	// defined, and if not, disable the "Next" button for the concepts.
	if (!mainproject.entities.length)
		$('#entityDiv input.next').prop('disabled', true);

	// We want to be notified of project changes so we can update the state of the concepts "Next"
	// button as needed.  We need to create an object to act as a project observer.
	obsobj = { projectPropertyChanged: projectPropertyChanged };
	mainproject.registerObserver(obsobj);

	updateSchemaUI();

	updateFlexTables();
}

/**
 * Respond to property changes in the currently-open project.
 **/
function projectPropertyChanged(project, propname) {
	//alert("changed: " + propname);
	
	// If concepts (entities) were changed, update the "Next" button state accordingly.
	if (propname == 'entities') {
		if (!mainproject.entities.length)
			$('#entityDiv input.next').prop('disabled', true);
		else
			$('#entityDiv input.next').prop('disabled', false);
	}
}

/**
 * A generic function for handling when one of the "Back" or "Next" navigation
 * buttons is clicked.  Activates one ProjectTable (activatePT) and
 * deactivates another (deactivatePT).
 **/
function navButtonClicked(activatePT, deactivatePT) {
	deactivatePT.setActive(false);
	activatePT.setActive(true);
	return true;
}

function dSNextButtonClicked() {
	activateDS(true);
	joinsPT.setActive(true);
	return true;
}

function joinsBackButtonClicked() {
	joinsPT.setActive(false);
	activateDS();
	return true;
}

function updateSchemaUI() {
	// update schema
	$("#dsDescription").html(getDataSourceName() + ", accessed: " + mainproject.dateTime);
	var schemaTable = $("#schemaTable");
	var columns;
	schemaTable.children("tbody").children().remove();
	$.each(mainproject.schema, function(i, table) {
		columns = "";
		$.each(table.columns, function(j, column) { 
			columns += column + ($.inArray(column, table.pkColumns) >= 0 ? "*" : "") + ", ";
		});
		columns = columns.substr(0, columns.length - 2); // remove last comma
		dbSourceTrTemplate.clone().children()
			.first().html(table.name) // write table name to first td
			.next().html(columns) // write columns to second td
			.end().end().end().appendTo(schemaTable);
	});
}

function updateFlexTables() {	
	// update data source
	$.each($("#dbForm").get(0), function(i, element) {
		if (element.type != "submit")
			element.value = (mainproject.connection.system == "sqlite" ? "" : (mainproject.connection[element.name] || ""));
	});

	// update joins, delete invalid (not in schema)
	joinsPT.setProject(mainproject, 'joins');
	//joinsPT.removeMatching(function(join) {
	//	return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	//});
	entitiesPT.setProject(mainproject, 'entities');
	attributesPT.setProject(mainproject, 'attributes');
	relationsPT.setProject(mainproject, 'relations');

	// Activate/deactivate each section depending on the project state.  Note the use of "!!" to ensure
	// we have a true boolean value.
	activateDS(mainproject.schema.length); 
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

function uploadData() {
	if (!this.file.value) {
		alert("Please select a file to upload.");
		this.file.focus();
		return false;
	}
	setStatus("Uploading file:</br>'" + this.file.value + "'");
	$("#uploadTarget").one("load", afterUpload);
	return true;
}

function afterUpload() {
	setStatus("");
	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data))
		readMapping(JSON.parse(data));
	else
		alert("Error" + (data ? ":\n\nUnable to contact server for data upload\nResponse="+data : "."));
}

function inspect() {
	// validate form
	if (!this.host.value) {
		alert("Please enter host address.");
		return false;
	}
	if (!this.database.value) {
		alert("Please enter database.");
		return false;
	}
	
	setStatus("Connecting to database:</br>'" + this.host.value + "'");
	  $.ajax({
		url: "rest/inspect",
		type: "POST",
		data: JSON.stringify($("#dbForm").formParams()),//$("#dbForm").serialize(),
		contentType:"application/json; charset=utf-8",
		dataType: "json",
		success: readMapping,
		error: alertError
	  });
	return false;
}

function setStatus(status) {
	$("#status").html(status);
	$("#status, #overlay").fadeToggle(status);
}

function readMapping(inspection) {
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
}

function getDataSourceName() {
   // NOTE: d2rq was re-writing dataSourceName() beginning with file: This was frustrating, so
   // i opted instead to use the BiSciCol namespace.  Ultimately, we want users to have some
   // control over the identity of their published dataset.
   //var name= project.connection.system == "sqlite"
	//    ? "file:" + project.connection.database.substr(0, project.connection.database.length-7)
	//	: "database:" + project.connection.database + "@" + project.connection.host;
	var name;

	if (mainproject.connection.system == "sqlite")
		name = "urn:x-biscicol:" + mainproject.connection.database.substr(0, mainproject.connection.database.length-7);
	else
		name = "urn:x-biscicol:" + mainproject.connection.database + "@" + mainproject.connection.host;
	
	// remove leading and trailing space
	name = $.trim(name);

	return name.replace(/ /g,'_');
}

function displayMapping() {
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
}

function activateDS(deactivate) {
	$("#dsDiv").toggleClass("active", !deactivate);
	$("#dbForm, #uploadForm").fadeToggle(!deactivate);
	$("#dsDescription, #schemaTable").fadeToggle(!!mainproject.schema.length);	
	$("#dsDiv > input.next").fadeToggle(!deactivate && mainproject.schema.length);
	return true;
}

function addJoinButton() { 
	return project.joins.length == project.schema.length - 1;
}

function searchRelations(entity1, entity2) { 
	var found = false;
	$.each(project.relations, function(i, relation) {
		if (relation.subject == entity1 && relation.object == entity2 || relation.subject == entity2 && relation.object == entity1) {
			found = true;
			return false;
		}
	});
	return found;
}

function countRelations(entity) { 
	var count = 0;
	$.each(project.relations, function(i, relation) {
		if (relation.subject == entity || relation.object == entity)
			count++;
	});
	return count;
}

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

function countOf(array, property, value) { 
	var count = 0;
	$.each(array, function(i, element) {
		if (element[property] == value)
			count++;
	});
	return count;
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
