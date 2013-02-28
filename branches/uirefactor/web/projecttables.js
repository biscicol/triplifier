/**
 * JoinsTable is a subclass of EditableTable that allows the user to add and modify joins in
 * the triplifier interface.
 **/
function JoinsTable(element) {
	// call the parent's constructor
	JoinsTable.superclass.call(this, element);
}

// JoinsTable inherits from EditableTable
JoinsTable.prototype = new EditableTable();
JoinsTable.prototype.constructor = JoinsTable;
JoinsTable.superclass = EditableTable;

JoinsTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	JoinsTable.superclass.prototype.setButtonStates.apply(this);

	// Set a custom message to display when the user clicks the "Delete" button.
	this.delete_msg = "Are you sure you want to delete the selected join?  This will also delete any relations that require this join.";

	// See if more joins are possible and set the state of the "add" button accordingly.
	var hasmorejoins = this.project.joins.length != this.project.schema.length - 1;
	this.contentelem.children("input.add").prop("disabled", !hasmorejoins);
}

/**
 * Adds the foreign table names from the project to the table row.
 *
 * @param tr The table row with the input elements to populate.
 * @param isedit Specifies whether this is an "add" or "edit" request.
 **/
JoinsTable.prototype.populateTableRowOptions = function(tr, isedit) {
	var ob = new OptionBuilder(tr);
	$.each(this.project.schema, function(i, table) { 
		ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	});

	var self = this;
	ob.addOptionsTo("foreignTable").change(function() { self.foreignTableChange(this); }).change();
}

/**
 * Adds the foreign table column names to the table row, as well as the primary table names.  The
 * currently-selected foreign table is excluded from the list of primary table names.
 **/
JoinsTable.prototype.foreignTableChange = function(eventsrc) {
	// get the index of the selected table in the schema
	var seltableindex = eventsrc.options[eventsrc.selectedIndex].getAttribute("data-schemaIdx");
	// get the object representing the selected foreign table
	var foreignTable = this.project.schema[seltableindex];
	var ob = new OptionBuilder($(eventsrc).parent().parent());
	var pk = "";

	$.each(foreignTable.columns, function(i, column) {
		if ($.inArray(column, foreignTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column + (column == pk ? "*" : ""));
	});

	ob.addOptionsTo("foreignColumn");
	$.each(this.project.schema, function(i, table) { 
		if (table.name != foreignTable.name)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	});

	var self = this;
	ob.addOptionsTo("primaryTable").change(function() { self.primaryTableChange(this); }).change();
}

/**
 * Updates the primary table column name options to reflect the currently-selected primary table.
 **/
JoinsTable.prototype.primaryTableChange = function(eventsrc) {
	var seltableindex = eventsrc.options[eventsrc.selectedIndex].getAttribute("data-schemaIdx");
	var primaryTable = mainproject.schema[seltableindex];
	var ob = new OptionBuilder($(eventsrc).parent().parent());
	var pk = "";

	$.each(primaryTable.columns, function(i, column) {
		if ($.inArray(column, primaryTable.pkColumns) >= 0)
			pk = column;
		ob.addOption(column, "", column + (column == pk ? "*" : ""));
	});
	ob.addOptionsTo("primaryColumn").val(pk);
}




/**
 * EntitiesTable is a subclass of EditableTable that allows the user to add and modify entities
 * (concepts) in the triplifier interface.
 **/
function EntitiesTable(element) {
	// call the parent's constructor
	EntitiesTable.superclass.call(this, element);
}

// EntitiesTable inherits from EditableTable
EntitiesTable.prototype = new EditableTable();
EntitiesTable.prototype.constructor = EntitiesTable;
EntitiesTable.superclass = EditableTable;

EntitiesTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	EntitiesTable.superclass.prototype.setButtonStates.apply(this);

	// Set a custom message to display when the user clicks the "Delete" button.
	this.delete_msg = "Are you sure you want to delete the selected concept?  This will also delete any associated attributes and relations.";

	// See if any more entity mappings can be specified and set the state of the "add" button accordingly.
	var hasmoreents = this.project['entities'].length != this.project.getColumnCount();
	this.contentelem.children("input.add").prop("disabled", !hasmoreents);
}

