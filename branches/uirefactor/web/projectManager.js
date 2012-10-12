function Project(name) {
	this.name = name;
	this.dateTime = "";
	this.connection = {};
	this.schema = [];
	this.joins = [];
	this.entities = [];
	this.relations = [];

	// An array for keeping track of observers of this project.
	this.observers = [];
}

// Project property names.
Project.PROPNAMES = ['name', 'dateTime', 'connection', 'schema', 'joins', 'entities', 'relations'];

// Notification events.
/*Project.prototype.NAME_CHANGED = 0;
Project.prototype.DATETIME_CHANGED = 0;
Project.prototype.CONN_CHANGED = 0;
Project.prototype.SCHEMA_CHANGED = 0;
Project.prototype.JOINS_CHANGED = 0;
Project.prototype.ENT_CHANGED = 0;
Project.prototype.REL_CHANGED = 0;*/

Project.prototype.registerObserver = function(observer) {
	this.observers.push(observer);
}

Project.prototype.notifyPropertyChange = function(propname) {
	for (var cnt = 0; cnt < this.observers.length; cnt++) {
		this.observers[cnt].projectPropertyChanged(this, propname);
	}
}

Project.prototype.getProperty = function(propname) {
	return this[propname];
}

Project.prototype.setProperty = function(propname, newval) {
	this[propname] = newval;
	this.notifyPropertyChange(propname);
}

Project.prototype.setName = function(newname) {
	this.name = newname;
	this.notifyObservers(Project.NAME_CHANGED);
}

Project.prototype.getName = function() {
	return this.name;
}

/**
 * Get the total number of columns in this project's source data schema.
 **/
Project.prototype.getColumnCount = function() {
	var totalcols = 0;

	$.each(this.schema, function(i, table) {
		totalcols += table.columns.length;
	});

	return totalcols;
}

/**
 * Find a specific table in this project's source data schema.
 **/
Project.prototype.findTable = function(tablename) {
	table = this.schema[indexOf(this.schema, "name", tablename)];
	
	return table;	
}

/*Project.prototype.setDateTime = function(newdatetime) {
	this.datetime = newdatetime;
	this.notifyObservers(Project.DATETIME_CHANGED);
}

Project.prototype.getDateTime = function() {
	return this.datetime;
}

Project.prototype.setConnection = function(newconn) {
	this.connection = $.extend(true, {}, newconn);
	this.notifyObservers(Project.CONN_CHANGED);
}

Project.prototype.getConnection = function() {
	return $.extend(true, {}, this.connection);
}

Project.prototype.setSchema = function(newschema) {
	this.schema = newschema;
	this.notifyObservers(Project.SCHEMA_CHANGED);
}

Project.prototype.getSchema = function() {
	return this.schema;
}

Project.prototype.setJoins = function(newjoins) {
	this.joins = newjoins;
	this.notifyObservers(Project.JOINS_CHANGED);
}

Project.prototype.getJoins = function() {
	return this.joins;
}

Project.prototype.setEntities = function(newents) {
	this.entities = newents;
	this.notifyObservers(Project.ENT_CHANGED);
}

Project.prototype.getEntities = function() {
	return this.entities;
}

Project.prototype.setRelations = function(newrels) {
	this.relations = newrels;
	this.notifyObservers(Project.REL_CHANGED);
}

Project.prototype.getRelations = function() {
	return this.relations;
}*/



/**
 * ProjectManager takes care of saving, retrieving, and deleting projects in local
 * storage, as well as creating new projects.  It also implements exporting and
 * importing projects to and from files.
 */
function ProjectManager() {
	this.projectskey = "triplifier.projects";
	this.projects = localStorage.getObject(this.projectskey) || [];
}

/**
 * Handle update notifications from projects that we're following.
 **/
ProjectManager.prototype.projectPropertyChanged = function(project, propname) {
	//alert('property: ' + propname + '\nchanged to: ' + project.getProperty(propname));
	localStorage.setObject(this.getStorageKey(propname, project.getName()), project.getProperty(propname));
}

/**
 * Get the total number of projects in this ProjectManager.
 **/
ProjectManager.prototype.getProjectCnt = function() {
	return this.projects.length;
}

/**
 * Return an array of all project names.
 **/
