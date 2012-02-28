var schema, // array of schema tables
	mappings, // array of mappings (each mapping has an array of attributes)
	checkedTr, // tr DOM element containing checked radio
	mappingTr, // tr DOM element of checked mapping (or mapping of checked attribute)
	mIdx, // index of checked mapping (or mapping of checked attribute) in mappings
	aIdx, // index of checked attribute in mappings[mIdx].attributes
	isEdit, // true if existing mapping/attribute is edited, false if new mapping/attribute is created
	mappingTable, // #mappingTable DOM element
	attributesTrTemplate,      // tr DOM element templates
	mappingEditTrTemplate,     // ...
	attributeEditTrTemplate,   // ...
	mappingTrTemplate,         // ...
	attributeTrTemplate,       // ...
	dbSourceTrTemplate,        // ...
	classes = ["dwc:Event", "dwc:Identification", "dwc:Occurrence"],
	predicatesLiteral = ["dcterms:modified", "geo:lat", "geo:lon"],
	predicatesBSC = ["bsc:leadsTo", "bsc:comesFrom"];

// execute once the DOM has loaded
$(function() {
	// set final variables, remove templates from DOM
	mappingTable = $("#mappingTable");
	mappingEditTrTemplate = mappingTable.children().children("tr.edit").remove();
	attributesTrTemplate = mappingTable.children().children("tr.attributes").remove();
	mappingTrTemplate = mappingTable.children().children().last().remove();
	attributeEditTrTemplate = attributesTrTemplate.children().children().children().children("tr.edit").remove();
	attributeTrTemplate = attributesTrTemplate.children().children().children().children().last().remove();
	dbSourceTrTemplate = $("#dbSourceTable").children().children().last().remove();

	// assign event handlers
	$("#dbSourceForm").submit(inspect);	
	$("#clear").click(clear);
	$("#addMapping").click(addMapping);
	$("#addAttribute").click(addAttribute);
	$("#edit").click(edit);
	$("#delete").click(remove);
	// read schema and mappings from localStorage, display/hide elements
	schema = localStorage.getObject("triplifierSchema");
	mappings = localStorage.getObject("triplifierMappings");
	if (schema && mappings) {
		$("#dbSourceForm").hide();
		displayInspection();
	} else
		$("#mappingDiv").hide();		
	$("#overlay").hide();
})

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
	
	  $.ajax({
		  url: "rest/inspect",
		  data: $("#dbSourceForm").serialize(),
		  dataType: "json",
		  success: readInspection,
		  error: function(xhr, status, error) {
			  alert(status + (error ? ": "+error : ""));	
		  }
	  });
//	readInspection(JSON.parse('{"schema":[{"name":"collecting_events","columns":["Locality","Coll_EventID_collector","Collector"],"pkColumns":["Coll_EventID_collector"]},{"name":"specimens","columns":["Coll_EventID_collector","Family","Specimen_Num_Collector","SpecificEpithet","Genus"],"pkColumns":["Specimen_Num_Collector"]}],"mappings":[]}'));
	return false;
}

function readInspection(inspection) {
	schema = inspection.schema;
	mappings = inspection.mappings;
	localStorage.triplifierDateTime = inspection.dateTime;
	localStorage.setObject("triplifierSchema", schema);
	saveMappings();
	displayInspection();
	$("#dbSourceForm").slideUp();
	$("#mappingDiv").slideDown();		
}

function displayInspection() {
	$("#dateTime").html(localStorage.triplifierDateTime);
	var dbSource = $("#dbSourceTable"), columns;
	$.each(schema, function(i, table) {
		columns = "";
		$.each(table.columns, function(j, column) { 
			columns += column + ($.inArray(column, table.pkColumns) >= 0 ? "*" : "") + ", ";
		});
		columns = columns.substr(0, columns.length - 2) // remove last comma
		dbSourceTrTemplate.clone().children()
			.first().html(table.name) // write table name to first td
			.next().html(columns) // write columns to second td
			.end().end().end().appendTo(dbSource);
	});
	refreshMappingTable();
}

function addMapping() { 
	isEdit = false;
	startEdit(authorMapping({}).appendTo(mappingTable.show()));
}

function addAttribute() {
	isEdit = false;
	startEdit(authorAttribute({}).appendTo(mappingTr.next().children().children("table").show()));
}

