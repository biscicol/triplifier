/**
 * Implements an HTML table-based UI widget that has the ability to dynamically add, remove,
 * and edit table rows.  An EditableTable is associated with a Project object and displays and
 * allows the user to edit a specific property of the project (e.g., joins, relations, etc.).
 *
 * This is a base class that implements all shared, generic functionality for all child classes
 * that control specific project properties.  This base class implements all display and event-
 * handling functionality, as well as basic methods to allow adding, editing, and deleting rows
 * from the table.
 *
 * Child classes need to do two things.  First, extend setButtonStates() to properly set the
 * state of the "add" button; second, provide an implementation of populateTableRowOptions() to
 * handle setting the input option values for add/edit operations.
 **/
function EditableTable(element) {
	// If element is null, the constructor is being called merely for inheritance purposes,
	// so exit without creating any "own" properties.
	if (element == null)
		return;

	this.element = element;
	this.contentelem = element.children("div.sectioncontent");
	this.property = null;
	this.project = null;
	// track whether this table is active
	this.isactive = false;
	// keep track of the selected table row
	this.selectedtr = null;
	// keep track of the index (in both the table and the project object) of the selected table row
	this.selrowindex = -1;

	// Set the message to display when the user clicks the "Delete" button.
	this.delete_msg = "Are you sure you want to delete the selected row?";
	
	this.contentelem.addClass("flexTable");
	//this.contentelem.children("table").hide();

	// Get the DOM for the rows of the table that we want to use as templates for creating new rows.
	this.templates = {};
	this.templates.edit = this.contentelem.children("table").children("tbody").children("tr.edit").remove();
	this.templates.display = this.contentelem.children("table").children("tbody").children(":last").remove();

	// Set event handlers.
	var self = this;
	this.contentelem.children("input.add").click(function() { self.addButtonClicked(); });
	this.contentelem.children("input.delete").click(function() { self.deleteButtonClicked(); });
	this.contentelem.children("input.edit").click(function() { self.editButtonClicked(); });
}

/**
 * Set the project to associate with this EditableTable.
 **/
EditableTable.prototype.setProject = function(project, projectproperty) {
	this.project = project;
	this.property = projectproperty;

	// Register this EditableTable as an observer of the project.
	this.project.registerObserver(this);

	this.resetRowsData();
}

/**
 * Respond to changes in this EditableTable's project that were not directly requested by
 * this EditableTable itself.
 **/
EditableTable.prototype.projectPropertyChanged = function(project, propname) {
	//alert('EditableTable (' + this.property + '): property changed (' + propname + ')');
	// We are only concerned about changes to this EditableTable's property.
	if (propname == this.property) {
		//alert('EditableTable (' + this.property + '): property changed (' + propname + ')');
		this.resetRowsData();
	}
}

/**
 * Set the activation state of this EditableTable.  If the table is not active, the controls
 * are hidden and the table and text are not highlighted.
 **/
EditableTable.prototype.setActive = function(isactive) {
	this.element.toggleClass("active", isactive);

	var inputs = this.contentelem.find("input");
	var notAnimate = false;
	if (notAnimate)
		inputs.toggle(isactive);
	else
		inputs.fadeToggle(isactive);

	if (isactive)
		this.setButtonStates();

	// Set the table visibility, if necessary.
	if (this.property && !this.project[this.property].length) {
	       if (isactive)
			this.contentelem.children("table").show()
		else
			this.contentelem.children("table").hide()
	}

	this.isactive = isactive;
}

/**
 * Re-populates the table based upon the values in the current project object.
 **/
