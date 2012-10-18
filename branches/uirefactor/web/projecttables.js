/**
 * JoinsTable is a subclass of EditableTable that allows the user to add and modify joins in
 * the triplifier interface.
 **/
function JoinsTable(element) {
	// call the parent's constructor
	this.superclass(element);
}

// JoinsTable inherits from EditableTable
JoinsTable.prototype = new EditableTable();
JoinsTable.prototype.superclass = EditableTable;

JoinsTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	this.superclass.prototype.setButtonStates.apply(this);

	// See if more joins are possible and set the state of the "add" button accordingly.
	var hasmorejoins = this.project.joins.length != this.project.schema.length - 1;
	this.element.children("input.add").prop("disabled", !hasmorejoins);
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
 * This function removes any relations that have been invalidated by an edited join.  I'll deal with this later.
 * I will implement this by having one FlexTable act as the observer of another and updating itself whenever edits
 * are made to the observed table.
 **/
function onJoinModify(oldJoin, newJoin) {
	if (!newJoin || oldJoin.foreignTable != newJoin.foreignTable || oldJoin.primaryTable != newJoin.primaryTable) {
		relationFT.removeMatching(function(relation) {
			return relation.subject.indexOf(oldJoin.foreignTable + ".") == 0 && relation.object.indexOf(oldJoin.primaryTable + ".") == 0
				|| relation.subject.indexOf(oldJoin.primaryTable + ".") == 0 && relation.object.indexOf(oldJoin.foreignTable + ".") == 0;
		});
	}
}



/**
 * EntitiesTable is a subclass of EditableTable that allows the user to add and modify entities in
 * the triplifier interface.
 **/
function EntitiesTable(element) {
	// call the parent's constructor
	this.superclass(element);
}

// EntitiesTable inherits from EditableTable
EntitiesTable.prototype = new EditableTable();
EntitiesTable.prototype.superclass = EditableTable;

EntitiesTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	this.superclass.prototype.setButtonStates.apply(this);

	// See if any more entity mappings can be specified and set the state of the "add" button accordingly.
	var hasmoreents = this.project['entities'].length != this.project.getColumnCount();
	this.element.children("input.add").prop("disabled", !hasmoreents);

	// See whether any new attributes can be defined.
	/*var hasmoreatts = false;
	if (this.selrowindex != -1) {
		var curent = this.project['entities'][this.selrowindex];
		var table = this.project.findTable(curent.table);
		if (table) {
			if (table.columns.length > curent.attributes.length)
				hasmoreatts = true;
		}
	}
	this.element.children("input.add2").prop("disabled", !hasmoreatts);*/
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

	tablelist.change(function() { self.TableChanged(this, entity); });
	tablelist.change();

	this.authorRdfControls(tr, ob, "rdfClass", "classes");
}

/**
 * Updates the "ID Column" options to reflect the currently-selected table name.
 *
 * @param eventsrc The drop-down list that triggered the change event.
 * @param entity The entity item being edited.  If this is an "add" operation, this will be {}.
 **/
EntitiesTable.prototype.TableChanged = function(eventsrc, entity) {
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
	this.superclass(element);
}

// AttributesTable inherits from EditableTable
AttributesTable.prototype = new EditableTable();
AttributesTable.prototype.superclass = EditableTable;

AttributesTable.prototype.setButtonStates = function() {
	// call the superclass implementation
	this.superclass.prototype.setButtonStates.apply(this);

	// See if any more attribute mappings can be specified and set the state of the "add" button accordingly.
	var hasmoreents = this.project['entities'].length != this.project.getColumnCount();

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

	this.element.children("input.add").prop("disabled", !hasmoreatts);
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

	entitieslist.change(function() { self.EntityChanged(this, attribute); });
	entitieslist.change();
}

/**
 * Updates the "Database columns" options to reflect the currently-selected entity.
 *
 * @param eventsrc The drop-down list that triggered the change event.
 * @param attribute The attribute item being edited.  If this is an "add" operation, this will be {}.
 **/
AttributesTable.prototype.EntityChanged = function(eventsrc, attribute) {
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

