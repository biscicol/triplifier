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
 * <p/>
 * Here is how to run the tests:
 * <p/>
 * 1) Two input files are passed as input to the triplifier:
 * classInput.txt
 * propertyInput.txt
 * <p/>
 * 2) Select "auto-generate project for 'Darwin Core Archive'"
 * <p/>
 * 3) In the Concepts section, be sure to select 'yes' for Unique ID's.  The default is no.  In order to structure
 * the tests accurately, we need to reference the same identifiers on the input file as the output file and the best
 * way to do this is to use the same identifiers that were passed in.
 * <p/>
 * 4) Triplify the files and name the output as:
 * classOutputUserInterface.ttl
 * propertyOutputUserInterface.ttl
 * <p/>
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

    /**
     * Construct the models used for the tests
     *
     * @throws IOException
     */
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
        baseuri = "file://" + baseuri + "/testdata/";

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

    /**
     * Test the expression of relationships between classes by looking for "expected" output from the
     * classMeasuringStick model against a model that is created by reading output from the triplifier
     * user interface.  The classMeasuringStick model contains "inferred" occurrences, designated by an
     * uppercase "O" that indicate no Occurrence was expressed in the model but some other class was that
     * indicates some implied occurrence.  This is a tricky concept.  In some cases occurrences certainly
     * be implied (such as data expressing only an Identification & a Taxon) and in other cases may not
     * be suitable at all (such as data expressing only a Taxon).  In any case, for the purposes of the
     * tests run in the class, we ignore any supposed "inferred" Occurrence relationships.
     *
     * @throws Exception
     */
    @Test
    public void classNoInferredOccurrencesTest() throws Exception {
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
            // For this test, do NOT look at the inferred Occurrence, which contains an uppercase "O" in either the
            // subject or the object
            if (!msStmt.getSubject().toString().contains("O") &&
                    !msStmt.getObject().toString().contains("O")) {
                // If a match is not found then set this statement.
                if (!match) {
                    unMatchedTriples += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
                }
            }
        }
        // Output assertion with message of results
        if (!unMatchedTriples.equals(""))
            assertTrue("\nThe following triples ARE in classMeasuringStick but NOT in " + classOutputFileName + ":\n" + unMatchedTriples
                    , false);
        else
            assertTrue(true);
    }

    /**
     * This test is the REVERSE of classNoInferredOccurrencesTest, looking at the output file and finds triples
     * that are NOT in the classMeasuringStickFile.
     *
     * @throws Exception
     */
    @Test
    public void reverseClassNoInferredOccurrencesTest() throws Exception {
        String unMatchedTriples = "";
        StmtIterator msIter = classOutput.listStatements();

        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            StmtIterator poIter = classMeasuringStick.listStatements();
            boolean match = false;
            while (poIter.hasNext()) {
                Statement outputStmt = poIter.nextStatement();
                if (outputStmt.equals(msStmt)) {
                    match = true;
                }
            }
            // For this test, we ignore the following cases:
            // 1. do NOT look at the inferred Occurrence (which contains an uppercase "O")
            // 2. predicates equal to type, subClassOf, equivalentClass since they're not declared in the measuringStick file
            // 3. classInput.txt here since this is a file designation that has a naming style and not directly related to DwC triplification
            if (!msStmt.getSubject().toString().contains("O") &&
                    !msStmt.getObject().toString().contains("O") &&
                    !msStmt.getPredicate().toString().contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") &&
                    !msStmt.getPredicate().toString().contains("http://www.w3.org/2000/01/rdf-schema#subClassOf") &&
                    !msStmt.getPredicate().toString().contains("http://www.w3.org/2002/07/owl#equivalentClass") &&
                    !msStmt.getSubject().toString().contains("classInput.txt")
                    ) {
                // If a match is not found then set this statement.
                if (!match) {
                    unMatchedTriples += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
                }
            }
        }
        // Output assertion with message of results
        if (!unMatchedTriples.equals(""))
            assertTrue("\nThe following triples ARE " + classOutputFileName + " but NOT in classMeasuringStick.n3:\n" + unMatchedTriples
                    , false);
        else
            assertTrue(true);
    }

    /**
     * Look for empty elements in the the propertyOutputFile
     * @throws Exception
     */
    @Test
    public void emptyElementsInPropertyFile() throws Exception {
        String output = "";

        StmtIterator msIter = propertyOutput.listStatements();
        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            if (msStmt.getSubject().toString().equals("") ||
                    msStmt.getPredicate().toString().equals("") ||
                    msStmt.getObject().toString().equals("") ||
                    msStmt.getSubject().toString().equals("urn:") ||
                    msStmt.getPredicate().toString().equals("urn:") ||
                    msStmt.getObject().toString().equals("urn:")
                    ) {
                output += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
            }
        }
        // Output assertion with message of results
        if (!output.equals(""))
            assertTrue("\nThe following triples in " + propertyOutputFileName + " have an empty element:\n" + output
                    , false);
        else
            assertTrue(true);
    }

    /**
     * Look for empty elements in the the classOutputFile
     * @throws Exception
     */
    @Test
    public void emptyElementsInClassFile() throws Exception {
        String output = "";

        StmtIterator msIter = classOutput.listStatements();
        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            if (msStmt.getSubject().toString().equals("") ||
                    msStmt.getPredicate().toString().equals("") ||
                    msStmt.getObject().toString().equals("")  ||
                    msStmt.getSubject().toString().equals("urn:") ||
                    msStmt.getPredicate().toString().equals("urn:") ||
                    msStmt.getObject().toString().equals("urn:")
                    ) {
                output += msStmt.getSubject() + " " + msStmt.getPredicate().toString() + " " + msStmt.getObject().toString() + " .\n";
            }
        }
        // Output assertion with message of results
        if (!output.equals(""))
            assertTrue("\nThe following triples in " + classOutputFileName + " have an empty element:\n" + output
                    , false);
        else
            assertTrue(true);
    }

    /**
     * Test the expression of property expressions by looking for "expected" output from the
     * propertyMeasuringStick model against a model that is created by reading output from the triplifier
     * user interface
     *
     * @throws Exception
     */
    @Test
    public void propertyTest() throws Exception {
        String unMatchedTriples = "";
        StmtIterator msIter = propertyMeasuringStick.listStatements();
        while (msIter.hasNext()) {
            Statement msStmt = msIter.nextStatement();
            // TODO: find a more formal way to filter out properties we don't need to test
            if (!msStmt.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") &&
                    !msStmt.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")) {
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
            assertTrue("\nThe following triples ARE in " + propertyOutputFileName + " but NOT in propertyMeasuringStick.n3:\n" + unMatchedTriples
                    , false);
        else
            assertTrue(true);

    }

}
