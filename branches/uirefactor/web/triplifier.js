//var project = {project:"",dateTime:"",connection:{},schema:[],joins:[],entities:[],relations:[]}, // current project object
var    mainproject,
	allRelations, // array of all possible relations, each allRelation is a hash with subject and array of all possible objects in current project
	allRelationsTotal, // .5 count of all possible relations (each relation has inverse relation, only one per pair is allowed) in current project
	schemaTotal, // total number of columns in schema in current project
	joinFT, entityFT, relationFT, triplifyFT, // FlexTable objects
	vocabularyManager,
	dbSourceTrTemplate,
	relationPredicates = ["ma:isSourceOf", "ma:isRelatedTo"],
	biscicolUrl = "http://biscicol.org/",
	triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here

//	biscicolUrl = "http://geomuseblade.colorado.edu/biscicol/",
//	triplifierUrl = "http://geomuseblade.colorado.edu/triplifier/";
//	biscicolUrl = "http://johns-macbook-air-2.local:8080/biscicol/",
//	triplifierUrl = "http://johns-macbook-air-2.local:8080/triplifier/";

// execute once the DOM has loaded
$(function() {
	dbSourceTrTemplate = $("#schemaTable > tbody").children(":last").remove();
   	
	// VocabularyManager must be created before FlexTables
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), getStorageKey("vocabularies"), alertError);

	// create the project tables (this also removes blank DOM elements)
	joinFT = new JoinsTable($("#joinDiv"));
	entitiesPT = new EntitiesTable($("#entityDiv"));
	attributesPT = new AttributesTable($("#attributeDiv"));

	// assign event handlers
	$("#dbForm").submit(inspect);
	$("#uploadForm").submit(uploadData);
	$("#getMapping").click(function() {triplify("rest/getMapping", downloadFile);});
	$("#getTriples").click(function() {triplify("rest/getTriples", downloadFile);});
	$("#sendToBiSciCol").click(function() {triplify("rest/getTriples", sendToBiSciCol);});

	$("#dbForm, #uploadForm, #dsDiv > input.next, #vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// ProjectManager must be created after FlexTables and hide() as it displays the first project, so everything must be already in place
	var projman = new ProjectManager();
	var projUI = new ProjectUI($("#projects"), projman);

	// Set handlers for the navigation buttons
	$("#dsDiv > input.next").click(dSNextButtonClicked);	
	$('#joinDiv input.back').click(joinsBackButtonClicked);
	$('#joinDiv input.next').click(joinsNextButtonClicked);
	$('#entityDiv input.back').click(entitiesBackButtonClicked);
	$('#entityDiv input.next').click(entitiesNextButtonClicked);
});

/**
 * Set the currently open project.
 **/
function setMainProject(project) {
	//alert('main project set');
	mainproject = project;

	updateSchemaUI();

	updateFlexTables();
}

function dSNextButtonClicked() {
	activateDS(true);
	joinFT.setActive(true);
	return true;
}

function joinsNextButtonClicked() {
	//$("#vocabularies").fadeOut();
	joinFT.setActive(false);
	entitiesPT.setActive(true);
	return true;
}

function joinsBackButtonClicked() {
	activateDS();
	joinFT.setActive(false);
}

function entitiesNextButtonClicked() {
	//$("#vocabularies").fadeOut();
	//joinFT.setActive(true);
	return true;
}

function entitiesBackButtonClicked() {
	joinFT.setActive(true);
	entitiesPT.setActive(false);
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
	joinFT.setProject(mainproject, 'joins');
	//joinFT.removeMatching(function(join) {
	//	return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	//});
	entitiesPT.setProject(mainproject, 'entities');
	attributesPT.setProject(mainproject, 'attributes');

	// Activate/deactivate each section depending on the project state.  Note the use of "!!" to ensure
	// we have a true boolean value.
	activateDS(mainproject.schema.length); 
	joinFT.setActive(!!mainproject.schema.length && !mainproject.entities.length && !mainproject.relations.length);
	entitiesPT.setActive(!!mainproject.entities.length && !mainproject.relations.length)
}

function alertError(xhr, status, error) {
	setStatus("");
	alert(status + ': ' + error + '\n' + 'response status: ' + xhr.status + '\n' + xhr.responseText)
	//alert(status + (xhr.status==500 ? ":\n\n"+xhr.responseText : (error ? ": "+error : "")));
}

function downloadFile(url) {
	setStatus("");
	window.open(url);
}

function triplify(url, successFn) {
}

function sendToBiSciCol(url) {
}

function afterBiSciCol() {
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

/**
 * Searches for a specified table name in a project's schema.  If only the table name
 * is specified, then either the matching table object or "undefined" is returned.  If
 * a column name is also provided, then a table object is only returned if it has the
 * matching table name and contains a matching column name.  Otherwise, "undefined" is
 * returned.
 *
 * @param project The project to search.
 * @param table The table name to search for.
 * @param column The column name to search for.
 **/
function findInSchema(project, table, column) {
	// get the table object from the project's schema
	table = project.schema[indexOf(project.schema, "name", table)];

	// see if the table contains the specified column
	if (table && column && $.inArray(column, table.columns) < 0)
		table = undefined;
	return table;
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