function edit() {
	isEdit = true;
	var tr = aIdx >= 0 ?
		authorAttribute(mappings[mIdx].attributes[aIdx]) : 
		authorMapping(mappings[mIdx]);
	tr.insertAfter(checkedTr);
	checkedTr.addClass("editData");
	startEdit(tr);
}

function authorMapping(mapping) {
	var editor = new Editor(mappingEditTrTemplate.clone());
	$.each(schema, function(i, table) { 
		if (mapping.table == table.name || indexOf(mappings, "table", table.name) < 0)
			editor.addOption(table.name, "data-schemaIdx='" + i + "'");
	}); 
	editor.addOptionsTo("table")
		.prop("disabled", !!mapping.table)
		.change(mappingTableChange)
		.change();
	$.each(classes, function(i, class_) { 
		editor.addOption(class_);
	});
	editor.addOptionsTo("class");
	return editor.finalize(saveMapping, mapping);
}

function mappingTableChange() {
	var table = schema[this.options[this.selectedIndex].getAttribute("data-schemaIdx")],
		editor = new Editor($(this).parent().parent()),
		pk = "";
	$.each(table.columns, function(i, column) {
		if ($.inArray(column, table.pkColumns) >= 0)
			pk = column;
		editor.addOption(column, "", column == pk ? "*" : "");
	});
	editor.addOptionsTo("idColumn").val(pk);
}

function authorAttribute(attribute) {
	var editor = new Editor(attributeEditTrTemplate.clone());
	$.each(schema[indexOf(schema, "name", mappings[mIdx].table)].columns, function(i, column) { 
		if (attribute.column == column || indexOf(mappings[mIdx].attributes, "column", column) < 0)
			editor.addOption(column);
	});
	editor.addOptionsTo("column");
	editor.addOption("literal");
	editor.addOption("join", mappings.length > 1 ? "" : " disabled='disabled'");
	editor.addOption("object");
	editor.addOptionsTo("mode")
		.change(attributeModeChange)
		.change();
	return editor.finalize(saveAttribute, attribute);
}

function attributeModeChange() {
	var editor = new Editor($(this).parent().parent()),
		predicateOptions, disableTarget = false;
	switch ($(this).val()) {
		case "literal":
			predicateOptions = predicatesLiteral;
			disableTarget = true;
			break;
		case "join":
			predicateOptions = predicatesBSC;
			$.each(mappings, function(i, mapping) {
				if (i != mIdx)
					editor.addOption(mapping.table);
			});
			break;
		case "object":
			predicateOptions = predicatesBSC;
			$.each(classes, function(i, class_) { 
				editor.addOption(class_);
			});
			break;
	}
	editor.addOptionsTo("target").prop("disabled", disableTarget);
	$.each(predicateOptions, function(i, option) {
		editor.addOption(option);
	});
	editor.addOptionsTo("predicate");
}

function saveMapping() {
	save(this, mappings, mIdx, mappingTrTemplate, saveNewMapping);
	mappingTr = checkedTr;
}

function saveAttribute() {
	save(this, mappings[mIdx].attributes, aIdx, attributeTrTemplate, saveNewAttribute);
}

function save(button, items, idx, displayTrTemplate, newFn) {
	var tr = $(button).parent().parent(),
		item = tr.formParams();
	if (isEdit) {
		$.each(item, function(name, value) { // copy values from form to mapping/attribute
			items[idx][name] = value;
		});
		tr.prev().remove(); // remove old row
	}
	else {
		items.push(item);
		newFn(item, items);
		setButtons();
	}
	saveMappings();
	checkedTr = display(displayTrTemplate, item).replaceAll(tr).first();
	finishEdit(checkedTr);
}

function saveNewMapping(mapping) {
	mapping.attributes = [];
	mIdx = mappings.length - 1;
	aIdx = -1;
}

function saveNewAttribute(attribute, attributes) {
	aIdx = attributes.length - 1;
}

function cancel() {
	var tr = $(this).parent().parent();
	finishEdit(tr);
	hideRemove(tr);
}

