/**
 * Implements shared, generic functionality for all triplifier project sections.  A "section" includes
 * all of the content inside one of the HTML section <div>s.  This top-level base class provides
 * basic activate/deactive functionality.
 **/
function ProjectSection(element) {
	// If element is null, the constructor is being called merely for inheritance purposes,
	// so exit without creating any "own" properties.
	if (element == null) {
		return;
	}

	// An array for keeping track of observers of this ProjectSection.
	this.observers = [];

	this.element = element;
	this.contentelem = element.children("div.sectioncontent");

	// track whether this section is active
	this.isactive = undefined;

	// track whether this section's title link is enabled
	this.isenabled = undefined;
	
	this.contentelem.addClass("flexTable");
	//this.contentelem.children("table").hide();
	
	// Set up the handler for clicks on the section title.
	this.titleanchor = this.element.children('h2').children('a').first();
	this.setEnabled(true);
}

/**
 * Register an observer of this ProjectSection.  Observers are notified when a ProjectSection
 * is activated.  To be a project observer, an object must provide the following method:
 *
 * projectSectionActivated(projectsection) { ... }.
 *
 * The argument "projectsection" references the ProjectSeciton object that triggered the event.
 **/
ProjectSection.prototype.registerObserver = function(observer) {
	this.observers.push(observer);
}

/**
 * Remove an object from this ProjectSection's list of observers.
 **/
ProjectSection.prototype.unregisterObserver = function(observer) {
	for (var cnt = this.observers.length - 1; cnt >= 0; cnt--) {
		if (this.observers[cnt] === observer) {
			// Remove the observer from the list.
			this.observers.splice(cnt, 1);
		}
	}
}

/**
 * Notifies observers that this section was activated.
 **/
ProjectSection.prototype.notifySectionActivated = function() {
	for (var cnt = 0; cnt < this.observers.length; cnt++) {
		this.observers[cnt].projectSectionActivated(this);
	}
}

/**
 * Set the activation state of this ProjectSection.
 **/
ProjectSection.prototype.setActive = function(isactive) {
	if (isactive != this.isactive) {
		this.element.toggleClass("active", isactive);

		// Show or hide any input elements in this section.
		var inputs = this.contentelem.find("input");
		inputs.fadeToggle(isactive);

		// Shor or hide any extra activatable content in this section.
		var activatablecontent = this.contentelem.find("span.activatablesectioncontent");
		activatablecontent.fadeToggle(isactive);

		this.isactive = isactive;

		if (isactive)
			this.notifySectionActivated();
	}
}

/**
 * Set whether or not the section title link is this ProjectSection is enabled.
 **/
ProjectSection.prototype.setEnabled = function(isenabled) {
	if (this.isenabled == isenabled)
		return;

	if (isenabled) {
		var self = this;
		this.titleanchor.click(function() { return self.titleClicked(); });
		this.titleanchor.attr('href', '#');
		this.titleanchor.toggleClass('disabled', false);
	} else {
		this.titleanchor.removeAttr('href');
		this.titleanchor.off('click');
		this.titleanchor.toggleClass('disabled', true);
	}

	this.isenabled = isenabled;
}

/**
 * Handle clicks on the section title.
 **/
ProjectSection.prototype.titleClicked = function() {
	if (!this.isactive)
		this.setActive(true);

	return false;
}




/**
 * SectionManager keeps track of an arbitrary number of project sections and makes sure that they
 * work together properly.  Currently, this just means making sure that only one section is open
 * at a time.
 **/
function SectionManager() {
	// Tracks the section that is currently active.
	this.activesection = null;

	// Keeps references to all of the sections being managed.
	this.sections = [];
}

/**
 * Adds one or more sections to this SectionManager.
 *
 * @param ...  One or more ProjectSections to manage.
 **/
SectionManager.prototype.addSections = function(/* ... */) {
	for (var cnt = 0; cnt < arguments.length; cnt++) {
		this.sections.push(arguments[cnt]);
		arguments[cnt].registerObserver(this);
	}
}

/**
 * Handles a section activation event.  Whenever a section is activated, the SectionManager will close
 * the previously-active section, if there was one.
 **/
SectionManager.prototype.projectSectionActivated = function(projectsection) {
	if (projectsection !== this.activesection) {
		if (this.activesection != null) {
			this.activesection.setActive(false);
		}
		this.activesection = projectsection;
	}
}

/**
 * Enables or disables one or more ProjectSections.
 *
 * @param isenabled  Whether the section(s) is/are enabled or disabled.
 * @param ...  One or more ProjectSections to enable or disable.
 **/
