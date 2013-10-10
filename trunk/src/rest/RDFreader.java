package rest;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.map.ObjectMapper;
import settings.SettingsManager;


/**
 * Reads RDF file, extracts Vocabulary containing
 * classes and properties to use in interface.
 * 
 */
public class RDFreader {	
    private Model model = ModelFactory.createDefaultModel();
    private Property propertySubProperty = null; // URI of subProperty Predicate
    private RDFNode propertyName = null; // URI of Property Name
    private Property classSubClass = null; // URI of subClass Predicate
    private Property classProperty = null; // URI of Class designation property (usually rdf:type)
    private RDFNode className = null; // URI of how Classes are referred to
    private Property domainProperty = null; // URI of domain designation property (usually rdfs:domain)
    private Property rangeProperty = null; // URI of range designation property (usually rdfs:range)

    private String fileName; // filename of the RDF file (e.g. dwcterms.rdf)

    /**
     * Construct a new RDFReader for a given RDF file. 
     * The file must exist in the vocabularies directory.
     * If the filename is present in triplifiersettings.props: 
     * "vocabularies" - specific vocabulary settings are used, 
     * otherwise - "defaultVocabulary" settings are used. 
     */
    public RDFreader(String fileName) throws Exception {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        
        String vocabulary = sm.retrieveJsonMap("vocabularies").get(fileName);
        Map<String, String> settings = sm.retrieveJsonMap(vocabulary != null ? vocabulary : "defaultVocabulary");
        Map<String, String> spec = sm.retrieveJsonMap(settings.get("spec"));
        
        className = createResource(spec.get("cName"));
        classProperty = createProperty(spec.get("cProperty"));
        classSubClass = "true".equals(settings.get("subClasses")) ? createProperty(spec.get("cSubClass")) : null;

        propertyName = createResource(spec.get("pName"));
        propertySubProperty = "true".equals(settings.get("subProperties")) ? createProperty(spec.get("pSubProperty")) : null;
        domainProperty = createProperty(spec.get("domain"));
        //System.out.println(Rest.getVocabulariesPath());
        rangeProperty = createProperty(spec.get("range"));
        
        String fileUrl = FileUtils.toURL(Rest.getVocabulariesPath() + "/" + fileName);
        model.read(fileUrl);
        
    	this.fileName = fileName;
    }
    
    private RDFNode createResource(String name) {
    	return name != null && !name.isEmpty() ? ResourceFactory.createResource(name) : null;
    }

    private Property createProperty(String name) {
    	return name != null && !name.isEmpty() ? ResourceFactory.createProperty(name) : null;
    }

    /**
     * Recursively extract sub-properties from given Iterator.
     *
     * @param iter Model Statement Iterator to loop through.
     * @return A set of extracted RDF properties.
     */
    private Set<RDFproperty> getSubProperties(StmtIterator iter) {
    	Set<RDFproperty> subItems = new TreeSet<RDFproperty>();
    	while (iter.hasNext()) {
            Resource subject = iter.nextStatement().getSubject();
            
        	Set<RDFproperty> subSubItems = null;
            if (propertySubProperty != null) 
	            subSubItems = getSubProperties(subject.getModel()
	            		.listStatements(null, propertySubProperty, subject));


            subItems.add(new RDFproperty(subject.getLocalName(), subject.toString(), subSubItems,
            		getProperties(subject, domainProperty), getProperties(subject, rangeProperty)));
        }
    	return subItems;
    }

    /**
     * Retrieves all of the property values for a given "subject" (in the sense
     * of an RDF triple) and property name.
     * 
     * @param resource The subject to retrieve property values for.
     * @param property The property definition to look for.
     * @return The set of property values.
     */
    private Set<String> getProperties(Resource resource, Property property) {
    	if (property == null)
    		return null;
    	Set<String> properties = new HashSet<String>(2);
        StmtIterator domainIter = resource.getModel().listStatements(resource, property, (RDFNode) null);
    	while (domainIter.hasNext()) 
    		properties.add(domainIter.nextStatement().getObject().toString());
    	return properties;
    }
    
    /**
     * Recursively extract sub-classes from given Iterator.
     *
     * @param iter Model Statement Iterator to loop through.
     * @return A set of extracted RDF classes.
     */
    private Set<RDFclass> getSubClasses(StmtIterator iter) {
    	Set<RDFclass> subItems = new TreeSet<RDFclass>();
    	while (iter.hasNext()) {
            Resource subject = iter.nextStatement().getSubject();
            
        	Set<RDFclass> subSubItems = null;
            if (classSubClass != null) 
	            subSubItems = getSubClasses(subject.getModel().listStatements(null, classSubClass, subject));


            subItems.add(new RDFclass(model.getNsURIPrefix(subject.getNameSpace())
                    + ":" + subject.getLocalName(), subject.toString(), subSubItems));
        }
    	return subItems;
    }

    /**
     * Extract Vocabulary from RDF file.
     *
     * @return Extracted Vocabulary.
     */
    public Vocabulary getVocabulary() {
       	StmtIterator propIter = model.listStatements(null, null, propertyName);
        StmtIterator classIter = model.listStatements(null, classProperty, className);
        Vocabulary v = new Vocabulary(fileName, getSubProperties(propIter), getSubClasses(classIter));
        return v;
    }

    /**
     * Main class for testing purposes
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
    	SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        RDFreader or = new RDFreader("triplifier-vocab.rdf");
        
//        System.out.println(or.getProperties());
//        System.out.println(or.getClasses());
        
    	new ObjectMapper().writeValue(System.out, or.getVocabulary());
    }
}