/**
 * Construct a project item from the values of the form elements in the add/edit table row.
 * This method inspects the "unique ID" checkbox and defines an appropriate ID prefix for
 * the entity (concept).
 *
 * @param tablerow The table row object containing the form elements.
 * @returns An object containing the values for the new project item.
 **/
EntitiesTable.prototype.getItemFromFormRow = function(tablerow) {
	params = tablerow.formParams();

	var idprefix;
	if (params.uniqueID == 'on')
		// "Unique ID" is checked.
		idprefix = '';
	else
		// "Unique ID" is not checked.
		idprefix = params.table + '.' + params.idColumn + '_';

	// Build a new parameters object.  This is necessary to make sure the properties are in
	// the correct order.
	var newparams = { table:params.table, idColumn:params.idColumn, idPrefixColumn:idprefix,
		rdfClass:params.rdfClass };

	//console.log(newparams);
	return newparams;
}

/**
 * Construct a form parameters object from the properties of a concept item.  The idPrefixColumn
 * property of the concept item is mapped to the "uniqueID" checkbox on the HTML form.
 *
 * @param item A project item.
 * @returns A form parameters object.
 **/
EditableTable.prototype.getFormParamsFromItem = function(item) {
	var isunique = undefined;
	if (item.idPrefixColumn == '')
		isunique = 'on';

	var formparams = { table:item.table, idColumn:item.idColumn, uniqueID:isunique,
		rdfClass:item.rdfClass };

	return formparams;
}

/**
 * Add the properties of a concept item to the <td> elements of a table row.  This method is identical
 * to the parent class implementation except for handling the idPrefixColumn property, which is mapped
 * to the "Globally Unique ID" column in the UI.
 *
 * @param item The concept item to map to a table row.
 * @param td The first <td> element in the table row.
 *
 * @returns None.
 **/
EditableTable.prototype.mapItemToTableRow = function(item, td) {
	$.each(item, function(name, value) {
		var vKeys, valstr;

		// See if this value is an object or a string.
		if (value.substr) {
			vKeys = [];
			valstr = value;
		} else {
			// Extract the keys from the value object.
			vKeys = Object.keys(value);
			valstr = value[vKeys[0]];
		}
	
		// Write value to the next sibling <td>.
		td = td.next();
		if (name != 'idPrefixColumn') {
			td.html(valstr);
		} else {
			if (value == '')
				td.html('yes');
			else
				td.html('no');
		}
		td.attr("title", value[vKeys[1]]);
	});
}

/**
 * Adds the table names from the project to the table row.
 *
 * @param tr The table row with the input elements to populate.
 * @param isedit Specifies whether this is an "add" or "edit" request.
 **/
EntitiesTable.prototype.populateTableRowOptions = function(tr, isedit) {
	var ob = new OptionBuilder(tr);
	var entity = {};

	// If this is an edit operation, get the currently-selected entity from the project.
	if (isedit)
		entity = this.getSelectedItemFromProject();

	var self = this;

	// Determine which table names should be added to the "Table" drop-down list.
	$.each(this.project.schema, function(i, table) {
		if (table.name == entity.table || self.project.getEntityCntByTable(table.name) < table.columns.length)
			ob.addOption(table.name, "data-schemaIdx='" + i + "'");
	});

	// Add the options to the "Table" list.
	var tablelist = ob.addOptionsTo("table");
	
	tablelist.prop("disabled", !!entity.table);

	tablelist.change(function() { self.tableChanged(this, entity); });
	tablelist.change();

	this.authorRdfControls(tr, ob, "rdfClass", "classes");
}

/**
 * Updates the "ID Column" options to reflect the currently-selected table name.
 *
 * @param eventsrc The drop-down list that triggered the change event.
 * @param entity The entity item being edited.  If this is an "add" operation, this will be {}.
 **/