SectionManager.prototype.setSectionsEnabled = function(isenabled /*, ...*/) {
	for (var cnt = 1; cnt < arguments.length; cnt++) {
		arguments[cnt].setEnabled(isenabled);
	}
}




/**
 * DataSourceSection controls the UI elements for the "Data Source" section of the triplifier interface.
 * It implements all of the functionality to manage connecting to and uploading data sources.
 *
 * @param element The HTML <div> element that contains the UI for the DatSourceSection.
 * @param element The "Triplify" ProjectSection of the interface.  This is necessary so that this section
 *                can receive the focus after successful "simplification."
 **/
function DataSourceSection(element, triplifyPS) {
	// call the parent's constructor
	DataSourceSection.superclass.call(this, element);

	this.project = null;
	this.triplifyPS = triplifyPS;

	// Get the DOM for the table row that we want to use as a template for creating new rows.
	this.dbSourceTrTemplate = $("#schemaTable > tbody").children(":last").remove();

	// Create a SimplifierFactory.
	this.simpfact = new SimplifierFactory();

	// Add the supported simplifier formats to the UI drop-down list.
	var formatsel = document.getElementById('uploadForm').inputdataformat;
	var formatsmap = this.simpfact.getFormatsMap();
	for (formatcode in formatsmap) {
		var opt = new Option(formatsmap[formatcode], formatcode);
		formatsel.add(opt);
	}

	// Add an event handler to the simplify checkbox so that the format drop-down list is disabled
	// if the checkbox is unchecked.
	$('#uploadForm > input[name=simplify]').click(function() {
		formatsel.disabled = !this.checked
	});

	// Set event handlers for the data source form submit buttons.
	var self = this;
	$("#dbForm").submit(function(){ return self.databaseFormSubmitted(this); });
	$("#uploadForm").submit(function(){ return self.uploadFormSubmitted(this); });
}

// DataSourceSection inherits from ProjectSection.
DataSourceSection.prototype = new ProjectSection();
DataSourceSection.prototype.constructor = DataSourceSection;
DataSourceSection.superclass = ProjectSection;

/**
 * Set the project to associate with this DataSourceSection.
 **/
DataSourceSection.prototype.setProject = function(project) {
	this.project = project;

	// Register this EditableTable as an observer of the project.
	//this.project.registerObserver(this);
	
	// Fill in the database form.  Leave it blank if the source was a sqlite database.
	var self = this;
	if (this.project.connection.system) {
		$.each($("#dbForm").get(0), function(i, element) {
			if (element.type != "submit")
				element.value = (self.project.connection.system == "sqlite" ? "" : (self.project.connection[element.name] || ""));
		});
	}

	this.updateSchemaUI();
}

/**
 * Set the activation state of this DataSourceSection.
 **/
DataSourceSection.prototype.setActive = function(isactive) {
	// Call the superclass implementation.
	DataSourceSection.superclass.prototype.setActive.call(this, isactive);

	$("#dsDiv").toggleClass("active", isactive);
	$("#dbForm, #uploadForm").fadeToggle(isactive);
	$("#dsDescription, #schemaTable").fadeToggle(!!this.project.schema.length);	

	return true;
}

/**
 * Updates the data source description and the schema table in the "Data Source" section so that
 * they match the contents of the project.
 **/
DataSourceSection.prototype.updateSchemaUI = function() {
	// update schema
	$("#dsDescription").html(this.getDataSourceName() + ", accessed: " + this.project.dateTime);

	var schemaTable = $("#schemaTable");
	var columns;
	schemaTable.children("tbody").children().remove();

	var self = this;
	$.each(this.project.schema, function(i, table) {
		columns = "";
		$.each(table.columns, function(j, column) { 
			columns += column + ($.inArray(column, table.pkColumns) >= 0 ? "*" : "") + ", ";
		});
		columns = columns.substr(0, columns.length - 2); // remove last comma
		self.dbSourceTrTemplate.clone().children()
			.first().html(table.name) // write table name to first td
			.next().html(columns) // write columns to second td
			.end().end().end().appendTo(schemaTable);
	});

	// If the project contains a data source, make sure the relevant parts of the UI are visible.
	$("#dsDescription, #schemaTable").fadeToggle(!!this.project.schema.length);	
}

/**
 * Constructs and returns a URI string for the data source.
 **/
