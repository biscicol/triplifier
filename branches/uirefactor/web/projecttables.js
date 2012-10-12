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
 **/
JoinsTable.prototype.populateTableRowOptions = function(tr) {
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
 * the triplifier interface.  This is the most complicated of the project tables, because we need to
 * handle both "entities" and their attributes.  Thus, many of the methods in EditableTable need to
 * be extended to provide the additional functionality.
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

	// See if any more entities can be specified and set the state of the "add" button accordingly.
	var hasmoreents = this.project['entities'].length != this.project.getColumnCount();
	this.element.children("input.add").prop("disabled", !hasmoreents);

	// See whether any new attributes can be defined.
	var hasmoreatts = false;
	if (this.selrowindex != -1) {
		var curent = this.project['entities'][this.selrowindex];
		var table = this.project.findTable(curent.table);
		if (table) {
			if (table.columns.length > curent.attributes.length)
				hasmoreatts = true;
		}
	}
	this.element.children("input.add2").prop("disabled", !hasmoreatts);
}


