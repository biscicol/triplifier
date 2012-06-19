var project = {project:"",dateTime:"",connection:{},schema:[],joins:[],entities:[],relations:[]}, // current project object
//	connection, // hash of connection parameters in current project
//	schema, // array of schema tables in current project
//	joins, // array of joins in current project
//	entities, // array of entities (each entity has an array of attributes) in current project
//	relations, // array of relations in current project
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
//	biscicolUrl = "http://localhost:8080/biscicol/",
//	triplifierUrl = "http://localhost:8080/triplifier/";

// execute once the DOM has loaded
$(function() {
	dbSourceTrTemplate = $("#schemaTable > tbody").children(":last").remove();
   	
	// VocabularyManager must be created before FlexTables
	vocabularyManager = new VocabularyManager($("#vocabularies"), $("#vocabularyUpload"), getStorageKey("vocabularies"), alertError);

	// create empty flexTables (this also removes blank DOM elements)
	joinFT = new FlexTable($("#joinDiv"), authorJoin, addJoinButton, activateDS, activateEntities, onJoinModify);
	entityFT = new FlexTable($("#entityDiv"), authorEntity, addEntityButton, activateJoins, activateRelations, 
		onEntityModify, "attributes", authorAttribute, addAttributeButton);
	relationFT = new FlexTable($("#relationDiv"), authorRelation, addRelationButton, activateEntities, activateTriplify);
	triplifyFT = new FlexTable($("#triplifyDiv"), null, null, activateRelations);

	// assign event handlers
	$("#dbForm").submit(inspect);
	$("#uploadForm").submit(upload);
	$("#dsDiv > input.next").click(function() {activateDS(true);  activateJoins();});	
	$("#getMapping").click(function() {triplify("rest/getMapping", downloadFile);});
	$("#getTriples").click(function() {triplify("rest/getTriples", downloadFile);});
	$("#sendToBiSciCol").click(function() {triplify("rest/getTriples", sendToBiSciCol);});

	$("#dbForm, #uploadForm, #dsDiv > input.next, #vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// ProjectManager must be created after FlexTables and hide() as it displays the first project, so everything must be already in place
	new ProjectManager($("#projects"), getStorageKey("projects"), project, "project", getStorageKey, displayMapping);
});
	
function alertError(xhr, status, error) {
	setStatus("");
	alert(status + (xhr.status==500 ? ":\n\n"+xhr.responseText : (error ? ": "+error : "")));
}

function downloadFile(url) {
	setStatus("");
	window.open(url);
	// location = url;
}

function triplify(url, successFn) {
	setStatus("Triplifying Data Source...");
	$.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify({connection:project.connection, joins:project.joins, entities:project.entities, relations:project.relations}),
		contentType: "application/json; charset=utf-8",
		dataType: "text",
		success: successFn,
		error: alertError
	});
}

function sendToBiSciCol(url) {
	var sendToBiSciColForm = document.getElementById("sendToBiSciColForm");
	// sendToBiSciColForm.url.value = "http://" + location.host + location.pathname.substr(0, location.pathname.lastIndexOf("/")) + "/" + url;
	sendToBiSciColForm.url.value = triplifierUrl + url; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here
	$("#uploadTarget").one("load", afterBiSciCol);
	sendToBiSciColForm.submit();
}

function afterBiSciCol() {
	setStatus("");
	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data))
		window.open(biscicolUrl + "?model=" + data.substr(1, data.length-2)); 
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
}

function upload() {
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
	readItem(inspection, "dateTime");
	readItem(inspection, "connection");
	readItem(inspection, "schema");
	if (!project.joins || !project.joins.length)
		readItem(inspection, "joins");
	if (!project.entities || !project.entities.length)
		readItem(inspection, "entities");
	if (!project.relations || !project.relations.length)
		readItem(inspection, "relations");
	displayMapping();
}

function readItem(inspection, key) {
	project[key] = inspection[key];
	localStorage.setObject(getStorageKey(key, project.project), inspection[key]);
}

