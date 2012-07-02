/**
 * Provides functionality related to projects, such as:
 * open, create, export, import, delete, delete all. 
 * When instantiated, binds functions to relevant forms 
 * and inputs, populates projects stored in localStorage,
 * loads first project (or creates default project).
 * 
 * @param element {jQuery} DOM element containing all 
 * 		project controls. Example html:
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
 * @param projectsStorage {String} localStorage key whose value is 
 * 		an array of names of all projects stored in localStorage.
 * @param project {Object} Object with all project parts that 
 * 		are stored as separate localStorage entries.
 * @param projectKey {String} Key in 'project' whose value is project name.
 * @param getStorageKeyFn {Function(part, project)} Function that 
 * 		returns {String} localStorage key whose value is project 
 * 		part for given {String} part and {String} project.
 * @param onOpenProjectFn {Function()} Function {void} called 
 * 		when a project is opened.
 */
function ProjectManager(element, projectsStorage, project, projectKey, getStorageKeyFn, onOpenProjectFn) {

	var lastProject = 0, // used to assign ids to project DOM elements (needed for labels to work)
		projectTemplate = element.find("div.project").remove(); // used to create project DOM elements

	// assign event handlers
	element.find("form.new").submit(newProject);	
	element.find("form.export").submit(exportProject);	
	element.find("input.importFile").change(importProject);	
	element.find("input.import").click(function() {element.find("input.importFile").val("").click();});	
	element.find("input.delete").click(deleteProject);	
	element.find("input.deleteAll").click(deleteAll);

	// populate projects section, load first (or default) project
	var projects = [];
	$.each(localStorage.getObject(projectsStorage) || [], function(i, prj) { 
		projects.push(projectElement(prj).get(0));
	});
	if (projects.length) 
		$(projects).appendTo(element).first()
			.children("input").prop("checked", true).change(); // load first project
	else
		newDefaultProject(); // load default project

	function newProject() {
		var newProject = this.project.value;
		if (!newProject) {
			alert("Please enter a project name.");
			this.project.focus();
			return false;
		}
		var projects = localStorage.getObject(projectsStorage) || [];
		if ($.inArray(newProject, projects) >= 0) {
			alert("Project '" + newProject + "' already exists. Please use a different name.");
			this.project.focus();
			return false;
		}
		addProject(newProject, projects);
		return false;
	}
	
	function addProject(newProject, projects) {
		projects.push(newProject);
		localStorage.setObject(projectsStorage, projects);
		projectElement(newProject).appendTo(element)
			.children("input").prop("checked", true).change();
	}
	
	function projectElement(prj) {
		lastProject++;
		return projectTemplate.clone().children("input").val(prj)
			.attr("id", "p" + lastProject).change(openProject).end()
			.children("label").attr("for", "p" + lastProject).html(prj).end();
	}
	
	function openProject() {
		var prj = this.value;
		$.each(project, function(key, value) { 
			project[key] = localStorage.getObject(getStorageKeyFn(key, prj));
		});
		project[projectKey] = prj; // this is just in case someone has old projects that don't have 'project' property in localStorage
		onOpenProjectFn();
	}
	
	function deleteProject() {
		if (!confirm("Are you sure you want to DELETE project '" + project[projectKey] + "'?")) 
			return;
		var projects = localStorage.getObject(projectsStorage) || [],
			idx = $.inArray(project[projectKey], projects);
		if (idx >= 0) {
			projects.splice(idx, 1);
			localStorage.setObject(projectsStorage, projects);
		}
		$.each(project, function(key, value) { 
			localStorage.removeItem(getStorageKeyFn(key, project[projectKey]));
		});
		element.find("div.project").children("input[value='" + project[projectKey] + "']").parent().remove()
			.end().end().first().children("input").prop("checked", true).change();
		if (!projects.length) 
			newDefaultProject();
	}
	
	function deleteAll() {
		if (confirm("Are you sure you want to DELETE ALL PROJECTS?")) {
			localStorage.clear();
			location.reload();
		}
	}
	
	function newDefaultProject() {
		element.find("form.new input[name='project']").val("Default Project")
			.parent("form").submit().end().val("");
	}
	
	function exportProject() {
		this.filename.value = project[projectKey].replace(/\s+/g, "_") + ".trp";
		this.content.value = JSON.stringify(project); 
	}
	
	function importProject() {
	    var reader = new FileReader();
	    reader.onload = readProject;
	    reader.readAsText(this.files[0]);
	}
	
	function readProject() {
		try {
			var newProject = JSON.parse(this.result),
				projects = localStorage.getObject(projectsStorage) || [],
				originalName = newProject[projectKey],
				i = 1;
			while ($.inArray(newProject[projectKey], projects) >= 0)
				newProject[projectKey] = originalName + "." + i++;
			$.each(project, function(key, value) { 
				localStorage.setObject(getStorageKeyFn(key, newProject[projectKey]), newProject[key]);
			});
			addProject(newProject[projectKey], projects);
		}
		catch(err) {
			alert("Error reading file.");
		}	
	}
	
}