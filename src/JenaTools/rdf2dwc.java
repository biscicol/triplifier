package JenaTools;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;


public class rdf2dwc {
    private String baseuri;
    Model model;
    QueryExecution qe;

    /**
     * Constructor reads the test File
     */
    public rdf2dwc(String testFile, String lang) {
        // The base URI for all input and output for these tests
        baseuri = "file://" + System.getProperty("user.dir") + "/testdata/";

        // A model for the class tests. The MeasuringStick is the output that we expect
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        model.read(baseuri + testFile, lang);
    }

    /**
     * Get all properties of the specified DwCClass
     *
     * @param queryClass
     * @return a jena ResultSet
     * @throws Exception
     */
    public com.hp.hpl.jena.query.ResultSet getDwcClass(String queryClass) throws Exception {

        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
                        "select ?s ?p ?o " +
                        "where { " +
                        "?s ?p ?o . " +
                        "?s a <" + queryClass + "> . " +
                        "FILTER(isLiteral(?o)) . " +
                        "} \n ";
        // Create a new query
        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        qe = QueryExecutionFactory.create(query, model);
        com.hp.hpl.jena.query.ResultSet results = qe.execSelect();
        return results;
    }

    /**
     * Print the output of a resultSet... This is just an example of how to work with resultSet
     *
     * @param resultSet
     */
    public void printer(ResultSet resultSet) {
        for (; resultSet.hasNext(); ) {
            QuerySolution soln = resultSet.nextSolution();
            RDFNode x = soln.get("s");
            Resource r = soln.getResource("p");
            Literal l = soln.getLiteral("o");

            // Print output as an example here
            System.out.println(x.toString() + " " + r.toString() + " " + l.toString());
        }
        // close the Query after printing.
        qe.close();
    }

    /**
     * for testing, non-static methods should typically control class
     */
    public static void main(String[] args) throws Exception {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        rdf2dwc r = new rdf2dwc("rdf2dwcTest.ttl", FileUtils.langTurtle);

        // Output query results
        r.printer(r.getDwcClass("dwc:Occurrence"));
        r.printer(r.getDwcClass("dwc:Taxon"));
    }

}