function displayMapping() {
	if (!project.connection) {
		project.dateTime = "";
		project.connection = {};
		project.schema = [];
		project.joins = [];
		project.entities = [];
		project.relations = [];
	}

	// update schema
	$("#dsDescription").html((project.connection.system == "sqlite" 
			? "file: " + project.connection.database.substr(0, project.connection.database.length-7) 
			: "database: " + project.connection.database + "@" + project.connection.host)
		+ ", accessed: " + project.dateTime);
	schemaTotal = 0;
	var schemaTable = $("#schemaTable"), 
		columns;
	schemaTable.children("tbody").children().remove();
	$.each(project.schema, function(i, table) {
		columns = "";
		$.each(table.columns, function(j, column) { 
			columns += column + ($.inArray(column, table.pkColumns) >= 0 ? "*" : "") + ", ";
			schemaTotal++;
		});
		columns = columns.substr(0, columns.length - 2); // remove last comma
		dbSourceTrTemplate.clone().children()
			.first().html(table.name) // write table name to first td
			.next().html(columns) // write columns to second td
			.end().end().end().appendTo(schemaTable);
	});
	
	// update data source
	$.each($("#dbForm").get(0), function(i, element) {
		if (element.type != "submit")
			element.value = (project.connection.system == "sqlite" ? "" : (project.connection[element.name] || ""));
	});
	
	// update joins, delete invalid (not in schema)
	joinFT.update(project.joins, getStorageKey("joins", project.project));
	joinFT.removeMatching(function(join) {
		return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	});
	
	// update entities, delete invalid (not in schema)
	entityFT.update(project.entities, getStorageKey("entities", project.project));
	var schemaTbl;
	entityFT.removeMatching(
		function(entity) {
			schemaTbl = findInSchema(entity.table, entity.idColumn);
			return !schemaTbl;
		},
		function(attribute) {
			return $.inArray(attribute.column, schemaTbl.columns) < 0;
		}
	);
	
	// set allRelations, update relations, delete invalid (not in allRelations)
	if (project.relations.length)
		setAllRelations();
	relationFT.update(project.relations, getStorageKey("relations", project.project));
	relationFT.removeMatching(function(relation) {
		var idx = indexOf(allRelations, "subject", relation.subject);
		return idx < 0 || $.inArray(relation.object, allRelations[idx].objects) < 0;
	});
	
	// place, show/hide vocabularies
//	$("#vocabularies").prependTo(relations.length ? $("#relationDiv") : $("#entityDiv"))
//		.toggle(!!(entities.length || relations.length));
	$("#vocabularies").fadeToggle(!!project.entities.length && !project.relations.length);
	
	// activate/deactivate each section
	activateDS(project.schema.length); 
	joinFT.activate(!project.schema.length || project.entities.length || project.relations.length);
	entityFT.activate(!project.entities.length || project.relations.length);
	relationFT.activate(!project.relations.length);
	triplifyFT.activate(true);
}

function activateDS(deactivate) {
	$("#dsDiv").toggleClass("active", !deactivate);
	$("#dbForm, #uploadForm").fadeToggle(!deactivate);
	$("#dsDescription, #schemaTable").fadeToggle(!!project.schema.length);	
	$("#dsDiv > input.next").fadeToggle(!deactivate && !!project.schema.length);	
	return true;
}

function activateJoins() {
	$("#vocabularies").fadeOut();
	joinFT.activate();
	return true;
}

function activateEntities() {
	$("#vocabularies").fadeIn();//.prependTo($("#entityDiv")).show();
	entityFT.activate();
	return true;
}

function activateRelations() {
	setAllRelations();
	$("#relationDiv > input.add").prop("disabled", addRelationButton());
	$("#vocabularies").fadeOut();
//	$("#vocabularies").prependTo($("#relationDiv")).show();
	relationFT.activate();
	return true;
}

function activateTriplify() {
//	$("#vocabularies").hide();
	triplifyFT.activate();
	return true;
}