DataSourceSection.prototype.getDataSourceName = function() {
   // NOTE: d2rq was re-writing dataSourceName() beginning with file: This was frustrating, so
   // i opted instead to use the BiSciCol namespace.  Ultimately, we want users to have some
   // control over the identity of their published dataset.
   //var name= project.connection.system == "sqlite"
	//    ? "file:" + project.connection.database.substr(0, project.connection.database.length-7)
	//	: "database:" + project.connection.database + "@" + project.connection.host;
	var name;

	if (this.project.connection.system == "sqlite")
		name = "urn:x-biscicol:" + this.project.connection.database.substr(0, this.project.connection.database.length-7);
	else
		name = "urn:x-biscicol:" + this.project.connection.database + "@" + this.project.connection.host;
	
	// remove leading and trailing space
	name = $.trim(name);

	return name.replace(/ /g,'_');
}

/**
 * Responds to submit events on the data source upload form.  This method checks the upload
 * form data and provides a handler for the finished upload event.
 **/
DataSourceSection.prototype.uploadFormSubmitted = function(evtsrc) {
	// Make sure a file name was provided.
	if (!evtsrc.file.value) {
		alert("Please select a file to upload.");
		evtsrc.file.focus();
		return false;
	}
	// If the simplify checkbox is checked, make sure a file format is selected.
	if (evtsrc.simplify.checked && evtsrc.inputdataformat.selectedIndex < 1) {
		alert("Please choose an input file format.");
		evtsrc.inputdataformat.focus();
		return false;
	}

	var self = this;
	setStatus("Uploading file:</br>'" + evtsrc.file.value + "'");
	$("#uploadTarget").one("load", function(){ self.afterUpload(); });

	return true;
}

/**
 * Called after an input file is uploaded.
 **/
DataSourceSection.prototype.afterUpload = function() {
	setStatus("");

	var data = frames.uploadTarget.document.body.textContent;
	// distinguish response OK status by JSON format
	if (isJson(data))
		this.processFileData(JSON.parse(data));
	else
		alert("Error" + (data ? ":\n\nUnable to contact server for data upload\nResponse="+data : "."));
}

/**
 * Responds to submit events on the database connection form.
 **/
DataSourceSection.prototype.databaseFormSubmitted = function(evtsrc) {
	// validate form
	if (!evtsrc.host.value) {
		alert("Please enter host address.");
		return false;
	}
	if (!evtsrc.database.value) {
		alert("Please enter database.");
		return false;
	}
	
	var self = this;
	setStatus("Connecting to database:</br>'" + evtsrc.host.value + "'");
	$.ajax({
		url: "rest/inspect",
		type: "POST",
		data: JSON.stringify($("#dbForm").formParams()),//$("#dbForm").serialize(),
		contentType:"application/json; charset=utf-8",
		dataType: "json",
		success: function(inspection) { self.readDatabaseMapping(inspection); },
		error: alertError
	});

	return false;
}

/**
 * Called upon successful completion of a request to get a database mapping object.
 **/
DataSourceSection.prototype.readDatabaseMapping = function(inspection) {
	setStatus("");

	this.project.setProperty('dateTime', inspection["dateTime"]);
	this.project.setProperty('connection', inspection["connection"]);
	this.project.setProperty('schema', inspection["schema"]);
	if (!this.project.joins || !this.project.joins.length)
		this.project.setProperty('joins', inspection["joins"]);
	if (!this.project.entities || !this.project.entities.length)
		this.project.setProperty('entities', inspection["entities"]);
	if (!this.project.relations || !this.project.relations.length)
		this.project.setProperty('relations', inspection["relations"]);

	this.updateSchemaUI();
}

/**
 * Called upon successful completion of a data file upload.
 **/
DataSourceSection.prototype.processFileData = function(inspection) {
	setStatus("");

	this.project.setProperty('dateTime', inspection["dateTime"]);
	this.project.setProperty('connection', inspection["connection"]);
	this.project.setProperty('schema', inspection["schema"]);

	this.updateSchemaUI();

	// See if we should try to "simplify" the input data.
	if ($('#uploadForm > input[name=simplify]').prop('checked')) {
		// Get the selected format from the UI drop-down list.
		var formatlist = $('#uploadForm > select[name=inputdataformat]');
		var formatcode = formatlist.prop('options')[formatlist.prop('selectedIndex')].value;

		// Get an appropriate Simplifier from the SimplifierFactory.
		simplifier = this.simpfact.getSimplifier(formatcode);

		// "Simplify" the input data schema.
		if (simplifier.simplify(this.project))
			// If simplification succeeded, jump straight to the "Triplify" section.
			this.triplifyPS.setActive(true);
		else
			// Otherwise, choose the appropriate section to activate.
			updateProjectSections();
	}
}


