/**
 * Implements all UI functionality for managing projects.  Essentially, this provides a user
 * interface for a ProjectManager.  It requires properly-defined HTML UI elements to function
 * properly, as explained below.
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
	this.lastProject = 0;	// used to assign ids to project DOM elements (needed for labels to work)
	this.projectTemplate = UIdiv.find("div.project").remove(); // used to create project DOM elements

	// Save a local reference to this ProjectUI object.
	var self = this;

	// assign event handlers
	UIdiv.find("form.new").submit(function() { self.createProjectClicked(this); return false; });	
	UIdiv.find("form.export").submit(function() { self.exportProjectClicked(this); return false; });
	UIdiv.find("input.importFile").change(function() { self.importProjectClicked(this); });	
	UIdiv.find("input.import").click(function() {UIdiv.find("input.importFile").val("").click();});	
	UIdiv.find("input.delete").click(function() { self.deleteProjectClicked(); });
	UIdiv.find("input.deleteAll").click(function() { self.deleteAllClicked(); });

	var projHTML = []
	var projnames = this.projman.getProjectNames();

	// Generate radio-button HTML for each project name.
	$.each(projnames, function(i, projname) { 
		projHTML.push(self.createProjectElement(projname, self.lastProject++).get(0));
	});

	if (this.projman.getProjectCnt() > 0)
		// Add the radio-button HTML to the project UI and trigger the change() event on the first
		// project button in order to load it.
		$(projHTML).appendTo(UIdiv).first().children("input").prop("checked", true).change();
	else {
		// No projects exist yet, so display a new empty project.
		setMainProject(this.projman.newProject('new project'));
		this.addProjectToUI('new project');
	}
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
	
ProjectUI.prototype.addProjectToUI = function(projectname) {
	// Add the project name to the UI and trigger the change event.
	this.createProjectElement(projectname).appendTo(this.UIdiv).children("input").prop("checked", true).change();
}
	
ProjectUI.prototype.createProjectElement = function(projectname, index) {
	var self = this;

	return this.projectTemplate.clone().children("input").val(projectname)
		.attr("id", "p" + index).change(function() { self.projectButtonSelected(this); }).end()
		.children("label").attr("for", "p" + index).html(projectname).end();
}
	
ProjectUI.prototype.projectButtonSelected = function(element) {
	var projname = element.value;
	setMainProject(this.projman.openProject(projname));

	/*$.each(project, function(key, value) { 
		project[key] = localStorage.getObject(getStorageKeyFn(key, prj));
	});

	onOpenProjectFn();*/
}
	
ProjectUI.prototype.deleteProjectClicked = function() {
	if (!confirm("Are you sure you want to DELETE project '" + mainproject.getName() + "'?")) 
		return;

	this.projman.deleteProject(mainproject);

	this.UIdiv.find("div.project").children("input[value='" + mainproject.getName() + "']").parent().remove()
		.end().end().first().children("input").prop("checked", true).change();

	if (this.projman.getProjectCnt() == 0) {
		setMainProject(this.projman.newProject('New Project'));
		this.addProjectToUI('New Project');
	}
}
	
ProjectUI.prototype.deleteAllClicked = function() {
	if (confirm("Are you sure you want to DELETE ALL PROJECTS?")) {
		this.projman.deleteAll();
		location.reload();
	}
}
	
ProjectUI.prototype.exportProjectClicked = function(element) {
	//element.filename.value = project[projectKey].replace(/\s+/g, "_") + ".trp";
	//element.content.value = JSON.stringify(project); 
}
	
ProjectUI.prototype.importProjectClicked = function() {
}

