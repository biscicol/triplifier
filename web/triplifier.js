var project, // name of current project
	lastProject = 0, //
	lastVocabulary = 0,
	vocabularies = {}, // hash of vocabularies downloaded in current session
	connection, // hash of connection parameters in current project
	schema, // array of schema tables in current project
	joins, // array of joins in current project
	entities, // array of entities (each entity has an array of attributes) in current project
	relations, // array of relations in current project
	allRelations, // array of all possible relations, each allRelation is a hash with subject and array of all possible objects in current project
	allRelationsTotal, // .5 count of all possible relations (each relation has inverse relation, only one per pair is allowed) in current project
	schemaTotal, // total number of columns in schema in current project
	joinFT, entityFT, relationFT, // FlexTable objects
	dbSourceTrTemplate,
	storage = {projects:"triplifier.projects", vocabularies:"triplifier.vocabularies",
		connection:"triplifier.connection.", schema:"triplifier.schema.", joins:"triplifier.joins.", 
		entities:"triplifier.entities.", relations:"triplifier.relations.", dateTime:"triplifier.dateTime."},
	relationPredicates = ["ma:isSourceOf", "ma:isRelatedTo"],
	biscicolUrl = "http://biscicol.org/",
	triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here
//	biscicolUrl = "http://geomuseblade.colorado.edu/biscicol/",
//	triplifierUrl = "http://geomuseblade.colorado.edu/triplifier/";
//	biscicolUrl = "http://localhost:8080/biscicol/",
//	triplifierUrl = "http://localhost:8080/triplifier/";

// execute once the DOM has loaded
$(function() {
   	$.ajax({
		url: "rest/getVocabularies",
		type: "POST",
		data: JSON.stringify(localStorage.getObject(storage.vocabularies) || []),
		contentType: "application/json; charset=utf-8",
		dataType: "json",
		success: populateVocabularies,
		error: alertError
		});
	
	dbSourceTrTemplate = $("#schemaTable").children("tbody").children(":last").remove();
	
	// create empty flexTables (this also removes blank DOM elements)
	joinFT = new FlexTable($("#joinDiv"), authorJoin, addJoinButton, activateDS, activateEntities, onJoinModify);
	entityFT = new FlexTable($("#entityDiv"), authorEntity, addEntityButton,
		activateJoins, activateRelations, onEntityModify, "attributes", authorAttribute, addAttributeButton);
	relationFT = new FlexTable($("#relationDiv"), authorRelation, addRelationButton, activateEntities, activateTriplify);
	triplifyFT = new FlexTable($("#triplifyDiv"), null, null, activateRelations);

	// assign event handlers
	$("#newProjectForm").submit(newProject);	
	$("#exportForm").submit(exportProject);	
	$("#importFile").change(importProject);	
	$("#importProject").click(function() {$("#importFile").val("").click();});	
	$("#deleteProject").click(deleteProject);	
	$("#deleteAll").click(deleteAll);
	$("#dbForm").submit(inspect);
	$("#uploadForm").submit(upload);
	$("#dsDiv > input.next").click(function() {activateDS(true);  activateJoins();});	
	$("#getMapping").click(function() {triplify("rest/getMapping", downloadFile);});
	$("#getTriples").click(function() {triplify("rest/getTriples", downloadFile);});
	$("#sendToBiSciCol").click(function() {triplify("rest/getTriples", sendToBiSciCol);});
	$("#showVocabularyUpload").click(function() {$("#overlay, #vocabularyUpload").fadeIn();});
	$("#hideVocabularyUpload").click(function() {$("#overlay, #vocabularyUpload").fadeOut();});
	$("#vocabularyUploadLocalForm").submit(vocabularyUploadLocal);
	$("#vocabularyLoadUrlForm").submit(vocabularyLoadUrl);

	$("#dbForm, #uploadForm, #dsDiv > input.next, #vocabularies, #status, #overlay, #vocabularyUpload").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// populate projects section, load project
	var projects = [];
	$.each(localStorage.getObject(storage.projects) || [], function(i, prj) { 
		projects.push(projectElement(prj));
	});
	if (projects.length) 
		$(projects.join("")).appendTo($("#projects"))
			.filter("input").change(openProject)
			.first().prop("checked", true).change(); // load first project
	else
		newDefaultProject(); // load default project
});

