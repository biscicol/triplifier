var connection, // hash of connection parameters
	schema, // array of schema tables
	joins, // array of joins
	entities, // array of entities (each entity has an array of attributes)
	relations, // array of relations
	allRelations, // array of all possible relations, each allRelation is a hash with subject and array of all possible objects
	allRelationsTotal, // .5 count of all possible relations (each relation has inverse relation, but we'll allow only one per pair)
	schemaTotal, // total number of columns in schema
	joinFT, entityFT, relationFT, // FlexTable objects
	dbSourceTrTemplate,
	storage = {connection:"triplifierConnection", schema:"triplifierSchema", joins:"triplifierJoins", 
		entities:"triplifierEntities", relations:"triplifierRelations", dateTime:"triplifierDateTime"},
	classes = ["dwc:Occurrence", "dwc:Event", "dcterms:Location", "dwc:GeologicalContext", 
	           "dwc:Identification", "dwc:Taxon", "dwc:ResourceRelationship", "dwc:MeasurementOrFact"],
	predicatesLiteral = ["dcterms:modified", "geo:lat", "geo:lon"],
	predicatesBSC = ["dcterms:relation", "dcterms:source"],
	biscicolUrl = "http://biscicol.org/",
	triplifierUrl = "http://biscicol.org:8080/triplifier/"; // [hack] when file on triplifier is accessed from biscicol on the same server then port forwarding won't work so the port is set here
//	biscicolUrl = "http://geomuseblade.colorado.edu/biscicol/",
//	triplifierUrl = "http://geomuseblade.colorado.edu/triplifier/";
//	biscicolUrl = "http://localhost:8080/biscicol/",
//	triplifierUrl = "http://localhost:8080/triplifier/";

// execute once the DOM has loaded
$(function() {
	dbSourceTrTemplate = $("#schemaTable").children("tbody").children(":last").remove();
	
	// create empty flexTables (this also removes blank DOM elements)
	joinFT = new FlexTable($("#joinDiv"), authorJoin, addJoinButton, storage.joins, activateDS, activateEntities, onJoinModify);
	entityFT = new FlexTable($("#entityDiv"), authorEntity, addEntityButton, storage.entities,
		activateJoins, activateRelations, onEntityModify, "attributes", authorAttribute, addAttributeButton);
	relationFT = new FlexTable($("#relationDiv"), authorRelation, addRelationButton, storage.relations, activateEntities, activateTriplify);
	triplifyFT = new FlexTable($("#triplifyDiv"), null, null, null, activateRelations);

	// assign event handlers
	$("#dbForm").submit(inspect);	
	$("#uploadForm").submit(upload);
	$("#clear").click(clear);
	$("#getMapping").click(function() {triplify("rest/getMapping", downloadFile);});
	$("#getTriples").click(function() {triplify("rest/getTriples", downloadFile);});
	$("#sendToBiSciCol").click(function() {triplify("rest/getTriples", sendToBiSciCol);});
	
	$("#sendToBiSciColForm").attr("action", biscicolUrl + "rest/search");
	
	// read JSON objects from localStorage, display/hide elements
	connection = localStorage.getObject(storage.connection);
	schema = localStorage.getObject(storage.schema);
	joins = localStorage.getObject(storage.joins);
	entities = localStorage.getObject(storage.entities);
	relations = localStorage.getObject(storage.relations);
	if (connection && schema && schema.length && joins && entities && relations) 
		displayMapping();
	else 
		activateDS();
	$("#status, #overlay").hide();
	$("#uploadTarget").appendTo($("body")); // prevent re-posting on reload
});

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

function loadRDF(url) {
    // TODO: make a call to the loadRDF function!
	// TODO: assign a success function like the other examples
	$.ajax({
		url: url,
		type: "GET",
		data: $(form).serialize(),
		dataType: "json",
		success: function(data) {
			if (data) {
				 count = 0;
                $.each(data, function() {
                    var key, label, color = "";
                    alert ("yea data!");
                    $.each(this, function(k, v) {
                        if (k == "url") key = v;
                        if (k == "label") label = v;
                        if (k == "color") color = v;
                    });
                 });
			} else
			    // what to do with data if no data?
				//noResults();
			    alert("no data");
		},
		error: alertError
		});
}

// function openFile(url) {
	// $.ajax({
		// url: url,
		// success: showFile,
		// error: alertError
	// });
// }

