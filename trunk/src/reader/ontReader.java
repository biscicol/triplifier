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
 * To change this template use File | Settings | File Templates.
 */
public class ontReader {
    Integer prefixCount = 1;
    String rdfName;
    Model model = ModelFactory.createDefaultModel();
    Property propertySubProperty = null;
    RDFNode propertyName = null;
    Property classSubClass = null;
    Property classProperty = null;
    RDFNode className = null;

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

        dsw.getClasses();
        //dwc.getProperties();
    }

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

    private String prefix (int count) {
        String prefix="";
        for (int i =0 ; i < count; i++) {
            prefix += "\t";
         }
        return prefix;
    }

    private String json(StmtIterator iter, Property subProperty, boolean header, String type) {
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
                StmtIterator iter2 = object.getModel().listStatements(
                        null,
                        subProperty,
                        (RDFNode) ResourceFactory.createResource(subject.toString())
                );

                if (iter2.hasNext()) {
                    results += ",\n";
                    prefixCount++;
                    results += prefix(prefixCount) + " \"subClasses\": [\n";
                    prefixCount++;
                    results += json(iter2, subProperty, false, type);
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

    private void getClasses() {

        StmtIterator iter = model.listStatements(
                null,
                classProperty,
                (RDFNode) className);

        System.out.println(json(iter, classSubClass, true, "class"));
    }


    private void getProperties() {

        StmtIterator iter = model.listStatements(
                null,
                null,
                (RDFNode) propertyName);

        System.out.println(json(iter, propertySubProperty, true, "property"));
    }
}
