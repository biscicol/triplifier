package JenaTools;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;

import java.io.File;
import java.util.ArrayList;

/**
 * Conversion of input RDF data to ISATab Specification.
 * Classes are coded using the Biological Collections Ontology while relationships are translated to graph-based
 * instructions that are used to inform the construction of source-sample based chains as part of the "ISATab study file"
 * and also assays as part of the "ISATab assay files"
 *
 * derives_from (transitive/non-symmetric)  indicates sample-based derivations
 * depends_on (non-transitive/non-symmetric)   indicates
 *
 */
public class rdf2isa extends rdfConversionTools {

    /**
     * Constructor reads the test File
     */
    public rdf2isa(String inputFile, String lang, Integer maxDepth,Resource rootResource, File outputDir) {
        super(inputFile, lang, maxDepth,  rootResource, outputDir);
    }



    /**
     * Print the output of a resultSet... This is just an example of how to work with resultSet
     *
     * @param resultSet
     */
    public void printer(ResultSet resultSet) {
        for (; resultSet.hasNext(); ) {
            QuerySolution soln = resultSet.nextSolution();
            rootResource = soln.getResource("s");
            System.out.println(rootResource);
             printMyLiterals(rootResource);

        }
        // close the Query after printing.
        qe.close();
    }
    public void printMyLiterals(Resource resource) {
        currentDepth++;
         StmtIterator s = resource.listProperties();
            while (s.hasNext()) {
                Statement s2 = s.nextStatement();

                Resource x2 = s2.getSubject();
                Resource r2 = s2.getPredicate();
                RDFNode l2 = s2.getObject();
                if (l2.isLiteral())
                    System.out.println(rootResource + " " + r2.toString() + " " + l2);

                if (!l2.isLiteral() && currentDepth < maxDepth)
                    printMyLiterals(l2.asResource());
            }

         currentDepth--;
    }

    /**
     * for testing, non-static methods should typically control class
     */
    public static void main(String[] args) throws Exception {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);


        String inputFile = "file://" + System.getProperty("user.dir") + "/testdata/rdf2dwcTest.ttl";

        rdf2isa r = new rdf2isa(
                inputFile,
                FileUtils.langTurtle,
                3,
                ResourceFactory.createResource("dwc:Event"),
                org.gbif.utils.file.FileUtils.createTempDir());

        // Output query results
        r.printer(r.getRootResources());
        //r.printer(r.getDwcClass("dwc:Taxon"));
    }

    class Investigation {
        ArrayList<Study> studyArrayList = new ArrayList<Study>();
    }
    class Study {
        ArrayList<Sample> sampleNameArrayList = new ArrayList<Sample>();
    }
    class Sample {
        Protocol protocol;
    }
    class Protocol {
        String protocolURI;
        String protocolName;
    }
    class Assay {

    }


}