// function showFile(data) {
	// setStatus("");
	// var doc = window.open().document;
	// doc.open("text/plain");
	// doc.write(data);
	// doc.close();
// }

function downloadFile(url) {
	setStatus("");
	window.open(url);
	// location = url;
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
	// distinguish response OK status by JSON format (quotes in this case)
	if (data && data.charAt(0)=='"' && data.charAt(data.length-1)=='"')
		window.open(biscicolUrl + "?model=" + data.substr(1, data.length-2)); 
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
}

function alertError(xhr, status, error) {
	setStatus("");
	alert(status + (xhr.status==500 ? ":\n\n"+xhr.responseText : (error ? ": "+error : "")));
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
	if (data && data.charAt(0)=="{" && data.charAt(data.length-1)=="}")
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
	$("#status, #overlay").toggle(!!status);
}

function readMapping(inspection) {
	setStatus("");
	localStorage.setItem(storage.dateTime, inspection.dateTime);
	connection = inspection.connection;
	localStorage.setObject(storage.connection, connection);
	schema = inspection.schema;
	localStorage.setObject(storage.schema, schema);
	if (!joins || !joins.length)
		joins = inspection.joins;
	if (!entities || !entities.length)
		entities = inspection.entities;
	if (!relations || !relations.length)
		relations = inspection.relations;
	displayMapping();
}

function displayMapping() {
	// update schema
	$("#dsDescription").html((connection.system == "sqlite" 
			? "file: " + connection.database.substr(0, connection.database.length-7) 
			: "database: " + connection.database + "@" + connection.host)
		+ ", accessed: " + localStorage.getItem(storage.dateTime));
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
	
	// update data dource
	$.each($("#dbForm").get(0), function(i, element) {
		if (element.type != "submit")
			element.value = (connection.system == "sqlite" ? "" : (connection[element.name] || ""));
	});
	
	// update joins, delete invalid (not in schema)
	joinFT.update(joins);
	joinFT.removeMatching(function(join) {
		return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	});
	
	// update entities, delete invalid (not in schema)
	entityFT.update(entities);
	var schemaTable;
	entityFT.removeMatching(
		function(entity) {
			schemaTable = findInSchema(entity.table, entity.idColumn);
			return !schemaTable;
		},
		function(attribute) {
			return $.inArray(attribute.column, schemaTable.columns) < 0;
		}
	);
	
	// set allRelations, update relations, delete invalid (not in allRelations)
	if (relations.length)
		setAllRelations();
	relationFT.update(relations);
	relationFT.removeMatching(function(relation) {
		var idx = indexOf(allRelations, "subject", relation.subject);
		return idx < 0 || $.inArray(relation.object, allRelations[idx].objects) < 0;
	});
	
	// activate/deactivate each section
	activateDS(schema.length); 
	joinFT.activate(!schema.length || entities.length || relations.length);
	entityFT.activate(!entities.length || relations.length);
	relationFT.activate(!relations.length);
}

function activateDS(deactivate) {
	$("#dsDiv").toggleClass("active", !deactivate);
	$("#dbForm, #uploadForm").toggle(!deactivate);
	$("#clear, #dsDescription, #schemaTable").toggle(!!deactivate);
	return true;
}

function activateJoins() {
	joinFT.activate();
	return true;
}

function activateEntities() {
	entityFT.activate();
	return true;
}

function activateRelations() {
	setAllRelations();
	$("#relationDiv > input.add").prop("disabled", addRelationButton());
	relationFT.activate();
	return true;
}

function activateTriplify() {
	triplifyFT.activate();
	return true;
}

function authorJoin(tr, join) {
	var ob = new OptionBuilder(tr);
	$.each(schema, function(i, table) { 
		ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("foreignTable")
		.change(foreignTableChange)
		.change();
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
	ob.addOptionsTo("primaryTable")
		.change(primaryTableChange)
		.change();
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
	ob.addOptionsTo("table")
		.prop("disabled", !!entity.table)
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
	$.each(classes, function(i, class_) { 
		ob.addOption(class_);
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
	$.each(predicatesLiteral, function(i, predicate) {
		ob.addOption(predicate);
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
	$.each(predicatesBSC, function(i, predicate) {
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

function clear() {
	if (confirm("This will clear all Data Source, Joins, Entities and Relations information. Are you sure?")) {
		localStorage.clear();
		location.reload();
		return true;
	}
	return false;
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

Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
};

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return value && JSON.parse(value);
};
