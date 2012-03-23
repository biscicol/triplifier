var connection, 
	schema, // array of schema tables
	joins, 
	mappings, // array of mappings (each mapping has an array of attributes)
	relations,
	allRelations,
	allRelationsTotal,
	schemaTotal, // total number of columns in schema
	joinFT, mappingFT, relationFT, // FlexTable objects
	dbSourceTrTemplate,
	storage = {connection:"triplifierConnection", schema:"triplifierSchema", joins:"triplifierJoins", 
		mappings:"triplifierMappings", relations:"triplifierRelations", dateTime:"triplifierDateTime"},
	classes = ["dwc:Event", "dwc:Identification", "dwc:Occurrence"],
	predicatesLiteral = ["dcterms:modified", "geo:lat", "geo:lon"],
	predicatesBSC = ["bsc:leadsTo", "bsc:comesFrom"];

// execute once the DOM has loaded
$(function() {
	dbSourceTrTemplate = $("#schemaTable").children("tbody").children(":last").remove();
	
	// create empty flexTables
	joinFT = new FlexTable($("#joinDiv"), authorJoin, addJoinButton, storage.joins, activateDS, activateMappings, onJoinModify);
	mappingFT = new FlexTable($("#mappingDiv"), authorMapping, addMappingButton, storage.mappings,
		activateJoins, activateRelations, onMappingModify, "attributes", authorAttribute, addAttributeButton);
	relationFT = new FlexTable($("#relationDiv"), authorRelation, addRelationButton, storage.relations, activateMappings, activateTriplify);
	triplifyFT = new FlexTable($("#triplifyDiv"), null, null, null, activateRelations);

	// assign event handlers
	$("#dbForm").submit(inspect);	
	$("#uploadForm").submit(upload);
	$("#clear").click(clear);
	$("#openMapping").click(function() {triplify("rest/getMapping", openFile);});
	$("#downloadMapping").click(function() {triplify("rest/getMapping", downloadFile);});
	$("#openTriples").click(function() {triplify("rest/getTriples", openFile);});
	$("#downloadTriples").click(function() {triplify("rest/getTriples", downloadFile);});
	$("#sendToBiscicol").click(function() {triplify("rest/sendToBiscicol");});
	
	// read JSON from localStorage, display/hide elements
	connection = localStorage.getObject(storage.connection);
	schema = localStorage.getObject(storage.schema);
	joins = localStorage.getObject(storage.joins);
	mappings = localStorage.getObject(storage.mappings);
	relations = localStorage.getObject(storage.relations);
	if (connection && schema && schema.length && joins && mappings && relations) 
		displayMapping();
	else 
		activateDS();
	$("#status, #overlay").hide();
	$("#uploadTarget").appendTo($('body')); // prevent re-posting on reload
});

$(window).load(function() {
	$("#uploadTarget").load(afterUpload);
});

function triplify(url, successFn) {
	setStatus("Triplifying Data Source...");
	$.ajax({
		url: url,
		type: "POST",
		data: JSON.stringify({connection:connection, joins:joins, entities:mappings, relations:relations}),
		contentType:"application/json; charset=utf-8",
		dataType: "text",
		success: successFn,
		error: alertError
	});
}

function openFile(url) {
	$.ajax({
		url: url,
		success: showFile,
		error: alertError
	});
}

function showFile(data) {
	setStatus("");
	var doc = window.open().document;
	doc.open("text/plain");
	doc.write(data);
	doc.close();
}

function downloadFile(url) {
	setStatus("");
	window.open(url);
//	location = url;
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
	return true;
}

function afterUpload(event) {
	setStatus("");
	var data = frames["uploadTarget"].document.body.textContent;
	if (data && data.charAt(0)=="{" && data.charAt(data.length-1)=="}")
		readMapping(JSON.parse(data));
	else
		alert("Error" + (data ? ":\n\n"+data : "."));	
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
	// readMapping(JSON.parse('{"schema":[{"name":"collecting_events","columns":["Locality","Coll_EventID_collector","Collector"],"pkColumns":["Coll_EventID_collector"]},{"name":"specimens","columns":["Coll_EventID_collector","Family","Specimen_Num_Collector","SpecificEpithet","Genus"],"pkColumns":["Specimen_Num_Collector"]}],"joins":[],"mappings":[],"relations":[],"dateTime":"Feb 28, 2012 12:31:17 PM"}'));
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
	if (!mappings || !mappings.length)
		mappings = inspection.entities;
	if (!relations || !relations.length)
		relations = inspection.relations;
	displayMapping();
	// $("#dbForm, #uploadForm").slideUp();
	// $("#schemaTable").slideDown();		
}

function displayMapping() {
	activateDS(true);

	// update schema
	$("#dsDescription").html(
		(connection.system == "sqlite" 
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
		columns = columns.substr(0, columns.length - 2) // remove last comma
		dbSourceTrTemplate.clone().children()
			.first().html(table.name) // write table name to first td
			.next().html(columns) // write columns to second td
			.end().end().end().appendTo(schemaTable);
	});
	
	// update joins, delete invalid (not in schema)
	joinFT.update(joins, !mappings.length && !relations.length);
	joinFT.removeMatching(function(join) {
		return !findInSchema(join.foreignTable, join.foreignColumn) || !findInSchema(join.primaryTable, join.primaryColumn);
	});
	
	// update entities, delete invalid (not in schema)
	mappingFT.update(mappings, mappings.length && !relations.length);
	var schemaTable;
	mappingFT.removeMatching(
		function(mapping) {
			schemaTable = findInSchema(mapping.table, mapping.idColumn);
			return !schemaTable;
		},
		function(attribute) {
			return $.inArray(attribute.column, schemaTable.columns) < 0;
		}
	);
	
	// set allRelations, update relations, delete invalid (not in allRelations)
	if (relations.length)
		setAllRelations();
	relationFT.update(relations, relations.length);
	relationFT.removeMatching(function(relation) {
		var idx = indexOf(allRelations, "subject", relation.subject);
		return idx < 0 || $.inArray(relation.object, allRelations[idx].objects) < 0;
	});
}

