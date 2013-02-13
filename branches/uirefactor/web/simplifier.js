
/**
 * The classes defined in this file implement "simplifiers" for various data formats supported by
 * the Triplifier.  There is no abstract base class definition for Simplifiers (this is JavaScript,
 * after all), but all Simplifiers must adhere to a simple interface.  They need to provide a
 * single method with the following signature.
 *
 *   simplify(project) { ... }
 *
 * This allows all Simplifier objects to be used transparently and interchangeably by the
 * rest of the Triplifier UI code.
 *
 * They must also provide two static properties, formatdescription and formatcode, that are used by
 * the SimplifierFactory and for UI construction.
 *
 *   formatdescription: A text description of the format.
 *   formatcode: A simple text code that can be used as a key for the format.
 **/




/**
 * A class for creating new Simplifier objects to match given input file formats.  This means clients
 * are not required to have any knowledge of specific concrete Simplifier classes.
 **/
function SimplifierFactory() {
	// A dictionary of all known Simplifiers.
	this.simplifiers = {};

	// A dictionary mapping format codes to text descriptions.
	this.formatsmap = {};

	// Load all defined Simplifiers.
	this.addSimplifier(DwCASimplifier);
}

/**
 * Adds a Simplifier class to the factory.
 *
 * @param simplifier The class (constructor function) of a concrete Simplifier implementation.
 **/
SimplifierFactory.prototype.addSimplifier = function(simplifier) {
	var formatcode = simplifier.formatcode;

	this.simplifiers[formatcode] = simplifier;
	this.formatsmap[formatcode] = simplifier.formatdescription;
}

/**
 * Gets a dictionary mapping format codes to format descriptions for all Simplifiers.
 **/
SimplifierFactory.prototype.getFormatsMap = function() {
	return this.formatsmap;
}

/**
 * Gets an instance of a Simplifier object for the specified input format.
 *
 * @param formatcode The input format for which to get a new Simplifier.
 **/
SimplifierFactory.prototype.getSimplifier = function(formatcode) {
	var simplifier;

	// See if the format code corresponds with a known Simplifier, and if so,
	// construct a new instance of the Simplifier.	
	if (formatcode in this.simplifiers)
		simplifier = new this.simplifiers[formatcode]();

	return simplifier;
}




/**
 * A Simplifier for Darwin Core Archives.  This currently only supports single-table archives.
 **/
function DwCASimplifier() {
}