function newProject() {
	var newProject = this.project.value;
	if (!newProject) {
		alert("Please enter a project name.");
		this.project.focus();
		return false;
	}
	var projects = localStorage.getObject(storage.projects) || [];
	if ($.inArray(newProject, projects) >= 0) {
		alert("Project '" + newProject + "' already exists. Please use a different name.");
		this.project.focus();
		return false;
	}
	projects.push(newProject);
	localStorage.setObject(storage.projects, projects);
	$(projectElement(newProject)).appendTo($("#projects"))
		.first("input").prop("checked", true).change(openProject).change();
	return false;
}

function projectElement(prj) {
	lastProject++;
	return "<input type='radio' name='projectChoice' value='" + prj + "' id='p" + lastProject + "' /><label for='p" + lastProject + "'>" + prj + "</label>";
}

function openProject() {
	project = this.value;
	connection = localStorage.getObject(storage.connection + project);
	schema = localStorage.getObject(storage.schema + project);
	joins = localStorage.getObject(storage.joins + project);
	entities = localStorage.getObject(storage.entities + project);
	relations = localStorage.getObject(storage.relations + project);
	displayMapping();
}

function deleteProject() {
	if (!confirm("Are you sure you want to DELETE project '" + project + "'?")) 
		return;
	var projects = localStorage.getObject(storage.projects) || [],
		idx = $.inArray(project, projects);
	if (idx >= 0) {
		projects.splice(idx, 1);
		localStorage.setObject(storage.projects, projects);
	}
	localStorage.removeItem(storage.connection + project);
	localStorage.removeItem(storage.schema + project);
	localStorage.removeItem(storage.joins + project);
	localStorage.removeItem(storage.entities + project);
	localStorage.removeItem(storage.relations + project);
	$("#projects").find("input[value='" + project + "']").next().remove().end().remove()
		.end().find("input[type='radio']").first().prop("checked", true).change();
	if (!projects.length) 
		newDefaultProject();
}

function deleteAll() {
	if (confirm("Are you sure you want to DELETE ALL PROJECTS?")) {
		localStorage.clear();
		location.reload();
	}
}

function newDefaultProject() {
	$("#newProjectForm input[name='project']").val("Default Project")
		.parent("form").submit().end().val("");
}

function exportProject() {
	this.filename.value = project.replace(/\s+/g, "_") + ".trp";
	this.content.value = JSON.stringify({project:project, 
		dateTime:localStorage.getItem(storage.dateTime + project), 
		connection:connection, schema:schema, joins:joins, entities:entities, relations:relations});
}

function importProject() {
    var reader = new FileReader();
    reader.onload = readProject;
    reader.readAsText(this.files[0]);
}

function readProject() {
	try {
		var projectObj = JSON.parse(this.result),
			projects = localStorage.getObject(storage.projects) || [],
			newProject = projectObj.project,
			i = 1;
		while ($.inArray(newProject, projects) >= 0)
			newProject = projectObj.project + "." + i++;
		projects.push(newProject);
		localStorage.setObject(storage.projects, projects);
		localStorage.setItem(storage.dateTime + newProject, projectObj.dateTime);
		localStorage.setObject(storage.connection + newProject, projectObj.connection);
		localStorage.setObject(storage.schema + newProject, projectObj.schema);
		localStorage.setObject(storage.joins + newProject, projectObj.joins);
		localStorage.setObject(storage.entities + newProject, projectObj.entities);
		localStorage.setObject(storage.relations + newProject, projectObj.relations);
		$(projectElement(newProject)).appendTo($("#projects"))
			.first("input").prop("checked", true).change(openProject).change();
	}
	catch(err) {
		alert("Error reading file.");
	}	
}

function populateVocabularies(data) {
	if (data) {
		var checkBoxes = [];
		$.each(data, function(vocabName, displayName) { 
			checkBoxes.push(vocabularyElement(vocabName, displayName));
		});
		$(checkBoxes.join("")).appendTo($("#vocabularies div"))
			.filter("input").change(getVocabulary)
			.first().prop("checked", true).change();
	} else
	    alert("Unable to find Vocabularies to Load");
}

function vocabularyElement(vocabName, displayName) {
	lastVocabulary++;
	return "<input type='radio' name='vocabularyChoice' value='" + vocabName + "' id='v" + lastVocabulary + "' /><label for='v" + lastVocabulary + "'>" + displayName + "</label><br />";
}

function vocabularyUploadLocal() {
	return vocabularyLoad(this.file, "Please select a vocabulary file to upload.");
}

function vocabularyLoadUrl() {
	return vocabularyLoad(this.url, "Please enter a URL to load vocabulary from.");
}

