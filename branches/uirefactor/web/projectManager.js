
/**
 * Project defines the project object and the methods for manipulating and querying projects.
 **/
function Project(name) {
	this.name = name;
	this.dateTime = "";
	this.connection = {};
	this.schema = [];
	this.joins = [];
	this.entities = [];
	this.attributes = [];
	this.relations = [];

	// An array for keeping track of observers of this project.
	this.observers = [];

	// Keeps track of all possible relations for this project.
	this.allrels = { count: 0, relations: [] };
}

// Project property names.
Project.PROPNAMES = ['name', 'dateTime', 'connection', 'schema', 'joins', 'entities', 'attributes', 'relations'];

// Notification events.
/*Project.prototype.NAME_CHANGED = 0;
Project.prototype.DATETIME_CHANGED = 0;
Project.prototype.CONN_CHANGED = 0;
Project.prototype.SCHEMA_CHANGED = 0;
Project.prototype.JOINS_CHANGED = 0;
Project.prototype.ENT_CHANGED = 0;
Project.prototype.REL_CHANGED = 0;*/

/**
 * Register an observer of this project.  Observers are notified whenever a project property
 * changes.  To be a project observer, an object must provide the following method:
 *
 * projectPropertyChanged(project, property_name) { ... }.
 *
 * The argument "project" references the Project object that triggered the event, and 
 * "property_name" is a string indicating which property changed.
 **/
Project.prototype.registerObserver = function(observer) {
	this.observers.push(observer);
}

/**
 * Remove an object from this Project's list of observers.
 **/
Project.prototype.unregisterObserver = function(observer) {
	for (var cnt = this.observers.length - 1; cnt >= 0; cnt--) {
		if (this.observers[cnt] === observer) {
			// Remove the observer from the list.
			this.observers.splice(cnt, 1);
		}
	}
}

Project.prototype.notifyPropertyChange = function(propname) {
	for (var cnt = 0; cnt < this.observers.length; cnt++) {
		this.observers[cnt].projectPropertyChanged(this, propname);
	}
}

Project.prototype.getProperty = function(propname) {
	return this[propname];
}

/**
 * Update the value of one of this project's properties.  It is important to use this method
 * to update the project data rather than directly setting the project's member object values.
 * Using this method ensures that the project's internal state is consistent and allows the
 * project to notify any observers of the change.
 *
 * Often, the object that requests a property change does not need to be subsequently notified
 * of the change.  Any object specified in the optional third argument will not be notified
 * of this event.
 *
 * @param propname  The property to update.
 * @param newval  The new value of the property.
 * @param dontnotify  An object not to notify of this change.
 **/