ProjectManager.prototype.getProjectNames = function() {
	return this.projects.slice()
}

/**
 * See if a project with a given name already exists in local storage.
 **/
ProjectManager.prototype.projectExists = function(projectname) {
	return $.inArray(projectname, this.projects) >= 0
}

/**
 * Create a new project with the given name.  The project will only be created if
 * there is no existing project with the same name.
 **/
ProjectManager.prototype.newProject = function(projectname) {
	if (!this.projectExists(projectname)) {
		var project = new Project(projectname);
		project.registerObserver(this);

		this.addProject(project);
		return project;
	} else {
		return false;
	}
}

/**
 * Adds a project to the projects list and to local storage.
 **/
ProjectManager.prototype.addProject = function(project) {
	// add the project name to the projects list
	this.projects.push(project.getName());

	// update the project names in local storage
	localStorage.setObject(this.projectskey, this.projects);

	// save the project properties to local storage
	for (var cnt = 0; cnt < Project.PROPNAMES.length; cnt++) {
		var propname = Project.PROPNAMES[cnt];
		localStorage.setObject(this.getStorageKey(propname, project.getName()), project.getProperty(propname));
	}
}

/**
 * Create a new, empty project.  Note that even the "name" property is left empty.
 **/
ProjectManager.prototype.createEmptyProject = function() {
	return {
		name:"",
		dateTime:"",
		connection:{},
		schema:[],
		joins:[],
		entities:[],
		relations:[]
	};
}

/**
 * Open a project that was saved in local storage.
 **/
ProjectManager.prototype.openProject = function(projectname) {
	var project = new Project(projectname);

	/*$.each(project, function(key, value) { 
		project[key] = localStorage.getObject(getStorageKeyFn(key, prj));
	});*/
	// Load the project properties
	for (var cnt = 0; cnt < Project.PROPNAMES.length; cnt++) {
		var propname = Project.PROPNAMES[cnt];
		project.setProperty(propname, localStorage.getObject(this.getStorageKey(propname, projectname)));
	}

	project.registerObserver(this);

	return project;
}

/**
 * Delete a project from both the ProjectManager and local storage.
 **/
ProjectManager.prototype.deleteProject = function(project) {
	// get the index of the project in the projects array
	var index = $.inArray(project.getName(), this.projects);

	// remove the project from the projects array
	if (index >= 0) {
		this.projects.splice(index, 1);
		localStorage.setObject(this.projectskey, this.projects);
	}

	// remove the project items from local storage
	/*$.each(project, function(key, value) { 
		localStorage.removeItem(this.getStorageKey(key, project['name']));
	});*/
	for (var cnt = 0; cnt < Project.PROPNAMES.length; cnt++) {
		var propname = Project.PROPNAMES[cnt];
		localStorage.removeItem(this.getStorageKey(propname, project.getName()));
	}
}

/**
 * Delete all projects from local storage.
 **/
ProjectManager.prototype.deleteAll = function() {
	localStorage.clear();
}
	
/**
 * Attempt to read a project definition from the specified file.  The file must contain
 * a JSON-encoded project object.
 **/
ProjectManager.prototype.importProject = function(file) {
    var reader = new FileReader();

    // Save a local reference to this ProjectManager object.
    var self = this;

    // Define a function for handling the file contents.
    reader.onload = function() {
	    self.loadProjectJSON(this.result);
	}

    // Read the file.
    reader.readAsText(file);
}

/**
 * Attempt to load a project from a JSON-formatted string representation of a project object.
 **/
ProjectManager.prototype.loadProjectJSON = function(jsonstring) {
	try {
		var newProject = JSON.parse(jsonstring);
		var originalName = newProject['name'];
		var i = 1;

		// Generate a unique name for the project if a project with the same name
		// already exists.
		while (this.projectExists(newProject['name']))
			newProject['name'] = originalName + "." + i++;

		// Save the contents of the project to local storage.
		$.each(newProject, function(key, value) { 
			localStorage.setObject(this.getStorageKey(key, newProject['name']), newProject[key]);
		});

		this.addProject(newProject['name']);
	}
	catch(err) {
		alert("Error reading file.");
	}
}

ProjectManager.prototype.getStorageKey = function(key, prj) {
	return "triplifier." + key + (prj ? "." + prj : "");
}