EntitiesTable.prototype.tableChanged = function(eventsrc, entity) {
	// get the index of the selected table in the schema
	var seltableindex = eventsrc.options[eventsrc.selectedIndex].getAttribute("data-schemaIdx");
	// get the object representing the selected entity table
	var entityTable = this.project.schema[seltableindex];

	var ob = new OptionBuilder($(eventsrc).parent().parent());
	var pk = "";

	// Determine which column names should be added to the drop-down list.
	var self = this;
	$.each(entityTable.columns, function(i, column) {
		// Only add the column if it is the column of the entity being edited or is not part
		// of another entity.
		if (column == entity.idColumn ||
		    indexOf(self.project.entities, "table", entityTable.name, "idColumn", column) < 0) {
			if ($.inArray(column, entityTable.pkColumns) >= 0)
				pk = column;
			ob.addOption(column, "", column + (column == pk ? "*" : ""));
		}
        });

	ob.addOptionsTo("idColumn").val(pk);
}




/**
 * AttributesTable is a subclass of EditableTable that allows the user to add and modify the attributes
 * of entities in the triplifier interface.
 **/
function AttributesTable(element) {
	// call the parent's constructor
	AttributesTable.superclass.call(this, element);
}

// AttributesTable inherits from EditableTable
AttributesTable.prototype = new EditableTable();
AttributesTable.prototype.constructor = AttributesTable
AttributesTable.superclass = EditableTable;

AttributesTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	AttributesTable.superclass.prototype.setButtonStates.apply(this);

	// See whether any new attributes can be defined.
	var hasmoreatts = this.project.entities.length < 1;
	var self = this;
	$.each(this.project.entities, function(i, entity) {
		var entname = entity.table + '.' + entity.idColumn;

		var attribs = self.project.getAttributesByEntity(entname);
		var table = self.project.getTableByName(entity.table);

		//alert(entname + ': ' + attribs.length + ', ' + table.columns.length);
		if (attribs.length < (table.columns.length - 1)) {
			// There are fewer attributes then columns, so we can still define more attributes.
			hasmoreatts = true;
		}
	});

	this.contentelem.children("input.add").prop("disabled", !hasmoreatts);
}

/**
 * Adds the entity names from the project to the table row.
 *
 * @param tr The table row with the input elements to populate.
 * @param isedit Specifies whether this is an "add" or "edit" request.
 **/
AttributesTable.prototype.populateTableRowOptions = function(tr, isedit) {
	var ob = new OptionBuilder(tr);
	var attribute = {};

	// If this is an edit operation, get the currently-selected attribute from the project.
	if (isedit)
		attribute = this.getSelectedItemFromProject();

	var self = this;

	// Determine which entity names should be added to the "Concept" drop-down list.  Only add an
	// entity if it is either the entity of the attribute currently selected for editing or still
	// has attributes left to define.
	$.each(this.project.entities, function(i, entity) {
		var entname = entity.table + '.' + entity.idColumn;
		if (attribute.entity == entname) {
			// This the entity of the attribute being edited.
			ob.addOption(entname, "entity-Idx='" + i + "'");
		} else {
			var attribs = self.project.getAttributesByEntity(entname);
			var table = self.project.getTableByName(entity.table);
			//alert(entname + ': ' + attribs.length + ', ' + table.columns.length);
			if (attribs.length < (table.columns.length - 1))
				// There are fewer attributes then columns, so add this entity to the list.
				ob.addOption(entname, "entity-Idx='" + i + "'");
		}
	});

	// Add the options to the "Concepts" list.
	var entitieslist = ob.addOptionsTo("entity");
	
	//tablelist.prop("disabled", !!entity.table);

	entitieslist.change(function() { self.entityChanged(this, attribute); });
	entitieslist.change();
}

/**
 * Updates the "Database columns" options to reflect the currently-selected entity.
 *
 * @param eventsrc The drop-down list that triggered the change event.
 * @param attribute The attribute item being edited.  If this is an "add" operation, this will be {}.
 **/
