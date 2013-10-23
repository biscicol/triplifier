
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
 * A Simplifier for Darwin Core Archives.  This simplifier expects to receive data
 * for a "re-normalized" DwC-A after processing by the Triplifier's server-side
 * reader system.
 **/
function DwCASimplifier() {
}

// Static properties of the Simplifier.
DwCASimplifier.formatdescription = 'Darwin Core Archive';
DwCASimplifier.formatcode = 'DwCA';

// A lookup table for matching DwC concept ID field names to DwC concepts.
DwCASimplifier.prototype.cncpttable = {
	taxonID: {name:'dwc:Taxon', uri:'http://rs.tdwg.org/dwc/terms/Taxon'},
	datasetID: {name:'dwc:Dataset', uri:'http://rs.tdwg.org/dwc/terms/Dataset'},
	locationID: {name:'dcterms:Location', uri:'http://purl.org/dc/terms/Location'},
	eventID: {name:'dwc:Event', uri:'http://rs.tdwg.org/dwc/terms/Event'},
	geologicalContextID: {name:'dwc:GeologicalContext', uri:'http://rs.tdwg.org/dwc/terms/GeologicalContext'},
	identificationID: {name:'dwc:Identification', uri:'http://rs.tdwg.org/dwc/terms/Identification'},
	measurementID: {name:'dwc:MeasurementOrFact', uri:'http://rs.tdwg.org/dwc/terms/MeasurementOrFact'},
	occurrenceID: {name:'dwc:Occurrence', uri:'http://rs.tdwg.org/dwc/terms/Occurrence'},
	resourceRelationshipID: {name:'dwc:ResourceRelationship', uri:'http://rs.tdwg.org/dwc/terms/ResourceRelationship'}
}

// Define the record-level terms that should be mapped to the Occurrence class.
DwCASimplifier.prototype.record_level_occurrence = ['dcterms:type', 'institutionID', 'collectionID', 'institutionCode',
    'collectionCode', 'ownerInstitutionCode', 'basisOfRecord'];

// A tree-like data structure that specifies all possible relations.  The top-level
// indices are the subjects, which each point to a list of possible objects and predicates.
DwCASimplifier.prototype.rels_list = {
	'dwc:Identification': {
		'dwc:Taxon': 'bsc:depends_on',
		'dwc:Occurrence': 'bsc:depends_on'
	},
	'dwc:Event': {
		'dcterms:Location': 'bsc:related_to',
		'dwc:GeologicalContext': 'bsc:related_to'
	},
	'dwc:Occurrence': {
		'dwc:Event': 'bsc:depends_on',
		'dwc:GeologicalContext': 'bsc:depends_on',
		'dcterms:Location': 'bsc:depends_on',
		'dwc:Taxon': 'bsc:related_to'
	},
};

// The order in which to map subject classes.  This is important to make sure that classes
// do not first get mapped to Occurrence when they should be mapped to Event or Identification.
DwCASimplifier.prototype.map_order = ['dwc:Identification', 'dwc:Event', 'dwc:Occurrence'];

/**
 * Attempts to "simplify" the data schema of a project by automatically defining as many project
 * elements as possible.  For a Darwin Core Archive, this means specifying joins among concept
 * tables, then inferring concept and attribute definitions.
 *
 * @param project The project to simplify.
 *
 * @returns True if simplification was successful, false otherwise.
 **/
