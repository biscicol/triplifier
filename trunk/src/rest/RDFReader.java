package rest;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Reads RDF file, extracts Vocabulary containing
 * classes and properties to use in interface.
 * 
 */
public class RDFReader {	
    private Model model = ModelFactory.createDefaultModel();
    private Property propertySubProperty = null; // URI of subProperty Predicate
    private RDFNode propertyName = null; // URI of Property Name
    private Property classSubClass = null; // URI of subClass Predicate
    private Property classProperty = null; // URI of Class designation property (usually just rdf:type)
    private RDFNode className = null; // URI of how Classes are referred to

    private String fileName; // filename of the RDF file (e.g. dwcterms.rdf)

    /**
     * Construct a new RDFReader for a given RDF file. 
     * The file must exist in the vocabularies directory.
     * If the filename is present in triplifiersettings.props: 
     * "vocabularies" - specific vocabulary settings are used, 
     * otherwise - "defaultVocabulary" settings are used. 
     */
    public RDFReader(String fileName) throws Exception {
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
        
        String fileUrl = FileUtils.toURL(Rest.getVocabulariesPath() + fileName);
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
     * Recursively extract vocabulary items from given Iterator.
     *
     * @param iter Model Statement Iterator to loop through.
     * @param subProperty subProperty/subClass to follow.
     * @return A set of extracted vocabulary items.
     */
    private Set<VocabularyItem> getSubItems(StmtIterator iter, Property subProperty) {
    	Set<VocabularyItem> subItems = new TreeSet<VocabularyItem>();
    	while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            Resource subject = stmt.getSubject();     // get the subject
            RDFNode object = stmt.getObject();      // get the object
            
        	Set<VocabularyItem> subSubItems = null;
            if (subProperty != null) {
	            StmtIterator subIter = object.getModel().listStatements(
	                    null,
	                    subProperty,
	                    (RDFNode) subject
	            );
	            subSubItems = getSubItems(subIter, subProperty);
            }
            
            subItems.add(new VocabularyItem(subject.getLocalName(), 
            		subject.toString(), subSubItems));
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
        return new Vocabulary(fileName,getSubItems(propIter, propertySubProperty),
        		getSubItems(classIter, classSubClass));
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
//        System.out.println("Available RDF Files: " + RDFReader.RDFFilesAsJSON(sm) );

        RDFReader or = new RDFReader("dsw.owl");
        
//        System.out.println(or.getProperties());
//        System.out.println(or.getClasses());
        
    	new ObjectMapper().writeValue(System.out, or.getVocabulary());
    }
}
