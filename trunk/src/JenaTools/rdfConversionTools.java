package JenaTools;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;

import java.io.File;
import java.io.IOException;

/**
 * Reads RDF files and provides methods for tools meant to convert RDF data into some other format.
 */
public class rdfConversionTools {
    protected String inputFile;
    protected Model model;
    protected QueryExecution qe;
    protected Integer currentDepth = 0;
    protected Integer maxDepth = 99;
    protected Resource rootResource;
    protected File outputDir;

    /**
     * Constructor does the actual reading
     */
    public rdfConversionTools(String inputFile, String lang, Integer maxDepth,Resource rootResource, File outputDir) {
        this.inputFile = inputFile;
        this.maxDepth = maxDepth;
        this.outputDir = outputDir;
        this.rootResource = rootResource;
        // A model for the class tests. The MeasuringStick is the output that we expect
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
        model.read(inputFile, lang);
    }

    /**
     * Get all properties of the specified DwCClass
     *
     * @return a jena ResultSet
     * @throws Exception
     */
    public ResultSet getRootResources() throws Exception {

        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
                        "select ?s  " +
                        "where { " +
                        "?s ?p ?o . " +
                        "?s a <" + rootResource.toString() + "> . " +
                        "FILTER(isLiteral(?o)) . " +
                        "} group by ?s \n ";
        // Create a new query
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        qe = QueryExecutionFactory.create(query, model);
        com.hp.hpl.jena.query.ResultSet results = qe.execSelect();
        return results;
    }

    /**
     * Meant to be over-written
     * @param resultSet
     * @throws IOException
     */
    public void printer(ResultSet resultSet) throws IOException {

    }



}
