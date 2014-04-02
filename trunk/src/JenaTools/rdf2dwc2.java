package src.JenaTools;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.text.DwcaWriter;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;

public class rdf2dwc2 {
  private String baseuri;
  Model model;
  QueryExecution qe;
  int depth = 0;
  private Resource rootResource;
  private static final TermFactory TERM_FACTORY = TermFactory.instance();


  /**
   * Constructor reads the test File
   */
  public rdf2dwc2(String testFile, String lang) {
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
   * for testing, non-static methods should typically control class
   */
  public static void main(String[] args) throws Exception {
    // Set logging output level
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    rdf2dwc2 r = new rdf2dwc2("rdf2dwcTest.ttl", FileUtils.langTurtle);

    // Output query results
    r.printer2(r.getDwcClass("dwc:Occurrence"));
    //r.printer(r.getDwcClass("dwc:Taxon"));
  }


  /**
   * Print the output of a resultSet... This is just an example of how to work with resultSet
   *
   * @param resultSet
   */
  public void printer2(ResultSet resultSet) throws IOException {
    File dwcaDir = org.gbif.utils.file.FileUtils.createTempDir();
    DwcaWriter writer = new DwcaWriter(DwcTerm.Occurrence, dwcaDir, false);

    for (; resultSet.hasNext(); ) {
      QuerySolution soln = resultSet.nextSolution();
      rootResource = soln.getResource("s");
      System.out.println("Create new record/row, with ID=" + rootResource);
      writer.newRecord(rootResource.toString());
      printMyLiterals(rootResource, writer);
    }

    // close the Query after printing.
    qe.close();
    // close the DwC-A writer
    writer.close();

    // create zip
    File zip = new File(dwcaDir, "dwca.zip");
    CompressionUtil.zipDir(dwcaDir, zip);
    System.out.println("Success, DwC-A was written to: " + dwcaDir.getAbsolutePath());
  }

  public void printMyLiterals(Resource resource, DwcaWriter writer) {
    depth++;
    StmtIterator s = resource.listProperties();
    while (s.hasNext()) {
      Statement s2 = s.nextStatement();
      Resource r2 = s2.getPredicate();
      RDFNode l2 = s2.getObject();

      // lookup the Term using the predicate, e.g dwc:occurrenceID -> DwcTerm.occurrenceID
      Term term = TERM_FACTORY.findTerm(r2.toString());
      if (l2.isLiteral()) {
        System.out.println("Add column " + term.simpleName() + " to existing record/row with value: " + l2.toString());
        writer.addCoreColumn(term, l2.toString());
      }

      if (!l2.isLiteral() && depth < 3)
        printMyLiterals(l2.asResource(), writer);
    }

    depth--;
  }
}
