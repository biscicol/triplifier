/**
 * Implements all UI functionality for managing projects.  Essentially, this provides a user
 * interface for a ProjectManager.  It requires properly-defined HTML UI elements to function,
 * as explained below.
 * 
 * @param UIdiv {jQuery} DOM element containing all project controls. Example html:
 *
 * @codestart html
 * <div id="projects">
 *	<div class="functions">
 *		<form class="new" action="">
 *			New Project:
 *			<input type="text" name="project" size="20" />
 *			<input type="submit" value="Create" />
 *		</form>
 *		<form class="export" method="post" action="rest/download">
 *			<input type="hidden" name="filename" />
 *			<input type="hidden" name="content" />
 *			<input type="submit" value="Export" class="external" />
 *		</form>
 *		<input type="file" class="importFile" />
 *		<input type="button" class="import" value="Import" />
 *		<input type="button" class="delete" value="Delete" />
 *		<input type="button" class="deleteAll" value="Delete ALL" class="external" />
 *	</div>
 *	<h3>Projects:</h3>
 *	<div class="project"><input type='radio' name='projectChoice' /><label /></div>
 * </div>
 * @codeend
 * @param projectmanager {Object} A ProjectManager to interact with.
 */
function ProjectUI(UIdiv, projectmanager) {
	this.projman = projectmanager;
	this.UIdiv = UIdiv;
	this.lastProject = 0;	// Used to assign IDs to project DOM elements (needed for labels to work).
	this.projectTemplate = UIdiv.find("div.project").remove(); // Used to create project DOM elements.

	// An array for keeping track of observers of this ProjectUI.
	this.observers = [];

	// Keep track of the currently-selected project.
	this.selproject = null;

	// Save a local reference to this ProjectUI object.
	var self = this;

	// assign event handlers
	UIdiv.find("form.new").submit(function() { self.createProjectClicked(this); return false; });
	var exportform = UIdiv.find("form.export");
	exportform.submit(function() { self.exportProjectClicked(exportform); return true; });
	UIdiv.find("input.importFile").change(function() { self.importProjectFileSelected(); });
	UIdiv.find("input.import").click(function() { UIdiv.find("input.importFile").val("").click(); });
	UIdiv.find("input.delete").click(function() { self.deleteProjectClicked(); });
	UIdiv.find("input.deleteAll").click(function() { self.deleteAllClicked(); });

	var projDOM = []
	var projnames = this.projman.getProjectNames();

	// Generate radio-button DOM <div> for each project name.
	$.each(projnames, function(i, projname) { 
		projDOM.push(self.createProjectElement(projname, self.lastProject++).get(0));
	});

	if (this.projman.getProjectCnt() > 0)
		// Add the radio-button HTML to the project UI. and trigger the change() event on the first
		// project button in order to load it.
		$(projDOM).appendTo(UIdiv);
	else {
		// No projects exist yet, so create a new empty project.
		this.projman.newProject('new project');
		this.addProjectToUI('new project');
	}
}

/**
 * Register an observer of this ProjectUI.  Observers are notified whenever the currently-
 * selected project changes.  To be an observer, an object must provide the following method:
 *
 * projectSelectionChanged(project) { ... }.
 *
 * The argument "project" references the currently-selected project.
 **/
ProjectUI.prototype.registerObserver = function(observer) {
	this.observers.push(observer);
}

/**
 * Remove an object from this ProjectUI's list of observers.
 **/
ProjectUI.prototype.unregisterObserver = function(observer) {
	for (var cnt = this.observers.length - 1; cnt >= 0; cnt--) {
		if (this.observers[cnt] === observer) {
			// Remove the observer from the list.
			this.observers.splice(cnt, 1);
		}
	}
}

/**
 * Notify all observers of a change in the currently-selected project.
 **/
ProjectUI.prototype.notifyProjectSelectionChange = function(projname) {
	for (var cnt = 0; cnt < this.observers.length; cnt++) {
		this.observers[cnt].projectSelectionChanged(projname);
	}
}