EditableTable.prototype.resetRowsData = function() {
	// get the table element
	var table = this.contentelem.children("table");

	// remove all existing table rows
	table.children("tbody").children().remove();

	// process each data item, converting it to a table row
	var self = this;
	$.each(this.project[this.property], function(i, item) {
		//alert(item);
		self.createTableRow(item).appendTo(table);
	});

	// Set the first row in the table to be selected, if it exists, and make the
	// table visible.  
	if (this.project[this.property].length) {
		var selectedinput = this.contentelem.find("table input:radio").first().prop("checked", true);
		this.selectedtr = selectedinput.parent().parent();
		this.selrowindex = this.selectedtr.index();

		table.show();
	} else if (!this.isactive) {
		table.hide();
	}

	this.setButtonStates();
}

/**
 * Enable/disable this table's "add", "edit", and "delete" buttons as necessary.  This implementation
 * does not set the state of the "add" button, because doing so depends on the table's context in the
 * project.  Thus, this method should be extended by child classes.
 **/
EditableTable.prototype.setButtonStates = function() {
	// See if the "edit" and "delete" buttons should be enabled or disabled, depending on
	// the state of the project.
	if (this.property != null)
		this.contentelem.children("input.edit, input.delete").prop("disabled", !this.project[this.property].length);
}

/**
 * Create a new row in the table, using the values of the specified item
 * to populate the <td> elements in the row.
 **/
EditableTable.prototype.createTableRow = function(item) {
	var newrow = this.templates.display.clone();
	var td = newrow.children().first(); // first td

	// set the handler for the radio button change event and make this row selected
	var self = this;
	td.children("input").change(function() { self.selectionChanged(this); }).prop("checked", true);

	//td.children("input").change(radioChange).prop("checked", true);

	$.each(item, function(name, value) { 
		var vKeys = (value.substr ? [] : Object.keys(value)); // extract keys from Object-like value
		td = td.next().html(value[vKeys[0]] || value).attr("title", value[vKeys[1]]); // write value to next sibling (td)
	});

	return newrow;
}

/**
 * Whenever a different row in the table is selected via the radio buttons, keep track of the
 * selected row and its index.
 **/
EditableTable.prototype.selectionChanged = function(srcelement) {
	this.selectedtr = $(srcelement).parent().parent();
	this.selrowindex = this.selectedtr.index();
}

/**
 * Deletes the selected row from the table and the project.
 **/
EditableTable.prototype.deleteButtonClicked = function() {
	if (confirm(this.delete_msg)) {
		var projelement = this.project.getProperty(this.property);
		// Remove the item.
		projelement.splice(this.selrowindex, 1);

		// Save the new version of the project element.  Tell the project that we don't
		// need to be notified of this change.
		this.project.setProperty(this.property, projelement, this);

		this.resetRowsData();
	}
}

/**
 * Add a new row to the table with the UI controls for creating a new project element.
 **/
EditableTable.prototype.addButtonClicked = function() {
	var tr = this.templates.edit.clone();
	var self = this;

	tr.find("input.save").click(function() { self.saveRowInput(this); }).prop("disabled", false); // disabled: firefox bug fix
	tr.find("input.cancel").click(function() { self.cancelRowInput(this); }).prop("disabled", false); // disabled: firefox bug fix

	this.populateTableRowOptions(tr, false);

	// add the input row to the table and make sure the table is visible
	tr.appendTo(this.contentelem.children("table").show())

	this.styleEdit(tr);	
}

/**
 * Add a new row to the table for editing the currently selected row.
 **/
EditableTable.prototype.editButtonClicked = function() {
	var tr = this.templates.edit.clone();
	var self = this;

	tr.find("input.save").click(function() { self.saveEditedRowInput(this); }).prop("disabled", false); // disabled: firefox bug fix
	tr.find("input.cancel").click(function() { self.cancelRowInput(this); }).prop("disabled", false); // disabled: firefox bug fix

	this.populateTableRowOptions(tr, true);

	// set the form values to match those of the targeted row
	tr.formParams(this.project[this.property][this.selrowindex]);

	// add the input row to the table immediately after the selected row
	tr.insertAfter(this.selectedtr);

	this.styleEdit(tr, false, true);
}

/**
 * Add the appropriate values from the project to the table row input options.  This should
 * be concretely implemented by all child classes.
 **/
