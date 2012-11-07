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

	this.element = element;
	this.contentelem = element.children("div.sectioncontent");

	// track whether this section is active
	this.isactive = false;
	
	this.contentelem.addClass("flexTable");
	//this.contentelem.children("table").hide();
}

/**
 * Set the activation state of this ProjectSection.
 **/
ProjectSection.prototype.setActive = function(isactive) {
	this.element.toggleClass("active", isactive);

	var inputs = this.contentelem.find("input");
	inputs.fadeToggle(isactive);

	this.isactive = isactive;
}




/**
 * DataSourceSection controls the UI elements for the "Data Source" section of the triplifier interface.
 * It implements all of the functionality to manage connecting to and uploading data sources.
 **/
function DataSourceSection(element) {
	// call the parent's constructor
	DataSourceSection.superclass.call(this, element);

	this.project = null;

	// Get the DOM for the table row that we want to use as a template for creating new rows.
	this.dbSourceTrTemplate = $("#schemaTable > tbody").children(":last").remove();

	// Set event handlers for the data source form submit buttons.
	var self = this;
	$("#dbForm").submit(function(){ self.databaseFormSubmitted(this); });
	$("#uploadForm").submit(function(){ self.uploadFormSubmitted(this); });
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
	if (mainproject.connection.system) {
		$.each($("#dbForm").get(0), function(i, element) {
			if (element.type != "submit")
				element.value = (mainproject.connection.system == "sqlite" ? "" : (mainproject.connection[element.name] || ""));
		});
	}

	this.updateSchemaUI();
}

/**
 * Set the activation state of this DataSourceSection.
 **/
DataSourceSection.prototype.setActive = function(isactive) {
	// call the superclass implementation
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
	if (!evtsrc.file.value) {
		alert("Please select a file to upload.");
		evtsrc.file.focus();
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
		this.readDatabaseMapping(JSON.parse(data));
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
		success: function(inspection) { self.readMapping(inspection); },
		error: alertError
	});

	return false;
}

/**
 * Called upon successful completion of an AJAX request to get a database mapping object.
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


