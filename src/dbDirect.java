import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 12/14/12
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class dbDirect {

    public static void main(String args[]) {
        // String D2RQmappingfile = "file:sampledata/biocode_dbtest_mapping.n3";
        //   String outputfile = "/tmp/ms_tmp/biocode_dbtest.n3";

        String D2RQmappingfile = "";
        String outputfile = "";
        if (args.length != 2) {
            System.out.println("Usage: dbDirect mapping.n3 output.n3");
        } else {
            D2RQmappingfile = args[0];
            outputfile = args[1];
        }

        try {
            Model model = new ModelD2RQ(FileUtils.toURL(D2RQmappingfile), FileUtils.langN3, "urn:x-biscicol:");
            FileOutputStream fileOutputStream = null;
            fileOutputStream = new FileOutputStream(outputfile);

            System.out.println("Writing output to " + outputfile);
            model.write(fileOutputStream, FileUtils.langN3);
            fileOutputStream.close();
            System.out.println("Done!");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