EditableTable.prototype.populateTableRowOptions = function(tr) {
}

/**
 * Set the styles for the table depending on whether we are adding or editing a row.
 **/
EditableTable.prototype.styleEdit = function(tr, isEnd, isEdit) {
	// set the editing style on the table header and the row selected for editing, if applicable
	tr.parent().siblings("thead").toggleClass("editData", !isEnd);
	if (isEdit && this.selectedtr)
		this.selectedtr.toggleClass("editData", !isEnd);

	$("#overlay").fadeToggle(!isEnd);
}

/**
 * Cancel the edit/add operation by removing the editing row from the table.
 **/
EditableTable.prototype.cancelRowInput = function(srcelement) {
	var tr = $(srcelement).parent().parent();

	this.styleEdit(tr, true);

	if (!tr.siblings().size()) 
		tr.closest("table").hide();
	tr.remove();
}

/**
 * Saves a newly-created row to the project and updates the table display.
 **/
EditableTable.prototype.saveRowInput = function(srcelement) {
	var tr = $(srcelement).parent().parent();

	// Read form values.
	var item = tr.formParams();

	// Get the element from the project and update it.
	var projelement = this.project.getProperty(this.property);
	projelement.push(item);

	// Save the new version of the project element.  Tell the project that we don't
	// need to be notified of this change.
	this.project.setProperty(this.property, projelement, this);

	this.setButtonStates();
		
	var newtr = this.createTableRow(item).replaceAll(tr);
	// fire the change event so the selected row index gets updated
	newtr.children().first().children("input").change();

	this.styleEdit(newtr, true);
}

/**
 * Save an edited table row to the project and updates the table display.
 **/
EditableTable.prototype.saveEditedRowInput = function(srcelement) {
	var tr = $(srcelement).parent().parent();

	// read form values
	var item = tr.formParams();
	
	// get the appropriate project element
	var projelement = this.project.getProperty(this.property);
	// get the old item from the project
	var olditem = projelement[this.selrowindex];

	// update the values for this item
	$.each(item, function(name, value) { // copy values from form to mapping/attribute
		olditem[name] = value;
	});

	// Save the new version of the project element.  Tell the project that we don't
	// need to be notified of this change.
	this.project.setProperty(this.property, projelement, this);

	// trigger the edited method for observers here

	// remove the old table row displaying this item
	tr.prev().remove();

	var newtr = this.createTableRow(item).replaceAll(tr);
	// fire the change event so the selected row index gets updated
	newtr.children().first().children("input").change();

	this.styleEdit(newtr, true);
}

/**
 * Get the currently-selected item from the project.
 **/
EditableTable.prototype.getSelectedItemFromProject = function() {
	// get the currently-selected item from the project
	var projelement = this.project.getProperty(this.property);
	var item = projelement[this.selrowindex];

	return item;
}

/**
 * Use the VocabularyManager (initialized in triplifier.js) to populate the RDF element drop-down lists.
 * This is a relic from the old system and should be refactored at some point -- it still relies on a
 * global variable called vocabularyManager.  Not very pretty.
 **/
EditableTable.prototype.authorRdfControls =  function(tr, ob, element, items, entityClass) {
	vocabularyManager.onChangeFn(function() {
		var vocabulary = vocabularyManager.getSelectedVocabulary();
		var hasItems = vocabulary && vocabulary[items] && vocabulary[items].length;

		if (hasItems) {
			$.each(vocabulary[items], function(i, item) {
				if (!entityClass || !item.domain || $.inArray(entityClass, item.domain) >= 0)
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




/**
 * OptionBuilder helps add input option values to a table row.  It is a utility class that can
 * be used by concrete implementations of populateTableRowOptions() in EditableTable.
 **/
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

	this.hide = function(name) {
		container.find("select[name='" + name + "']").hide();
	};

	this.show = function(name) {
		container.find("select[name='" + name + "']").show();
	};
}