DwCASimplifier.prototype.simplify = function(project) {
	this.project = project;

	// Try to infer joins.  Joins are only possible if there are at least two tables.
	// Object format for a join:
	// {	foreignTable:"maintable", foreignColumn:"eventID",
	// 	primaryTable:"event", primaryColumn:"id"}
	var cnt, table, col, jtablename;
	var projjoins = this.project.getPropertyCopy('joins');
	var projjoinlen = projjoins.length;
	if (this.project.schema.length > 1) {
		// First, find the main table.
		table = this.project.getTableByName('maintable');
		if (table == undefined)
			return false;

		// Inspect all of the ID columns from the main table.
		for (cnt = 0; cnt < table.columns.length; cnt++) {
			col = table.columns[cnt];
			if (col.search(/.*ID$/) != -1) {
				// See if this ID column has a matching table.  If so,
				// define a join for this column and the matching table.
				jtablename = col.replace(/ID$/, '');
				if (this.project.getTableByName(jtablename) != undefined) {
					var newjoin = {
						foreignTable:'maintable', foreignColumn:col,
						primaryTable:jtablename, primaryColumn:'id'
						};
					projjoins.push(newjoin);
				}
			}
		}
	}

	// If we defined new joins, save them to the project.
	if (projjoinlen != projjoins.length)
		this.project.setProperty('joins', projjoins);

	// Now try to figure out the DwC concepts (entities) that are used.
	// Look at each column of each table.
	// Object format for a concept project entry:
	// {	table:"occurrence_txt", idColumn:"taxonID", idPrefixColumn:"",
	// 	rdfClass:{name:"Taxon", uri:"http://rs.tdwg.org/dwc/terms/Taxon"} }
	var cnt2, cncptname, cncpttname;
	var projentities = this.project.getPropertyCopy('entities');
	var projentlen = projentities.length;
	for (cnt = 0; cnt < this.project.schema.length; cnt++) {
		table = this.project.schema[cnt];
		for (cnt2 = 0; cnt2 < table.columns.length; cnt2++) {
			col = table.columns[cnt2];
			// See if this column is a known ID column.
			if (col in this.cncpttable) {
				cncptname = this.cncpttable[col].name;
				cncpttname = col.replace(/ID$/, '');
				// Found an ID column, so create the concept and add it to the project.
				var newcncpt;
				if (this.project.getTableByName(cncpttname) != undefined) {
					// Check if there is a table specifically for this concept; if so, use it to
					// define the concept in the project.
					newcncpt = {
						table:cncpttname, idColumn:'id', idPrefixColumn:(cncpttname + '.id_'),
						rdfClass:{ name:cncptname, uri:this.cncpttable[col].uri }
					};
				} else {
					// Otherwise, use the current table to define the concept.
					newcncpt = {
						table:table.name, idColumn:col, idPrefixColumn:(table.name + '.' + col + '_'),
						rdfClass:{ name:cncptname, uri:this.cncpttable[col].uri }
					};
				}
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
	var cnt3, entity, columns, prop;
	//console.log(vocabularyManager.getSelectedVocabulary());
	
	// The general strategy is to look at each entity (concept) in the project and see if we can define any
	// attributes for it.
	// Object format for an attribute entry:
	// { 	entity:"occurrence_txt.taxonID",
	// 	rdfProperty:{name:"Binomial", uri:"http://rs.tdwg.org/dwc/terms/Binomial"},
	// 	column:"scientificName" }
	var projattributes = this.project.getPropertyCopy('attributes');
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
							 && this.record_level_occurrence.indexOf(prop.name) != -1)
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


	// Finally, define relationships between classes, if possible.  The basic idea is to take each
	// class in this.map_order, see if it is included in the data source, and then try to map it
	// to any matching object classes.
	//
	// The implementation is not particularly efficient in that the entire class list is
	// searched from beginning to end multiple times, but given that there are only 6 classes
	// in common use, it doesn't matter at all for performance.  Plus, this straightforward
	// implementation keeps the code a bit easier to read.
	// Object format for a concept project entry:
	// {	table:"occurrence_txt", idColumn:"taxonID", idPrefixColumn:"",
	// 	rdfClass:{name:"Taxon", uri:"http://rs.tdwg.org/dwc/terms/Taxon"} }
	// Object format for a relations entry:
	// {
	// 	subject: "occurrence_txt.occurrenceID",
	// 	predicate: "bsc:related_to",
	// 	object: "occurrence_txt.taxonID"
	// }
	
	// Keeps track of which concepts we've already mapped to a relation so we don't
	// try to map anything twice.
	var relation_objects = [];

	var ordercnt, cnt, cnt2, subj_concept, subj_class, obj_concept, obj_class;
	var projentities = this.project.getProperty('entities');
	var projrelations = this.project.getPropertyCopy('relations');
	var projrellen = projrelations.length;

	// Do the actual mapping.  Try to map each class in map_order, in order.
	for (ordercnt = 0; ordercnt < this.map_order.length; ordercnt++) {
		// Look at all concepts to try to find a potential subject class.
		for (cnt = 0; cnt < projentities.length; cnt++) {
			// See if this concept matches the class we are currently mapping.
			if (this.map_order[ordercnt] == projentities[cnt].rdfClass.name) {
				// Found a potential subject class.
				subj_concept = projentities[cnt];
				subj_class = subj_concept.rdfClass.name;
				// Next, we need to look at all of the other concepts to try to find
				// an object for the relation (or relations).
				for (cnt2 = 0; cnt2 < projentities.length; cnt2++) {
					// Check if this concept can be the object of a relation with the current
					// subject and if it is not already an object in a relation.
					if (
						projentities[cnt2].rdfClass.name in this.rels_list[subj_class] &&
			       			relation_objects.indexOf(projentities[cnt2].rdfClass.name) == -1
					) {
						// We found a match, so define the new relation.
						obj_concept = projentities[cnt2];
						obj_class = obj_concept.rdfClass.name;
						//alert (subj_class + ": " + obj_class);
						var newrel = {
							subject: subj_concept.table + '.' + subj_concept.idColumn,
							predicate: this.rels_list[subj_class][obj_class],
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