AttributesTable.prototype.entityChanged = function(eventsrc, attribute) {
	// get the index of the selected entity in the project
	var selentindex = eventsrc.options[eventsrc.selectedIndex].getAttribute("entity-Idx");
	// get the object representing the selected entity
	var entity = this.project.entities[selentindex];

	var tr = $(eventsrc).parent().parent();
	var ob = new OptionBuilder(tr);

	// Go through each column in the selected entity's table and add it to the list
	// if either it is the column of the attribute selected for editing or if it is
	// not already used in another attribute.
	var self = this;
	var attribs = self.project.getAttributesByEntity(entity.table + '.' + entity.idColumn);
	var vocab = vocabularyManager.getSelectedVocabulary();
	$.each(this.project.getTableByName(entity.table).columns, function(i, column) { 
		if (attribute.column == column) {
			ob.addOption(column);
		} else if (indexOf(attribs, "column", column) < 0 && entity.idColumn != column) {
			ob.addOption(column);
		}
	});

	ob.addOptionsTo("column");

	this.authorRdfControls(tr, ob, "rdfProperty", "properties", entity.rdfClass.uri);
}




/**
 * RelationsTable is a subclass of EditableTable that allows the user to add and modify the relations
 * between concepts in the triplifier interface.
 **/
function RelationsTable(element) {
	// call the parent's constructor
	RelationsTable.superclass.call(this, element);
}

// RelationsTable inherits from EditableTable
RelationsTable.prototype = new EditableTable();
RelationsTable.prototype.constructor = RelationsTable;
RelationsTable.superclass = EditableTable;

RelationsTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	RelationsTable.superclass.prototype.setButtonStates.apply(this);

	// See if any more relations can be specified and set the state of the "add" button accordingly.
	var hasmorerels = this.project['relations'].length != this.project.getAllPossibleRelations().count;
	//alert(this.project.getAllPossibleRelations().count);
	this.contentelem.children("input.add").prop("disabled", !hasmorerels);
}

/**
 * Adds the entity names from the project to the table row.
 *
 * @param tr The table row with the input elements to populate.
 * @param isedit Specifies whether this is an "add" or "edit" request.
 **/
RelationsTable.prototype.populateTableRowOptions = function(tr, isedit) {
	var ob = new OptionBuilder(tr);
	var relation = {};

	// If this is an edit operation, get the currently-selected relation from the project.
	if (isedit)
		relation = this.getSelectedItemFromProject();

	var self = this;
	var allrelations = this.project.getAllPossibleRelations();

	// Determine which entity names should be added to the "Subject" drop-down list.  Only add an
	// entity if it is either part of the relation currently selected for editing or there are
	// possible relations remaining that include it.
	$.each(allrelations.relations, function(i, relobj) {
		if (relobj.subject == relation.subject 
				|| relobj.subject == relation.object
				|| self.project.getRelationCountByEntity(relobj.subject) < relobj.objects.length)
			ob.addOption(relobj.subject, "data-allRelationIdx='" + i + "'");
	});

	// Add the options to the "Concepts" list.
	var subjectslist = ob.addOptionsTo("subject");
	
	subjectslist.change(function() { self.subjectChanged(this, relation); });
	subjectslist.change();

	// relationPredicates is a global defined in triplifier.js.  This is from the old system, and
	// for now it is staying the same.
	$.each(relationPredicates, function(i, predicate) {
		ob.addOption(predicate);
	});
	ob.addOptionsTo("predicate");
}

/**
 * Updates the "Object" options to reflect the currently-selected subject entity.
 *
 * @param eventsrc The drop-down list that triggered the change event.
 * @param attribute The relation item being edited.  If this is an "add" operation, this will be {}.
 **/
RelationsTable.prototype.subjectChanged = function(eventsrc, relation) {
	// get the index of the selected entity in the project's "all relations" object
	var relindex = eventsrc.options[eventsrc.selectedIndex].getAttribute("data-allRelationIdx");
	// get the object representing all relations for the selected entity
	var relobj = this.project.getAllPossibleRelations().relations[relindex];

	var tr = $(eventsrc).parent().parent();
	var ob = new OptionBuilder(tr);

	// Decide which entities to add to the "Objects" drop-down list.  Entities are only added if
	// they are part of the relation currently selected for editing or if they are not already part
	// of a relation with the selected subject entity.
	var self = this;
	$.each(relobj.objects, function(i, object) {
		if (object == relation.object || object == relation.subject
				|| !self.project.getRelationByEntities(relobj.subject, object))
			ob.addOption(object);
	});

	ob.addOptionsTo("object");
}