function authorJoin(tr, join) {
	var ob = new OptionBuilder(tr);
	$.each(project.schema, function(i, table) { 
		ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("foreignTable").change(foreignTableChange).change();
}

function foreignTableChange() {
	var foreignTable = project.schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
		ob = new OptionBuilder($(this).parent().parent()),
		pk = "";
	$.each(foreignTable.columns, function(i, column) {
		if ($.inArray(column, foreignTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column + (column == pk ? "*" : ""));
	});
	ob.addOptionsTo("foreignColumn");
	$.each(project.schema, function(i, table) { 
		if (table.name != foreignTable.name)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("primaryTable").change(primaryTableChange).change();
}

function primaryTableChange() {
	var primaryTable = project.schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
		ob = new OptionBuilder($(this).parent().parent()),
		pk = "";
	$.each(primaryTable.columns, function(i, column) {
		if ($.inArray(column, primaryTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column + (column == pk ? "*" : ""));
	});
	ob.addOptionsTo("primaryColumn").val(pk);
}

function authorEntity(tr, entity) {
	var ob = new OptionBuilder(tr);
	$.each(project.schema, function(i, table) { 
		if (table.name == entity.table || countOf(project.entities, "table", table.name) < table.columns.length)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("table").prop("disabled", !!entity.table)
		.change(function() {
			var entityTable = project.schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
				pk = "";
			$.each(entityTable.columns, function(i, column) {
				if (column == entity.idColumn || indexOf(project.entities, "table", entityTable.name, "idColumn", column) < 0) {
					if ($.inArray(column, entityTable.pkColumns) >= 0)
						pk = column;
					ob.addOption(column, "", column + (column == pk ? "*" : ""));
				}
			});
			ob.addOptionsTo("idColumn").val(pk);
		})
		.change();
	authorRdfControls(tr, ob, "rdfClass", "classes");
}

function authorAttribute(tr, attribute, entity) {
	var ob = new OptionBuilder(tr);
	$.each(findInSchema(entity.table).columns, function(i, column) { 
		if (attribute.column == column || indexOf(entity.attributes, "column", column) < 0)
			ob.addOption(column);
	});
	ob.addOptionsTo("column");
	authorRdfControls(tr, ob, "rdfProperty", "properties");
}

function authorRdfControls(tr, ob, element, items) {
	vocabularyManager.onChangeFn(function() {
		var vocabulary = vocabularyManager.getSelectedVocabulary(),
			hasItems = vocabulary && vocabulary[items] && vocabulary[items].length;
		if (hasItems) {
			$.each(vocabulary[items], function(i, item) {
				ob.addOption(item.uri, "title='" + item.uri + "'", item.name);
			});
			ob.addOptionsTo(element + "[uri]").change(function() {
				tr.find("input[name='" + element + "[name]']").val(this.options[this.selectedIndex].innerHTML);
			})
			.change();
		}
		tr.find("input.save, select[name='" + element + "[uri]']").prop("disabled", !hasItems);
	});
	vocabularyManager.onChangeFn();
}

function authorRelation(tr, relation) {
	var ob = new OptionBuilder(tr);
	$.each(allRelations, function(i, allRelation) {
		if (allRelation.subject == relation.subject 
				|| allRelation.subject == relation.object
				|| countRelations(allRelation.subject) < allRelation.objects.length)
			ob.addOption(allRelation.subject, "data-allRelationIdx='" + i + "'");
	});
	ob.addOptionsTo("subject")
		.change(function() {
			var allRelation = allRelations[this.options[this.selectedIndex].getAttribute("data-allRelationIdx")];
			$.each(allRelation.objects, function(i, object) {
				if (object == relation.object || object == relation.subject
						|| !searchRelations(allRelation.subject, object))
					ob.addOption(object);
			});
			ob.addOptionsTo("object");
		})
		.change();
	$.each(relationPredicates, function(i, predicate) {
		ob.addOption(predicate);
	});
	ob.addOptionsTo("predicate");
}

// build allRelations, count allRelationsTotal
function setAllRelations() {
	allRelations = [];
	allRelationsTotal = 0;
	var objects;
	$.each(project.entities, function(i, subMp) {
		objects = [];
		$.each(project.entities, function(j, objMp) {
			if (i != j && (subMp.table == objMp.table 
				|| indexOf(project.joins, "foreignTable", subMp.table, "primaryTable", objMp.table) >= 0 
				|| indexOf(project.joins, "foreignTable", objMp.table, "primaryTable", subMp.table) >= 0)) {
				objects.push(objMp.table + "." + objMp.idColumn);
				allRelationsTotal += .5; // each relation has inverse relation, but we'll allow only one per pair
			}
		});
		if (objects.length)
			allRelations.push({subject:subMp.table + "." + subMp.idColumn, objects:objects});
	});
}

function onJoinModify(oldJoin, newJoin) {
	if (!newJoin || oldJoin.foreignTable != newJoin.foreignTable || oldJoin.primaryTable != newJoin.primaryTable) {
		relationFT.removeMatching(function(relation) {
			return relation.subject.indexOf(oldJoin.foreignTable + ".") == 0 && relation.object.indexOf(oldJoin.primaryTable + ".") == 0
				|| relation.subject.indexOf(oldJoin.primaryTable + ".") == 0 && relation.object.indexOf(oldJoin.foreignTable + ".") == 0;
		});
	}
}

function onEntityModify(oldEntity, newEntity) {
	if (!newEntity || oldEntity.table != newEntity.table || oldEntity.idColumn != newEntity.idColumn) {
		var deletedEntity = oldEntity.table + "." + oldEntity.idColumn;
		relationFT.removeMatching(function(relation) {
			return relation.object == deletedEntity || relation.subject == deletedEntity;
		});
	}
}

function addJoinButton() { 
	return project.joins.length == project.schema.length - 1;
}

function addEntityButton() {
	$("#entityDiv > input.next").prop("disabled", !project.entities.length);
	return project.entities.length == schemaTotal;
}

function addAttributeButton(entity) {
	if (!entity) return true;
	var schemaTable = findInSchema(entity.table);
	if (!schemaTable) return true;
	return schemaTable.columns.length == entity.attributes.length;
}

function addRelationButton() {
	return project.relations.length == allRelationsTotal;
}

function findInSchema(table, column) { 
	table = project.schema[indexOf(project.schema, "name", table)];
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

function OptionBuilder(container) {
    var options = "";
    this.addOption = function(value, attributes, text) {
		options += "<option value='" + value + "' " + (attributes || "") + ">" 
			+ (text || value) + "</option>";
	};
    this.addOptionsTo = function(name) {
		var select = container.find("select[name='" + name + "']").html(options);
		options = "";
		return select;
	};
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