function vocabularyLoad(reqField, reqMessage) {
	if (!reqField.value) {
		alert(reqMessage);
		reqField.focus();
		return false;
	}
	$("#vocabularyUpload").css("z-index", 2);	
	setStatus("Loading...");
	$("#uploadTarget").one("load", afterVocabulary);
	return true;
}

function afterVocabulary() {
	$("#status").hide();
	$("#vocabularyUpload").css("z-index", 5);	
	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data)) {
		var vocabulary = JSON.parse(data),
			vocabularyStorage = localStorage.getObject(storage.vocabularies) || [];
		vocabularyStorage.push(vocabulary.name);
		localStorage.setObject(storage.vocabularies, vocabularyStorage);
		vocabularies[vocabulary.name] = vocabulary;
		$(vocabularyElement(vocabulary.name, vocabulary.name)).appendTo($("#vocabularies div"))
			.first("input").prop("checked", true).change(getVocabulary);
		alert("Vocabulary '" + vocabulary.name + "' uploaded successfully.");
		$("#hideVocabularyUpload").click();
	}
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
}

function getVocabulary() {
	if (!vocabularies[this.value])
		$.ajax({
			url: "rest/getVocabulary",
			type: "POST",
			data: this.value,
			contentType: "text/plain; charset=utf-8",
			dataType: "json",
			success: function(vocabulary) {vocabularies[vocabulary.name] = vocabulary;},
			error: alertError
		});
}

function getSelectedVocabulary() {
	return vocabularies[$("#vocabularies input:checked").val()];
}

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
		data: JSON.stringify({connection:connection, joins:joins, entities:entities, relations:relations}),
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
	localStorage.setItem(storage.dateTime + project, inspection.dateTime);
	connection = inspection.connection;
	localStorage.setObject(storage.connection + project, connection);
	schema = inspection.schema;
	localStorage.setObject(storage.schema + project, schema);
	if (!joins || !joins.length) {
		joins = inspection.joins;
		localStorage.setObject(storage.joins + project, joins);
	}
	if (!entities || !entities.length) {
		entities = inspection.entities;
		localStorage.setObject(storage.entities + project, entities);
	}
	if (!relations || !relations.length) {
		relations = inspection.relations;
		localStorage.setObject(storage.relations + project, relations);
	}
	displayMapping();
}

function displayMapping() {
	if (!connection) {
		connection = {};
		schema = [];
		joins = [];
		entities = [];
		relations = [];
	}

	// update schema
	$("#dsDescription").html((connection.system == "sqlite" 
			? "file: " + connection.database.substr(0, connection.database.length-7) 
			: "database: " + connection.database + "@" + connection.host)
		+ ", accessed: " + localStorage.getItem(storage.dateTime + project));
	schemaTotal = 0;
	var schemaTable = $("#schemaTable"), 
		columns;
	schemaTable.children("tbody").children().remove();
	$.each(schema, function(i, table) {
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
			element.value = (connection.system == "sqlite" ? "" : (connection[element.name] || ""));
	});
	
	// update joins, delete invalid (not in schema)
	joinFT.update(joins, storage.joins + project);
	joinFT.removeMatching(function(join) {
		return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	});
	
	// update entities, delete invalid (not in schema)
	entityFT.update(entities, storage.entities + project);
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
	if (relations.length)
		setAllRelations();
	relationFT.update(relations, storage.relations + project);
	relationFT.removeMatching(function(relation) {
		var idx = indexOf(allRelations, "subject", relation.subject);
		return idx < 0 || $.inArray(relation.object, allRelations[idx].objects) < 0;
	});
	
	// place, show/hide vocabularies
//	$("#vocabularies").prependTo(relations.length ? $("#relationDiv") : $("#entityDiv"))
//		.toggle(!!(entities.length || relations.length));
	$("#vocabularies").fadeToggle(!!entities.length && !relations.length);
	
	// activate/deactivate each section
	activateDS(schema.length); 
	joinFT.activate(!schema.length || entities.length || relations.length);
	entityFT.activate(!entities.length || relations.length);
	relationFT.activate(!relations.length);
	triplifyFT.activate(true);
}