Project.prototype.setProperty = function(propname, newval, dontnotify) {
	this[propname] = newval;
	dontnotify = dontnotify || false;

	// update all possible relations
	this.allrels = this.findAllPossibleRelations();

	if (dontnotify) {
		this.unregisterObserver(dontnotify);
		this.notifyPropertyChange(propname);
		this.registerObserver(dontnotify);
	} else {
		this.notifyPropertyChange(propname);
	}

	// After a project property is changed, we need to make sure that the internal state of
	// the project remains consistent.  For example, if concepts (entities) are modified, we
	// need to make sure that all of the defined attributes and relations are still valid.
	// Each check can trigger a recursive call to setProperty() so that observers can be
	// notified of the new changes and any further consistency checks can be performed.
	if (propname == 'joins') {
		// Remove any relations that are no longer valid (i.e., a join they
		// used is no longer valid).
		for (var i = this.relations.length - 1; i >= 0; i--) {
			if (!this.isRelationValid(this.relations[i])) {
				//alert('Invalid relation found.');
				// Delete the invalid relation.
				this.relations.splice(i, 1);
				// Update the project.
				this.setProperty('relations', this.relations);
			}
		}
	} else if (propname == 'entities') {
		// Remove any attributes that are no longer valid.
		for (var i = this.attributes.length - 1; i >= 0; i--) {
			if (!this.isAttributeValid(this.attributes[i])) {
				//alert('Invalid attribute found.');
				// Delete the invalid attribute.
				this.attributes.splice(i, 1);
				// Update the project.
				this.setProperty('attributes', this.attributes);
			}
		}

		// Remove any relations that are no longer valid (i.e., an entity (concept) they
		// use is no longer valid).
		for (var i = this.relations.length - 1; i >= 0; i--) {
			if (!this.isRelationValid(this.relations[i])) {
				//alert('Invalid relation found.');
				// Delete the invalid relation.
				this.relations.splice(i, 1);
				// Update the project.
				this.setProperty('relations', this.relations);
			}
		}
	}
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
 * Searches for a specified table name in the project's schema.  If only the table name
 * is specified, then either the matching table object or "undefined" is returned.  If
 * a column name is also provided, then a table object is only returned if it has the
 * matching table name and contains a matching column name.  Otherwise, "undefined" is
 * returned.
 *
 * @param project The project to search.
 * @param table The table name to search for.
 * @param column The column name to search for.
 **/
Project.prototype.getTableByName = function(table, column) {
	// get the table object from the project's schema
	table = this.schema[indexOf(this.schema, "name", table)];

	// see if the table contains the specified column
	if (table && column && $.inArray(column, table.columns) < 0)
		table = undefined;
	return table;
}

/**
 * Checks whether a specified column in a table is already used by either an entity
 * (concept) or an attribute.  Returns true if the column is available (unused);
 * false otherwise.
 *
 * @param table The table name.
 * @param column The column name to search for.
 **/
Project.prototype.isColumnAvailable = function(table, column) {
	var attribs, j;
	// Check each entity.
	for (var i = 0; i < this.entities.length; i++) {
		if (this.entities[i].table == table) {
			// First, check this entity's column.
		       	if (this.entities[i].idColumn == column)
				return false;
			else {
				// Next, check the attributes.
				attribs = this.getAttributesByEntity(this.entities[i].table + '.' + this.entities[i].idColumn);
				for (j = 0; j < attribs.length; j++) {
					if (attribs[j].column == column)
						return false;
				}
			}
		}
	}

	return true;
}

/**
 * Get all of the attributes for a specific entity.
 **/
Project.prototype.getAttributesByEntity = function(entityname) {
	var attribs = [];

	$.each(this.attributes, function(i, attribute) {
		if (attribute.entity == entityname)
			attribs.push(attribute);
	});

	return attribs;
}

/**
 * Determine whether a given attribute is still valid.  If the entity it references still
 * exist in the project, true is returned.  False otherwise.
 **/
Project.prototype.isAttributeValid = function(attribute) {
	var entname;

	for (var i = 0; i < this.entities.length; i++) {
		entname = this.entities[i].table + '.' + this.entities[i].idColumn;
		if (attribute.entity == entname)
			// The entity in the attribute is valid, so return true.
			return true;
	}

	// No matching entity could be found, so return false.
	return false;
}

/**
 * Get the number of entities associated with a specific database table.
 **/
Project.prototype.getEntityCntByTable = function(tablename) {
	var count = 0;

	$.each(this.entities, function(i, entity) {
		if (entity['table'] == tablename)
			count++;
	});

	return count;	
}

/**
 * Returns an object that combines the entities and attributes by assigning a list of
 * attributes to each entity.  This format is required by the java REST methods for
 * producing triples and D2RQ mappings.  This method emulates the format of the old
 * project object when entities and attributes were combined in the user interface.
 **/
Project.prototype.getCombinedEntitiesAndAttributes = function() {
	// Make a deep copy of the project's entities so we don't modify the entities
	// in the project.
	var entcopy = $.extend(true, {}, this.entities);

	// An array for the combined entities and attributes.  This is necessary because
	// the result of the extend() operation produces an object, not an array, which
	// will not work with the REST methods.
	var combined = [];

	var self = this;
	$.each(entcopy, function(i, entity) {
		var entityname = entity.table + '.' + entity.idColumn;
		entity.attributes = [];

		// Find all of the attributes for the current entity.
		$.each(self.attributes, function(i, attribute) {
			if (attribute.entity == entityname) {
				entity.attributes.push({ column: attribute.column, rdfProperty: attribute.rdfProperty });
				//alert(attribute.rdfProperty.name);
			}
		});

		combined.push(entity);
	});

	return combined;
}

/**
 * Get an object that specifies all possible relations that could be defined for this
 * project.  The returned object has two properties.  The first, "count", indicates how
 * many total relations are possible (each pair is only counted once, even though it
 * could also be specified with the subject/object swapped).  The second, "relations",
 * is a list, where each element of the list is an object with two properties: "subject",
 * which specifies the subject of the relation, and "objects", which is a list of all
 * possible objects that could be related to that subject.  A relation is only possible if
 * the subject and object are in the same database table or in separate tables that are
 * part of a join.  The structure of the returned object is as follows.
 *
 * {
 * 	count: relations_cnt,
 * 	relations: [ { subject: entity_name, objects: [entity_name, ...] }, ...]
 * }
 **/
Project.prototype.getAllPossibleRelations = function() {
	return this.allrels;
}

/**
 * Calculates all possible relations for this project.  This is meant to be a private
 * method used by the project to keep its list of all relations updated.  Use the public
 * method getAllPossibleRelations() to see the results of this method.
 **/
Project.prototype.findAllPossibleRelations = function() {
	var allRelations = [];
	var allRelationsTotal = 0;
	var self = this;

	$.each(this.entities, function(i, subMp) {
		var objects = [];
		$.each(self.entities, function(j, objMp) {
			// see if these two entities are in the same table or in joined tables
			if (i != j && (subMp.table == objMp.table 
				|| indexOf(self.joins, "foreignTable", subMp.table, "primaryTable", objMp.table) >= 0 
				|| indexOf(self.joins, "foreignTable", objMp.table, "primaryTable", subMp.table) >= 0)) {
				objects.push(objMp.table + "." + objMp.idColumn);
				allRelationsTotal += .5; // each relation has inverse relation, but we'll allow only one per pair
			}
		});

		if (objects.length)
			allRelations.push({subject:subMp.table + "." + subMp.idColumn, objects:objects});

	});

	var retval = {};
	retval.relations = allRelations;
	retval.count = allRelationsTotal;
	//alert(retval.count);
	//alert(allRelations[0].subject + ' -> ' + allRelations[0].objects[0]);

	return retval;
}

/**
 * Searches for a relation that has the specified entities.  If a match is found, the
 * relation object is returned.  Otherwise, "undefined" is returned.
 **/
Project.prototype.getRelationByEntities = function(entity1, entity2) {
	var rel = undefined;

	$.each(this.relations, function(i, relation) {
		if (relation.subject == entity1 && relation.object == entity2 || relation.subject == entity2 && relation.object == entity1) {
			rel = relation;
			return false;
		}
	});

	return rel;
}

/**
 * Get the total number of relations in this project that include the specified entity.
 **/
Project.prototype.getRelationCountByEntity = function(entity) {
	var count = 0;
	$.each(this.relations, function(i, relation) {
		if (relation.subject == entity || relation.object == entity)
			count++;
	});

	return count;
}

/**
 * Determine whether a given relation still valid.  If both of the entities in the relation
 * exist in the project, true is returned.  False otherwise.
 **/
Project.prototype.isRelationValid = function(relation) {
	rellist = this.getAllPossibleRelations().relations;

	for (var i = 0; i < rellist.length; i++) {
		for (var j = 0; j < rellist[i].objects.length; j++) {
			//alert(rellist[i].subject + ' ---> predicate ---> ' + rellist[i].objects[j]);
			// If we find a matching relation in the set of all possible relations, return true.
			if (rellist[i].subject == relation.subject && rellist[i].objects[j] == relation.object)
				return true;
		}
	}

	// No match found, so return false.
	return false;
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
		attributes:[],
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