function activateDS(deactivate, notAnimate) {
	$("#dsDiv").toggleClass("active", !deactivate);
	$("#dbForm, #uploadForm").toggle(!deactivate);
	$("#clear, #dsDescription, #schemaTable").toggle(deactivate);
	return true;
}

function activateJoins() {
	joinFT.activate();
	return true;
}

function activateMappings() {
	mappingFT.activate();
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

function authorMapping(tr, mapping) {
	var ob = new OptionBuilder(tr);
	$.each(schema, function(i, table) { 
		if (table.name == mapping.table || countOf(mappings, "table", table.name) < table.columns.length)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	ob.addOptionsTo("table")
		.prop("disabled", !!mapping.table)
		.change(function() {
			var mappingTable = schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
				pk = "";
			$.each(mappingTable.columns, function(i, column) {
				if (column == mapping.idColumn || indexOf(mappings, "table", mappingTable.name, "idColumn", column) < 0) {
					if ($.inArray(column, mappingTable.pkColumns) >= 0)
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

function authorAttribute(tr, attribute, mapping) {
	var ob = new OptionBuilder(tr);
	$.each(findInSchema(mapping.table).columns, function(i, column) { 
		if (attribute.column == column || indexOf(mapping.attributes, "column", column) < 0)
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
		if (allRelation.subject == relation.subject || countRelations(allRelation.subject) < allRelation.objects.length)
			ob.addOption(allRelation.subject, "data-allRelationIdx='" + i + "'");
	});
	ob.addOptionsTo("subject")
		.change(function() {
			var allRelation = allRelations[this.options[this.selectedIndex].getAttribute("data-allRelationIdx")];
			$.each(allRelation.objects, function(i, object) {
				if (object == relation.object || !searchRelations(allRelation.subject, object))
					ob.addOption(object);
			});
			ob.addOptionsTo("object")
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
	$.each(mappings, function(i, subMp) {
		objects = [];
		$.each(mappings, function(j, objMp) {
			if (i != j && (subMp.table == objMp.table || indexOf(joins, "foreignTable", subMp.table, "primaryTable", objMp.table) >= 0)) {
				objects.push(objMp.table + "." + objMp.idColumn);
				allRelationsTotal += (subMp.table == objMp.table ? .5 : 1);
			}
		});
		if (objects.length)
			allRelations.push({subject:subMp.table + "." + subMp.idColumn, objects:objects});
	});
}

// delete relations that are not in allRelations
// function updateRelations() {
	// relationFT.removeMatching(function(relation) {
		// var idx = indexOf(allRelations, "subject", relation.subject);
		// return idx < 0 || $.inArray(relation.object, allRelations[idx].objects) < 0;
	// });
	// for (var rIdx = 0; rIdx < relations.length; rIdx++) {
		// arIdx = indexOf(allRelations, "subject", relations[rIdx].subject)
		// if (arIdx < 0 || $.inArray(relations[rIdx].object, allRelations[arIdx].objects) < 0)
			// relations.splice(rIdx--, 1);
	// }
// }

function onJoinModify(oldJoin, newJoin) {
	if (!newJoin || oldJoin.foreignTable != newJoin.foreignTable || oldJoin.primaryTable != newJoin.primaryTable) {
		relationFT.removeMatching(function(relation) {
			return relation.subject.indexOf(oldJoin.foreignTable + ".") == 0 && relation.object.indexOf(oldJoin.primaryTable + ".") == 0;
		});
	}
}

function onMappingModify(oldMapping, newMapping) {
	if (!newMapping || oldMapping.table != newMapping.table || oldMapping.idColumn != newMapping.idColumn) {
		var deletedEntity = oldMapping.table + "." + oldMapping.idColumn;
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

function addMappingButton() {
	$("#mappingDiv > input.next").prop("disabled", !mappings.length);
	return mappings.length == schemaTotal;
}

function addAttributeButton(mapping) {
	if (!mapping) return true;
	var schemaTable = findInSchema(mapping.table);
	if (!schemaTable) return true;
	return schemaTable.columns.length == mapping.attributes.length;
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

function searchRelations(subject, object) { 
	var found = false,
		length = subject.indexOf(".") + 1, 
		sameTable = subject.substr(0, length) == object.substr(0, length);
	$.each(relations, function(i, relation) {
		if (relation.subject == subject && relation.object == object || sameTable && relation.subject == object && relation.object == subject) {
			found = true;
			return false;
		}
	});
	return found;
}

function countRelations(subject) { 
	var count = 0,
		length = subject.indexOf(".") + 1, 
		table = subject.substr(0, length);
	$.each(relations, function(i, relation) {
		if (relation.subject == subject || relation.object == subject && relation.subject.substr(0, length) == table)
			count++;
	});
	return count;
}

function OptionBuilder(container) {
    var options = "";
    this.addOption = function(option, attributes, text) {
		options += "<option value='" + option + "' " + (attributes || "") + ">" 
			+ option + (text || "") + "</option>";
	}
    this.addOptionsTo = function(name) {
		var select = container.find("select[name='" + name + "']").html(options);
		options = "";
		return select;
	}
}

Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
}

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return value && JSON.parse(value);
}