function activateDS(deactivate) {
	$("#dsDiv").toggleClass("active", !deactivate);
	$("#dbForm, #uploadForm").fadeToggle(!deactivate);
	$("#dsDescription, #schemaTable").fadeToggle(!!schema.length);	
	$("#dsDiv > input.next").fadeToggle(!deactivate && !!schema.length);	
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
	$.each(schema, function(i, table) { 
		ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("foreignTable").change(foreignTableChange).change();
}

function foreignTableChange() {
	var foreignTable = schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
		ob = new OptionBuilder($(this).parent().parent()),
		pk = "";
	$.each(foreignTable.columns, function(i, column) {
		if ($.inArray(column, foreignTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column == pk ? "*" : "");
	});
	ob.addOptionsTo("foreignColumn");
	$.each(schema, function(i, table) { 
		if (table.name != foreignTable.name)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("primaryTable").change(primaryTableChange).change();
}

function primaryTableChange() {
	var primaryTable = schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
		ob = new OptionBuilder($(this).parent().parent()),
		pk = "";
	$.each(primaryTable.columns, function(i, column) {
		if ($.inArray(column, primaryTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column == pk ? "*" : "");
	});
	ob.addOptionsTo("primaryColumn").val(pk);
}

function authorEntity(tr, entity) {
	var ob = new OptionBuilder(tr);
	$.each(schema, function(i, table) { 
		if (table.name == entity.table || countOf(entities, "table", table.name) < table.columns.length)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("table").prop("disabled", !!entity.table)
		.change(function() {
			var entityTable = schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
				pk = "";
			$.each(entityTable.columns, function(i, column) {
				if (column == entity.idColumn || indexOf(entities, "table", entityTable.name, "idColumn", column) < 0) {
					if ($.inArray(column, entityTable.pkColumns) >= 0)
						pk = column;
					ob.addOption(column, "", column == pk ? "*" : "");
				}
			});
			ob.addOptionsTo("idColumn").val(pk);
		})
		.change();
	var vocabulary = getSelectedVocabulary(),
		prefix = vocabulary.prefix ? vocabulary.prefix + ":" : "";
	$.each(vocabulary.classes, function(i, class_) { 
		ob.addOption(prefix + class_.name);
	});
	ob.addOptionsTo("rdfClass");
}

function authorAttribute(tr, attribute, entity) {
	var ob = new OptionBuilder(tr);
	$.each(findInSchema(entity.table).columns, function(i, column) { 
		if (attribute.column == column || indexOf(entity.attributes, "column", column) < 0)
			ob.addOption(column);
	});
	ob.addOptionsTo("column");
	var vocabulary = getSelectedVocabulary(),
		prefix = vocabulary.prefix ? vocabulary.prefix + ":" : "";
	$.each(vocabulary.properties, function(i, property) {
		ob.addOption(prefix + property.name);
	});
	ob.addOptionsTo("predicate");
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
	$.each(entities, function(i, subMp) {
		objects = [];
		$.each(entities, function(j, objMp) {
			if (i != j && (subMp.table == objMp.table 
				|| indexOf(joins, "foreignTable", subMp.table, "primaryTable", objMp.table) >= 0 
				|| indexOf(joins, "foreignTable", objMp.table, "primaryTable", subMp.table) >= 0)) {
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
	return joins.length == schema.length - 1;
}

function addEntityButton() {
	$("#entityDiv > input.next").prop("disabled", !entities.length);
	return entities.length == schemaTotal;
}

function addAttributeButton(entity) {
	if (!entity) return true;
	var schemaTable = findInSchema(entity.table);
	if (!schemaTable) return true;
	return schemaTable.columns.length == entity.attributes.length;
}

function addRelationButton() {
	return relations.length == allRelationsTotal;
}

function findInSchema(table, column) { 
	table = schema[indexOf(schema, "name", table)];
	if (table && column && $.inArray(column, table.columns) < 0)
		table = undefined;
	return table;
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

function searchRelations(entity1, entity2) { 
	var found = false;
	$.each(relations, function(i, relation) {
		if (relation.subject == entity1 && relation.object == entity2 || relation.subject == entity2 && relation.object == entity1) {
			found = true;
			return false;
		}
	});
	return found;
}

function countRelations(entity) { 
	var count = 0;
	$.each(relations, function(i, relation) {
		if (relation.subject == entity || relation.object == entity)
			count++;
	});
	return count;
}

function OptionBuilder(container) {
    var options = "";
    this.addOption = function(option, attributes, text) {
		options += "<option value='" + option + "' " + (attributes || "") + ">" 
			+ option + (text || "") + "</option>";
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
	
Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
};

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return value && JSON.parse(value);
};

jQuery.prototype.fadeToggle = function(fadeIn) {
	if (fadeIn)
		this.fadeIn();
	else
		this.fadeOut();
};