/**
 * Tells this ProjectUI to automatically select a project from the list
 * of available projects.  The first project in the list of radio
 * buttons will be selected.
 **/
ProjectUI.prototype.selectDefaultProject = function() {
	// Select the first project radio button and trigger the change() event on it.
	this.UIdiv.children('div.project').first().children("input").prop("checked", true).change();
}

ProjectUI.prototype.createProjectClicked = function(element) {
	var newProjName = element.project.value;

	if (!newProjName) {
		alert("Please enter a project name.");
		element.project.focus();
		return false;
	}

	if (this.projman.projectExists(newProjName)) {
		alert("Project '" + newProjName + "' already exists. Please use a different name.");
		element.project.focus();
		return false;
	}

	this.projman.newProject(newProjName);
	this.addProjectToUI(newProjName);
}

/**
 * Adds a radio button for the specified project name to the user interface.
 **/
ProjectUI.prototype.addProjectToUI = function(projectname) {
	// Add the project name to the UI and trigger the change event.
	this.createProjectElement(projectname).appendTo(this.UIdiv).children("input").prop("checked", true).change();
}

/**
 * Creates a new project <div> (including the radio button) from the DOM template.
 **/
ProjectUI.prototype.createProjectElement = function(projectname, index) {
	var self = this;

	return this.projectTemplate.clone().children("input").val(projectname)
		.attr("id", "p" + index).change(function() { self.projectButtonSelected(this); }).end()
		.children("label").attr("for", "p" + index).html(projectname).end();
}

/**
 * This method is called whenever a project radio button is selected.  It causes all observers
 * to be notified of the selection change.
 **/
ProjectUI.prototype.projectButtonSelected = function(element) {
	var projname = element.value;
	this.selproject = this.projman.openProject(projname);

	this.notifyProjectSelectionChange(this.selproject);
}
	
ProjectUI.prototype.deleteProjectClicked = function() {
	if (!confirm("Are you sure you want to DELETE project '" + this.selproject.getName() + "'?")) 
		return;

	this.projman.deleteProject(this.selproject);

	this.UIdiv.find("div.project").children("input[value='" + this.selproject.getName() + "']").parent().remove();

	if (this.projman.getProjectCnt() == 0) {
		this.projman.newProject('new project');
		this.addProjectToUI('new project');
	}

	// Select the next project in the set of radio buttons.
	this.selectDefaultProject();
}
	
ProjectUI.prototype.deleteAllClicked = function() {
	if (confirm("Are you sure you want to DELETE ALL PROJECTS?")) {
		this.projman.deleteAll();
		location.reload();
	}
}

/**
 * Handle "Export" button clicks.  Sets the hidden elements of the export form to the values
 * appropriate for the current project so that they can be submitted to the server for
 * export to the user as a file.
 *
 * @param element The export form JQuery object.
 **/
ProjectUI.prototype.exportProjectClicked = function(element) {
	// Access the form DOM element directly, because in this case, it is simpler
	// than going through JQuery.
	var form = element.get(0);
	form.filename.value = this.selproject.getName().replace(/\s+/g, "_") + ".trp";
	form.content.value = this.projman.getProjectJSON(this.selproject);
}
	
/**
 * Handle project file import requests.  This method is triggered whenever the value of the
 * "file" form element changes.
 **/
ProjectUI.prototype.importProjectFileSelected = function() {
	// Get the File object to read.
	var fileobj = this.UIdiv.find("input.importFile").get(0).files[0];

	this.readProjectFile(fileobj);
}

/**
 * Attempt to read a project definition from the specified file.  The file must contain
 * a JSON-encoded project object.
 *
 * @param file The path of the project file to read.
 **/
ProjectUI.prototype.readProjectFile = function(file) {
	var reader = new FileReader();

	// Save a local reference to this ProjectManager object.
	var self = this;

	// Define a function for handling the file contents.
	reader.onload = function() {
		self.projman.loadProjectJSON(this.result);
	}

	// Read the file.
	reader.readAsText(file);
}


