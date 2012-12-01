
/**
 * The classes defined in this file implement "simplifiers" for various data formats supported by
 * the Triplifier.  There is no abstract base class definition for Simplifiers (this is JavaScript,
 * after all), but all Simplifiers must adhere to a simple interface.  They need to provide a
 * single method with the following signature.
 *
 * simplify(project) { ... }
 *
 * This allows all Simplifier objects to be used transparently and interchangeably by the
 * rest of the Triplifier UI code.
 **/




/**
 * A Simplifier for Darwin Core Archives.  This currently only supports single-table archives.
 **/
function DwCASimplifier() {
}

// A lookup table for matching DwC concept ID field names to DwC concepts.
DwCASimplifier.prototype.cncpttable = {
	taxonID: {name:'Taxon', uri:'http://rs.tdwg.org/dwc/terms/Taxon'},
	datasetID: {name:'Dataset', uri:'http://rs.tdwg.org/dwc/terms/Dataset'},
	eventID: {name:'Event', uri:'http://rs.tdwg.org/dwc/terms/Event'},
	geologicalContextID: {name:'GeologicalContext', uri:'http://rs.tdwg.org/dwc/terms/GeologicalContext'},
	identificationID: {name:'Identification', uri:'http://rs.tdwg.org/dwc/terms/Identification'},
	measurementID: {name:'MeasurementOrFact', uri:'http://rs.tdwg.org/dwc/terms/MeasurementOrFact'},
	occurrenceID: {name:'Occurrence', uri:'http://rs.tdwg.org/dwc/terms/Occurrence'},
	resourceRelationshipID: {name:'ResourceRelationship', uri:'http://rs.tdwg.org/dwc/terms/ResourceRelationship'}
}

/**
 * Attempts to "simplify" the data schema of a project by automatically defining as many project
 * elements as possible.  For a single-table Darwin Core Archive, this means inferring concept and
 * attribute definitions.
 *
 * @param project The project to simplify.
 *
 * @returns True if simplification was successful, false otherwise.
 **/
DwCASimplifier.prototype.simplify = function(project) {
	this.project = project;

	// Try to infer joins.  Joins are only possible if there are at least two tables.
	if (this.project.schema.length > 1) {
	}

	// Now try to figure out the DwC concepts (entities) that are used.
	// Look at each column of each table.
	// Object format for a concept project entry:
	// { table:"DarwinCore_txt", idColumn:"taxonID", idPrefixColumn:"", rdfClass:{name:"Taxon", uri:"http://rs.tdwg.org/dwc/terms/Taxon"}}
	var cnt, cnt2, table, col;
	var projentities = this.project.getProperty('entities');
	var projentlen = projentities.length;
	for (cnt = 0; cnt < this.project.schema.length; cnt++) {
		table = this.project.schema[cnt];
		for (cnt2 = 0; cnt2 < table.columns.length; cnt2++) {
			col = table.columns[cnt2];
			// See if this column is a known ID column.
			if (col in this.cncpttable) {
				// Found an ID column, so create the concept and add it to the project.
				var newcncpt = {
					table:table.name, idColumn:col,
					rdfClass:{ name:this.cncpttable[col].name, uri:this.cncpttable[col].uri }
				};
				projentities.push(newcncpt);
			}
		}
	}

	// If we defined new entities (concepts), save them to the project.
	if (projentlen != projentities.length)
		this.project.setProperty('entities', projentities);

	// Next, work on the attributes.  Note the "domain" property of each property item from VocabularyManager
	// could indicate which class each property is appropriate for, but the domain property does not seem to
	// get defined by VocabularyManager, so it is not used here.  This problem needs to be solved eventually.
	var dwcatts = vocabularyManager.getSelectedVocabulary()['properties'];
	var cnt3, entity, columns, prop;
	// The general strategy is to look at each entity (concept) in the project and see if we can define any
	// attributes for it.
	// Object format for an attribute entry:
	// { 	entity:"DarwinCore_txt.taxonID",
	// 	rdfProperty:{name:"Binomial", uri:"http://rs.tdwg.org/dwc/terms/Binomial"},
	// 	column:"scientificName" }
	var projattributes = this.project.getProperty('attributes');
	var projattlen = projattributes.length;
	for (cnt = 0; cnt < this.project.entities.length; cnt++) {
		entity = this.project.entities[cnt];
		
		// Examine each column of this entity's table and see if it can be matched to a DwC property name.
		columns = this.project.getTableByName(entity.table).columns;
		for (cnt2 = 0; cnt2 < columns.length; cnt2++) {
			col = columns[cnt2];
			// Only consider columns that are not already part of an attribute or entity.
			if (this.project.isColumnAvailable(entity.table, col)) {
				// See if there is a DwC property name that matches the column name.  If so,
				// define a new attribute for the current entity.
				for (cnt3 = 0; cnt3 < dwcatts.length; cnt3++) {
					prop = dwcatts[cnt3];
					if (prop.name == col) {
						// Found a match, so define a new attribute.
						var newattrib = {
							entity:entity.table + '.' + entity.idColumn,
							rdfProperty:{ name:prop.name, uri:prop.uri },
							column:col
						};
						projattributes.push(newattrib);
					}
				}
			}
		}
	}

	// If we defined new attributes, save them to the project.
	if (projattlen != projattributes.length)
		this.project.setProperty('attributes', projattributes);

	// If we defined at least one concept, consider the simplification successful.
	if (projentlen != projentities.length)
		return true;
	else
		return false;
}

