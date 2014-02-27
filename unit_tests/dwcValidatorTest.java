import com.hp.hpl.jena.ontology.OntModelSpec;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;


/**
 * The following tests are structured to test files that are output from the triplifier User Interface.
 *
 * Here is how to run the tests:
 *
 * 1) Two input files are passed as input to the triplifier:
 *     classInput.txt
 *     propertyInput.txt
 *
 * 2) Select "auto-generate project for 'Darwin Core Archive'"
 *
 * 3) In the Concepts section, be sure to select 'yes' for Unique ID's.  The default is no.  In order to structure
 * the tests accurately, we need to reference the same identifiers on the input file as the output file and the best
 * way to do this is to use the same identifiers that were passed in.
 *
 * 4) Triplify the files and name the output as:
 *      classOutputUserInterface.ttl
 *      propertyOutputUserInterface.ttl
 *
 * 5) Run the tests, which compares the above output files to the two files in the sampledata directory called
 * classMeasuringStick.n3 and propertyMeasuringStick.n3
 *
 * @author jdeck, stuckyb, tomc
 */
public class dwcValidatorTest {
    private static Model classMeasuringStick;   // A model containing the correct interpretation for class tests
    private static Model propertyMeasuringStick;// A model containing the correct interpretation for property tests

    private static Model classOutput;           // A model of the class tests output
    private static Model propertyOutput;        // A model of the property tests output

    private static String baseuri;

    private static String classOutputFileName;
    private static String propertyOutputFileName;


    @BeforeClass
    public static void setUpClass() throws IOException {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // DO THIS BY HAND! Set the output file for the results of triplifying classInput.txt
        // User-interface output
        classOutputFileName = "classOutputUserInterface.ttl";
        // DO THIS BY HAND! Set the output file for the results of triplifying propertyInput.txt
        // User-interface output
        propertyOutputFileName = "propertyOutputUserInterface.ttl";

        // The base URI for all input and output for these tests
        baseuri = System.getProperty("user.dir");
        baseuri = "file://" + baseuri + "/sampledata/";

        // A model for the class tests. The MeasuringStick is the output that we expect
        classMeasuringStick = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        classMeasuringStick.read(baseuri + "classMeasuringStick.n3", "urn:", FileUtils.langN3);

        // A model for the class tests output (coming out of the triplifier)
        classOutput = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        classOutput.read(baseuri + classOutputFileName, "urn:", FileUtils.langTurtle);

        // A model for the property tests. The MeasuringStick is the output that we expect
        propertyMeasuringStick = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        propertyMeasuringStick.read(baseuri + "propertyMeasuringStick.n3", "urn:", FileUtils.langN3);

        // A model for the property tests output (coming out of the triplifier)
        propertyOutput = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        propertyOutput.read(baseuri + propertyOutputFileName, "urn:", FileUtils.langTurtle);
    }

    @Test
    public void classTest() throws Exception {
        String unMatchedTriples = "";
        StmtIterator msIter = classMeasuringStick.listStatements();
        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            StmtIterator poIter = classOutput.listStatements();
            boolean match = false;
            while (poIter.hasNext()) {
                Statement outputStmt = poIter.nextStatement();
                if (outputStmt.equals(msStmt)) {
                    match = true;
                }
            }
            // If a match is not found then set this statement.
            if (!match) {
                unMatchedTriples += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
            }
        }
        // Output assertion with message of results
        if (!unMatchedTriples.equals(""))
            assertTrue("The following triples ARE in MeasuringStick but NOT in " + classOutputFileName + ":\n" + unMatchedTriples
                    , false);
        else
            assertTrue(true);


    }

    @Test
    public void propertyTest() throws Exception {
        String unMatchedTriples = "";

        StmtIterator msIter = propertyMeasuringStick.listStatements();
        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            // TODO: find a more formal way to filter out properties we don't want to test
            if (!msStmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") &&
                  !msStmt.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")  ) {
                StmtIterator poIter = propertyOutput.listStatements();
                boolean match = false;
                while (poIter.hasNext()) {
                    Statement outputStmt = poIter.nextStatement();
                    if (outputStmt.equals(msStmt)) {
                        match = true;
                    }
                }
                // If a match is not found then set this statement.
                if (!match) {
                    unMatchedTriples += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
                }
            }
        }
        // Output assertion with message of results
        if (!unMatchedTriples.equals(""))
            assertTrue("The following triples ARE in MeasuringStick but NOT in " + propertyOutputFileName + ":\n" + unMatchedTriples
                    , false);
        else
            assertTrue(true);

    }
}