function remove() { 
	if (confirm("This will delete currently checked mapping. Are you sure?")) {
		if (aIdx >= 0) {
			mappings[mIdx].attributes.splice(aIdx, 1); // remove attribute
			hideRemove(checkedTr); // remove checked row
			mappingTr.find("input:radio").prop("checked", true); // check mapping
			checkedTr = mappingTr;
			aIdx = -1;
			$("#addAttribute").prop("disabled", false);
		}
		else {
			var table = mappings[mIdx].table;
			mappings.splice(mIdx, 1); // remove mapping
			$.each(mappings, function(i, mapping) { // cascading delete (remove attributes that join with removed mapping)
				for (var j = 0; j < mapping.attributes.length; j++)
					if (mapping.attributes[j].target == table) 
						mapping.attributes.splice(j--, 1); // remove attribute
			});
			mappingTable.children().children().slice(1).remove(); // empty mappingTable
			// checkedTr.next().remove(); // remove attibutes row
			// hideRemove(checkedTr); // remove checked row
			refreshMappingTable();
		}
		saveMappings();
	}
}

function clear() {
	if (confirm("This will clear all Data Source and Mapping information. Are you sure?")) {
		localStorage.clear();
		location.reload();
	}
}

function display(trTemplate, mapping) { 
	var result = trTemplate.clone(),
		td = result.children().first(); // first td
	td.children("input").change(radioChange).prop("checked", true);
	$.each(mapping, function(name, value) { 
		if (name != "attributes")
			td = td.next().html(value); // write value to next sibling (td)
		else {
			result = result.add(attributesTrTemplate.clone());
			var attributesTable = result.last().children().children("table");
			if (!value.length)
				attributesTable.hide();
			$.each(value, function(i, attribute) { 
				display(attributeTrTemplate, attribute).appendTo(attributesTable);
			});
		}
	});
	return result;
}

function startEdit(tr) {
	tr.siblings().first().addClass("editData"); // table header row
	$("#overlay").fadeIn();
}

function finishEdit(tr) {
	tr.parent().children().removeClass("editData");
	$("#overlay").fadeOut();
}

function hideRemove(tr) { 
	if (tr.siblings().size() <= 1) 
		tr.closest("table").hide();
	tr.remove();
}

function radioChange() { 
    checkedTr = $(this).parent().parent();
	mappingTr = checkedTr.parent().closest("tr").prev();
	if (!mappingTr.length)
		mappingTr = checkedTr;

	mIdx = indexOf(mappings, "table", mappingTr.children().eq(1).html());

	aIdx = checkedTr == mappingTr ? 
		-1 :
		indexOf(mappings[mIdx].attributes, "column", checkedTr.children().eq(1).html());
		
	setButtons();
}

function refreshMappingTable() { 
	if (!mappings.length)
		mappingTable.hide();
	$.each(mappings, function(i, mapping) {
		display(mappingTrTemplate, mapping).appendTo(mappingTable);
	});
	checkedTr = mappingTable.find("input:radio").first().prop("checked", true).parent().parent();
	mappingTr = checkedTr;
	mIdx = 0;
	aIdx = -1;
	setButtons();
}

function setButtons() { 
	$("#addMapping").prop("disabled", mappings.length == schema.length);
	$("#edit, #delete").prop("disabled", !mappings.length);
	$("#addAttribute").prop("disabled", 
		!mappings.length || 
		schema[indexOf(schema, "name", mappings[mIdx].table)].columns.length == 
		mappings[mIdx].attributes.length);
}

function indexOf(array, property, value) { 
	var result = -1;
	$.each(array, function(i, element) {
		if (element[property] == value) {
			result = i;
			return false;
		}
	});
	return result;
}

function saveMappings() { 
	localStorage.setObject("triplifierMappings", mappings);
}

function Editor(tr) {
    var options = "";
    this.addOption = function(option, attributes, text) {
		options += "<option value='" + option + "' " + (attributes || "") + ">" 
			+ option + (text || "") + "</option>";
	}
    this.addOptionsTo = function(name) {
		var select = tr.find("select[name='" + name + "']").html(options);
		options = "";
		return select;
	}
    this.finalize = function(saveButtonClick, item) {
		tr.find("#saveButton").click(saveButtonClick);
		tr.find("#cancelButton").click(cancel);
		$.each(item, function(name, value) { 
			tr.find("select[name='" + name + "']").val(value).change();
		});
		return tr;
	}
}

Storage.prototype.setObject = function(key, value) {
	this.setItem(key, JSON.stringify(value));
}

Storage.prototype.getObject = function(key) {
	var value = this.getItem(key);
	return value && JSON.parse(value);
}

