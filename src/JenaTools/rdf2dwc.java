package JenaTools;

import com.hp.hpl.jena.rdf.model.*;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.text.DwcaWriter;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.log4j.Level;

/**
 * Converts data structured as RDF to a DwC archive.   This is done by specifying a "root" node in the RDF input
 * and recursively traversing all known relations and discovering properties that are mapped as Darwin Core terms.
 * All properties/terms are aggregated and inserted into the specified DwC Archive Core which can be either
 * MaterialSample, Occurrence, or Taxon.
 */
public class rdf2dwc extends rdfConversionTools {
    private DwcTerm dwcRootTerm;
    private static final TermFactory TERM_FACTORY = TermFactory.instance();

    /**
     * rdf2dwc reads RDF input and outputs a darwin core archive
     *
     * @param inputFile    is the RDF file to read
     * @param lang         is the language of the RDF file
     * @param depth        is how many levels deep we should look for terms... strongly recommended to set this to a value < 10
     * @param rootResource is the root term to use when traversing the model
     * @param dwcRootTerm  is the root to use for constructing the DwC Archive (materialSample, Taxon, or Occurrence)
     * @param outputDir    is the directory to write output to
     */
    public rdf2dwc(String inputFile, String lang, Integer depth, Resource rootResource, DwcTerm dwcRootTerm, File outputDir) {

        super(inputFile, lang, depth, rootResource, outputDir);
        this.dwcRootTerm = dwcRootTerm;
    }


    /**
     * Print the output of a resultSet.
     *
     * @param resultSet
     */
    public void printer(ResultSet resultSet) throws IOException {
        DwcaWriter writer = new DwcaWriter(dwcRootTerm, outputDir, false);
        // Loop each of the root nodes
        for (; resultSet.hasNext(); ) {
            QuerySolution soln = resultSet.nextSolution();
            System.out.println("Create new record/row, with ID=" + dwcRootTerm);
            writer.newRecord(dwcRootTerm.toString());
            printResourceLiterals(soln.getResource("s"), writer);
        }

        // close the Query after printing.
        qe.close();

        // close the DwC-A writer
        writer.close();

        // create zip
        File zip = new File(outputDir, "dwca.zip");
        CompressionUtil.zipDir(outputDir, zip);

        System.out.println("Success, DwC-A was written to: " + outputDir.getAbsolutePath());
    }

    private void printResourceLiterals(Resource resource, DwcaWriter writer) {
        currentDepth++;
        StmtIterator s = resource.listProperties();
        while (s.hasNext()) {
            Statement s2 = s.nextStatement();
            Resource r2 = s2.getPredicate();
            RDFNode l2 = s2.getObject();

            // lookup the Term using the predicate, e.g dwc:occurrenceID -> DwcTerm.occurrenceID
            Term term = TERM_FACTORY.findTerm(r2.toString());
            if (l2.isLiteral()) {
                writer.addCoreColumn(term, l2.toString());
            }

            if (!l2.isLiteral() && currentDepth < maxDepth)
                printResourceLiterals(l2.asResource(), writer);
        }

        currentDepth--;
    }

    /**
     * for testing, non-static methods should typically control class
     */
    public static void main(String[] args) throws Exception {
        // Set logging output level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // The base URI for all input and output for these tests
        String inputFile = "file://" + System.getProperty("user.dir") + "/testdata/rdf2dwcTest.ttl";

        rdf2dwc r = new rdf2dwc(
                inputFile,
                FileUtils.langTurtle,
                3,
                ResourceFactory.createResource("dwc:Occurrence"),
                DwcTerm.MaterialSample,
                org.gbif.utils.file.FileUtils.createTempDir());

        // Output query results
        r.printer(r.getRootResources());
    }

}