// Static properties of the Simplifier.
DwCASimplifier.formatdescription = 'Darwin Core Archive';
DwCASimplifier.formatcode = 'DwCA';

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
	// {	table:"occurrence_txt", idColumn:"taxonID", idPrefixColumn:"",
	// 	rdfClass:{name:"Taxon", uri:"http://rs.tdwg.org/dwc/terms/Taxon"} }
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
					table:table.name, idColumn:col, idPrefixColumn:table.name,
					rdfClass:{ name:this.cncpttable[col].name, uri:this.cncpttable[col].uri }
				};
				projentities.push(newcncpt);
			}
		}
	}

	// If we defined new entities (concepts), save them to the project.
	if (projentlen != projentities.length)
		this.project.setProperty('entities', projentities);

	// Next, work on the attributes.  The "domain" array of each property item from VocabularyManager indicates
	// which class each property is appropriate for, and is used here to match properties to their classes.
	//
	// The domain definition actually comes from the dwcattributes:organizedInClass property of the DwC RDF
	// specification.  It should be noted that some DwC terms, such as basisOfRecord, have "all" as the value
	// of organizedInClass.  This is the case for all "record-level" terms.
	// Right now, we are mapping some record-level terms to the Occurrence class and dropping the others,
	// because we do not know how to best handle them.  The record-level terms that map to Occurrence are 
	// dcterms:type, institutionID, collectionID, institutionCode, collectionCode, ownerInstitutionCode, and
	// basisOfRecord.
	var dwcatts = vocabularyManager.getSelectedVocabulary()['properties'];
	// Define the record-level terms that should be mapped to the Occurrence class.
	var record_level_occurrence = ['dcterms:type', 'institutionID', 'collectionID', 'institutionCode',
	    'collectionCode', 'ownerInstitutionCode', 'basisOfRecord'];
	// basisOfRecord
	var cnt3, entity, columns, prop;
	//console.log(vocabularyManager.getSelectedVocabulary());
	
	// The general strategy is to look at each entity (concept) in the project and see if we can define any
	// attributes for it.
	// Object format for an attribute entry:
	// { 	entity:"occurrence_txt.taxonID",
	// 	rdfProperty:{name:"Binomial", uri:"http://rs.tdwg.org/dwc/terms/Binomial"},
	// 	column:"scientificName" }
	var projattributes = this.project.getProperty('attributes');
	var projattlen = projattributes.length;
	for (cnt = 0; cnt < this.project.entities.length; cnt++) {
		entity = this.project.entities[cnt];
		//alert(entity.rdfClass.uri);
		
		// Examine each column of this entity's table and see if it can be matched to a DwC property name.
		columns = this.project.getTableByName(entity.table).columns;
		for (cnt2 = 0; cnt2 < columns.length; cnt2++) {
			col = columns[cnt2];
			// Only consider columns that are not already part of an attribute or entity.
			if (this.project.isColumnAvailable(entity.table, col)) {
				// Look through all of the DwC property names to see if there is a property name
				// that matches the column name.
				for (cnt3 = 0; cnt3 < dwcatts.length; cnt3++) {
					prop = dwcatts[cnt3];
					if (prop.name == col) {
						// Found a match, so now see if the domain of this property includes
						// the class of the current DwC entity (concept).
						if (
							// Check if either this property matches the current class...
							prop.domain.indexOf(entity.rdfClass.uri) != -1 ||
							// or if it is a record-level term that goes with Occurrence.
							(entity.rdfClass.uri == 'http://rs.tdwg.org/dwc/terms/Occurrence'
							 && record_level_occurrence.indexOf(prop.name) != -1)
						   ) {
							// We found a match, so define a new attribute for the current entity.
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
	}

	// If we defined new attributes, save them to the project.
	if (projattlen != projattributes.length)
		this.project.setProperty('attributes', projattributes);


	// Define relationships between classes, if possible.
	// Object format for a concept project entry:
	// {	table:"occurrence_txt", idColumn:"taxonID", idPrefixColumn:"",
	// 	rdfClass:{name:"Taxon", uri:"http://rs.tdwg.org/dwc/terms/Taxon"} }
	// Object format for a relations entry:
	// {
	// 	subject: "occurrence_txt.occurrenceID",
	// 	predicate: "bsc:related_to",
	// 	object: "occurrence_txt.taxonID"
	// }
	
	// A tree-like data structure that specifies all possible relations.  The top-level
	// indices are the subjects, which each point to a list of possible objects and predicates.
	var rels_list = {
		'Identification': {
			'Taxon': 'bsc:depends_on',
			'Occurrence': 'bsc:depends_on'
		},
		'Event': {
			'dcterms:Location': 'bsc:related_to',
			'GeologicalContext': 'bsc:related_to'
		},
		'Occurrence': {
			'Event': 'bsc:depends_on',
			'GeologicalContext': 'bsc:depends_on',
			'dcterms:Location': 'bsc:depends_on',
			'Taxon': 'bsc:related_to'
		},
	};
	// Keeps track of which concepts we've already mapped to a relation so we don't
	// try to map anything twice.
	var relation_objects = [];

	var cnt, cnt2, subj_concept, subj_class, obj_concept, obj_class;
	var projentities = this.project.getProperty('entities');
	var projrelations = this.project.getProperty('relations');
	var projrellen = projrelations.length;

	// Examine each concept and try to map it to a relation with another concept.  We have
	// to start with Identification and Event to ensure that if they are present, they are
	// mapped properly.  If they are not present, then we can simply look at each concept
	// in turn and try to map it.
	for (cnt = 0; cnt < projentities.length; cnt++) {
		// See if the current concept can be the subject for any relations.
		if (projentities[cnt].rdfClass.name in rels_list) {
			subj_concept = projentities[cnt];
			subj_class = subj_concept.rdfClass.name;
			// It can, so we need to look at all of the other concepts to try to find
			// an object for the relation (or relations).
			for (cnt2 = 0; cnt2 < projentities.length; cnt2++) {
				// Check if this concept can be the object of a relation with the current
				// subject and if it is not already an object in a relation.
				if (
					projentities[cnt2].rdfClass.name in rels_list[subj_class] &&
			       		projentities.indexOf(projentities[cnt2].rdfClass.name) == -1
				) {
					// We found a match, so define the new relation.
					obj_concept = projentities[cnt2];
					obj_class = obj_concept.rdfClass.name;
					//alert (subj_class + ": " + obj_class);
					var newrel = {
						subject: subj_concept.table + '.' + subj_concept.idColumn,
						predicate: rels_list[subj_class][obj_class],
						object: obj_concept.table + '.' + obj_concept.idColumn
					}
					projrelations.push(newrel);
					//console.log(newrel);

					// Add the object to the list of "used" object classes.
					relation_objects.push(obj_class);
				}
			}
		}
	}

	// If we defined new relations, save them to the project.
	if (projrellen != projrelations.length)
		this.project.setProperty('relations', projrelations);


	// If we defined at least one concept, consider the simplification successful.
	if (projentlen != projentities.length)
		return true;
	else
		return false;
}

