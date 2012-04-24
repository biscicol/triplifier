package reader;

import com.hp.hpl.jena.rdf.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A class to read files containing Classes and Properties to use in interface
 * User: jdeck
 * Date: 4/23/12
 * Time: 4:36 PM
= */
public class ontReader {
    private Integer prefixCount = 1;
    private String rdfName;
    private Model model = ModelFactory.createDefaultModel();
    private Property propertySubProperty = null;
    private RDFNode propertyName = null;
    private Property classSubClass = null;
    private Property classProperty = null;
    private RDFNode className = null;

    /**
     * Main class for testing purposes
     * @param args
     */
    public static void main(String args[]) {
        ontReader dsw = new ontReader(
                "darwin-sw",
                "dsw.owl",
                "http://www.w3.org/2002/07/owl#Class",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                "http://www.w3.org/2002/07/owl#ObjectProperty",
                null);

        ontReader dwc = new ontReader(
                "DwC",
                "dwcterms.rdf",
                "http://www.w3.org/2000/01/rdf-schema#Class",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                null,
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
                "http://www.w3.org/2000/01/rdf-schema#subPropertyOf");

        //System.out.println(dwc.getClasses());
        //System.out.println(dwc.getProperties());

        System.out.println(dsw.getClasses());
        System.out.println(dsw.getProperties());
    }

    public String getRdfName() {
        return rdfName;
    }

    /**
     * onReader reads OWL and RDF files and extracts classes and property terms from those files.
     * The purpose of this class is not to represent relationships or ontology designations, but
     * merely to collect terms that can be useful to assign to identifiers.
     * @param rdfName is how the abbreviated name for referring to a particular RDF file (e.g. DwC)
     * @param filename is the filename where we find the RDF file (e.g. dwcterms.rdf)
     * @param cName  URI of how Classes are referred to
     * @param cProperty URI of Class designation property (usually just rdf:type)
     * @param cSubClass  URI of subClass Predicate
     * @param pName  URI of Property Name
     * @param pSubProperty  URI of subProperty Predicate
     */
    public ontReader(String rdfName, String filename, String cName, String cProperty, String cSubClass, String pName, String pSubProperty) {
        this.rdfName = rdfName;
        if (cName != null) className = ResourceFactory.createResource(cName);
        if (cProperty != null) classProperty = ResourceFactory.createProperty(cProperty);
        if (cSubClass != null) classSubClass = ResourceFactory.createProperty(cSubClass);

        if (pName != null) propertyName = ResourceFactory.createResource(pName);
        if (pSubProperty != null) propertySubProperty = ResourceFactory.createProperty(pSubProperty);

        InputStream fis = null;
        try {
            File file = new File(filename);
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        model.read(fis, null);
    }

    /**
     * Return the prefix, for formatting
     * @param count
     * @return
     */
    private String prefix (int count) {
        String prefix="";
        for (int i =0 ; i < count; i++) {
            prefix += "\t";
         }
        return prefix;
    }

    /**
     * Render json output of the incoming ontology
     * @param iter is a Statement Iterator to loop
     * @param subProperty name of the subProperty/subClass to follow
     * @param header true/false indicating whether we display the header fields (false for subClasses, subProperties)
     * @param type indicates whether this is class or property
     * @return
     */
    protected String json(StmtIterator iter, Property subProperty, boolean header, String type) {
        String results = "";
        if (header) {
            results += "{\n";
            results += prefix(prefixCount) + "\"type\":\"" + type + "\",\n";
            results += prefix(prefixCount) + "\"name\":\"" + this.rdfName + "\",\n";
            results += prefix(prefixCount) + "\"data\": [\n";
        }
        prefixCount++;

        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object

            results += prefix(prefixCount) + "{\n";
            results += prefix(prefixCount) + "\t\"" + type +"\":\"" + subject.getLocalName() + "\",\n";
            results += prefix(prefixCount) + "\t\"" + type + "URI\":\"" + subject.toString() + "\"";

            if (subProperty != null) {
                StmtIterator subIter = object.getModel().listStatements(
                        null,
                        subProperty,
                        (RDFNode) ResourceFactory.createResource(subject.toString())
                );

                if (subIter.hasNext()) {
                    results += ",\n";
                    prefixCount++;
                    results += prefix(prefixCount) + " \"data\": [\n";
                    prefixCount++;
                    results += json(subIter, subProperty, false, type);
                    prefixCount--;
                    results += prefix(prefixCount) + "]\n";
                    prefixCount--;
                } else {
                    results += "\n";
                }

            }
            results += "\n" + prefix(prefixCount) + "}";
            if (iter.hasNext()) {
                results += ",";
            }
            results += "\n";

        }
        if (header) {
            prefixCount--;
            results += prefix(prefixCount) + "]\n";
            results += "}\n";
        }
        prefixCount--;
        return results;
    }

    /**
     * Render classes and subClasses (if appropriate) in JSON format
     * @return
     */
    public String getClasses() {
        StmtIterator iter = model.listStatements(
                null,
                classProperty,
                (RDFNode) className);
        return json(iter, classSubClass, true, "class");
    }

    /**
     * Render properties and subProperties (if appropriate) in JSON format
     * @return
     */
    public String getProperties() {
        StmtIterator iter = model.listStatements(
                null,
                null,
                (RDFNode) propertyName);
        return json(iter, propertySubProperty, true, "property");
    }
}
