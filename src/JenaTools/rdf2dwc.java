package JenaTools;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class rdf2dwc {
    private static Model inputRDF;
    private static String baseuri;
    private static String classOutputFileName;

    /**
     * for testing, non-static methods should typically control class
     */
    public static void main(String[] args) throws Exception {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // DO THIS BY HAND! Set the output file for the results of triplifying classInput.txt
        // User-interface output
        //classOutputFileName = "classOutputUserInterface.ttl";


        // The base URI for all input and output for these tests
        baseuri = System.getProperty("user.dir");
        baseuri = "file://" + baseuri + "/testdata/";

        // A model for the class tests. The MeasuringStick is the output that we expect
        inputRDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        inputRDF.read(baseuri + "rdf2dwcTest.ttl", null, FileUtils.langNTriple);

        rdf2dwc r = new rdf2dwc();
        r.loopStatements();
    }

    /**
    * Loop statements
     * @throws Exception
     */
    public void loopStatements() throws Exception {
        StmtIterator msIter = inputRDF.listStatements();

        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            String subject = msStmt.getSubject().toString();
            String predicate = msStmt.getPredicate().toString();
            String object = msStmt.getObject().toString();
            System.out.println(subject + " " + predicate + " " + object);
        }

    }



}

