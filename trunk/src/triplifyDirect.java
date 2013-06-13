import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import org.apache.log4j.Level;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class accepts a D2RQ mapping file, containing a direct database connection,
 * and will call D2RQ to produce triples based on this mapping file.
 * This "back-end" approach works well for developers and advanced
 * users wishing to triplify data using options outside of the triplifier interface.
 * This tool is powerful and does not constrain relationships, expressions, or attributes
 * in any way and thus should be used with care.
 *
 * This class is different from triplify in that it cannot load text files or use any of
 * the available Readers.  It only reads in a D2RQ mapping file that contains a database
 * connection definition.  However, this makes it more powerful in that the full triplification
 * process can be completed in a single step.
 *
 * John Deck
 * June 12, 2013
 */
public class triplifyDirect {
    String disclaimer = "This command allows us to triplify from the commandline.  It assumes that the mapping.n3\n" +
                    "file already contains a database connection definition accessible from the host that is running" +
                    "this command-line.  This tool is powerful as it will allow any type of relationship or attribute " +
                    "expressions, including those not possible via the triplifier interface.";

    public triplifyDirect(String mappingFile, String outputFile) {
        // Set Logging Level
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        try {
            // Construct a fileOutputStream to hold the output
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            // Construct the model
            Model model = new ModelD2RQ(FileUtils.toURL(mappingFile), FileUtils.langN3, "urn:x-biscicol:");

            // Write output
            System.out.println("Writing output to " + outputFile);
            model.write(fileOutputStream, FileUtils.langN3);

            // Finish up
            fileOutputStream.close();
            System.out.println("Done!");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows us to triplify from the command line
     * @param args
     */
    public static void main(String args[]) {
        // String D2RQmappingfile = "file:sampledata/biocode_dbtest_mapping.n3";
        // String outputfile = "/tmp/ms_tmp/biocode_dbtest.n3";
        triplifyDirect tD = null;
        if (args.length != 2) {
            System.out.println("Usage: triplifyDirect mapping.n3 output.n3\n" + tD.disclaimer);

        } else {
            tD = new triplifyDirect(args[0], args[1]);
        }
    }
}

