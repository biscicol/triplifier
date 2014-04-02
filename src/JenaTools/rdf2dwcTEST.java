package JenaTools;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;


public class rdf2dwcTEST {
    private String baseuri;
    Model model;
    QueryExecution qe;
    int depth = 0;          // Go to a maximum Depth in the tree
    private Resource rootResource;

    /**
     * Constructor reads the test File
     */
    public rdf2dwcTEST(String testFile, String lang) {
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
                        "select ?s " +
                        "where { " +
                        //"?s ?p ?o . " +
                        //"?s a <" + queryClass + "> . " +
                        "?s a <" + queryClass + "> . " +
                        //"FILTER(isLiteral(?o)) . " +
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
            rootResource = soln.getResource("s");
             printMyLiterals(rootResource);

        }
        // close the Query after printing.
        qe.close();
    }
    public void printMyLiterals(Resource resource) {
        depth++;
         StmtIterator s = resource.listProperties();
            while (s.hasNext()) {
                Statement s2 = s.nextStatement();

                Resource x2 = s2.getSubject();
                Resource r2 = s2.getPredicate();
                RDFNode l2 = s2.getObject();
                if (l2.isLiteral())
                    System.out.println(rootResource + " " + r2.toString() + " " + l2);

                if (!l2.isLiteral() && depth < 3)
                    printMyLiterals(l2.asResource());
            }

         depth--;
    }

    /**
     * for testing, non-static methods should typically control class
     */
    public static void main(String[] args) throws Exception {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        rdf2dwcTEST r = new rdf2dwcTEST("rdf2dwcTest.ttl", FileUtils.langTurtle);

        // Output query results
        r.printer(r.getDwcClass("dwc:Occurrence"));
        //r.printer(r.getDwcClass("dwc:Taxon"));
    }

}

