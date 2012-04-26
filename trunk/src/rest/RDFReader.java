package rest;

import com.google.gson.Gson;
import com.hp.hpl.jena.rdf.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A class to read files containing Classes and Properties to use in interface, instantiated using GSON
 * to read JSON objects from a properties file.  Requirements to use this class:
 * 1. JSON string must exist in the properties file (follow example)
 * 2. The names of the ontologies to be used (and referenced in the properties file) must exist
 * as files in the root directory.
 * 3. Write the filenames of the ontologies into build.xml to copy into WEB-INF/classes so they'll be
 * available at runtime.
 * User: jdeck
 * Date: 4/23/12
 * Time: 4:36 PM
 * =
 */
public class RDFReader {
    private Integer prefixCount = 1;
    private Model model = ModelFactory.createDefaultModel();
    private Property propertySubProperty = null;
    private RDFNode propertyName = null;
    private Property classSubClass = null;
    private Property classProperty = null;
    private RDFNode className = null;

    private String rdfName;
    private String displayName;
    private String filename;
    private String cName;
    private String cProperty;
    private String cSubClass;
    private String pName;
    private String pSubProperty;

    /**
     * Default empty constructor for gson
     * rdfName is how the abbreviated name for referring to a particular RDF file (e.g. DwC)
     * filename is the filename where we find the RDF file (e.g. dwcterms.rdf)
     * cName  URI of how Classes are referred to
     * cProperty URI of Class designation property (usually just rdf:type)
     * cSubClass  URI of subClass Predicate
     * pName  URI of Property Name
     * pSubProperty  URI of subProperty Predicate
     */
    public RDFReader() {
    }

    /**
     * Must call init() in order to run this application and call getClass and getProperties
     */
    public void init() {
        if (!cName.equals("")) className = ResourceFactory.createResource(cName);
        if (!cProperty.equals("")) classProperty = ResourceFactory.createProperty(cProperty);
        if (!cSubClass.equals("")) classSubClass = ResourceFactory.createProperty(cSubClass);

        if (!pName.equals("")) propertyName = ResourceFactory.createResource(pName);
        if (!pSubProperty.equals("")) propertySubProperty = ResourceFactory.createProperty(pSubProperty);

        InputStream fis = null;
        try {
            File file = new File(Thread.currentThread().getContextClassLoader().getResource(filename).getFile());
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        model.read(fis, null);
    }

    public String getRdfName() {
        return rdfName;
    }

    /**
     * Return the prefix, for formatting
     *
     * @param count
     * @return
     */
    private String prefix(int count) {
        String prefix = "";
        for (int i = 0; i < count; i++) {
            prefix += "\t";
        }
        return prefix;
    }

    /**
     * Render json output of the incoming ontology
     *
     * @param iter        is a Statement Iterator to loop
     * @param subProperty name of the subProperty/subClass to follow
     * @param header      true/false indicating whether we display the header fields (false for subClasses, subProperties)
     * @param type        indicates whether this is class or property
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
            results += prefix(prefixCount) + "\t\"" + type + "\":\"" + subject.getLocalName() + "\",\n";
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
     *
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
     *
     * @return
     */
    public String getProperties() {
        StmtIterator iter = model.listStatements(
                null,
                null,
                (RDFNode) propertyName);
        return json(iter, propertySubProperty, true, "property");
    }

    /**
     * Main class for testing purposes
     *
     * @param args
     */
    public static void main(String args[]) {
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        RDFReader or = new Gson().fromJson(sm.retrieveValue("dsw"), RDFReader.class);
        or.init();
        System.out.println(or.getClasses());
    }
}
