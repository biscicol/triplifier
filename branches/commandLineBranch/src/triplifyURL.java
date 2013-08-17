import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import org.apache.log4j.Level;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.io.File;
import java.net.URLConnection;

import org.apache.commons.io.FilenameUtils;


/**
 * Triplify an input source by supplying URLs for mapping file and input source
 * This will form the basis of a triplification service for projects.
 */
public class triplifyURL {
    ReaderManager rm;

    public triplifyURL() {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Create the ReaderManager and load the plugins.
        rm = new ReaderManager();
        try {
            rm.loadReaders();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Could not load data reader plugins.");
            return;
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: Could not load the SQLite JDBC driver.");
            return;
        }
    }

    /**
     * Given input URLs, triplify them and return a PrintStream
     * @param mapping
     * @param spreadsheet
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public PrintStream triplify(URL mapping, URL spreadsheet) throws ClassNotFoundException, SQLException, IOException {
        File mappingFile = null, spreadsheetFile = null;
        File file, sqlitefile;
        TabularDataReader tdr;
        TabularDataConverter tdc;

        /**
         * Copy files to local directory
         */
        String mappingOut = "/tmp/" + FilenameUtils.getBaseName(mapping.toString()) + "_mapping.n3";
        String spreadsheetOut = "/tmp/" + FilenameUtils.getBaseName(spreadsheet.toString()) + "." + FilenameUtils.getExtension(spreadsheet.toString());

        NetworkFileCopier networkFileCopier = new NetworkFileCopier();
        try {
            mappingFile = networkFileCopier.copyFileFromWeb(mapping.toString(), mappingOut);
            spreadsheetFile = networkFileCopier.copyFileFromWeb(spreadsheet.toString(), spreadsheetOut);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**
         * Convert input file to sqlite database
         */
        tdr = rm.openFile(spreadsheetFile.toString());
        sqlitefile = new File(FilenameUtils.getBaseName(spreadsheet.toString()) + ".sqlite");
        tdc = new TabularDataConverter(tdr, "jdbc:sqlite:/tmp/" + sqlitefile.getName());
        tdc.convert();
        tdr.closeFile();

        /**
         * Convert to N3
         */
        // Prepare the outputStream
        PrintStream printStream = new PrintStream(System.out);
        // Construct the model
        Model model = new ModelD2RQ(FileUtils.toURL(mappingOut), FileUtils.langN3, "urn:x-biscicol:");
        // Write output
        model.write(printStream, FileUtils.langN3);

        return printStream;
    }

    public static void main(String args[]) {

        //String mapping = "https://biocode-fims.googlecode.com/svn/trunk/mappings/biocode_dbtest_mapping.n3";
        //String spreadsheet = "";
        String mapping = "https://biocode-fims.googlecode.com/svn/trunk/mappings/biocode_template.n3";
        String spreadsheet = "https://biocode-fims.googlecode.com/svn/trunk/mappings/biocode_template.xls";
        triplifyURL triplifyURL = new triplifyURL();
        PrintStream printStream = null;

        try {
            printStream = triplifyURL.triplify(new URL(mapping), new URL(spreadsheet));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Simply print This to Output
        printStream.println();
    }


    public static class NetworkFileCopier {
        public File copyFileFromWeb(String address, String filePath) throws Exception {
            byte[] buffer = new byte[1024];
            int bytesRead;

            URL url = new URL(address);
            BufferedInputStream inputStream = null;
            BufferedOutputStream outputStream = null;
            URLConnection connection = url.openConnection();
            // If you need to use a proxy for your connection, the URL class has another openConnection method.
            // For example, to connect to my local SOCKS proxy I can use:
            // url.openConnection(new Proxy(Proxy.Type.SOCKS, newInetSocketAddress("localhost", 5555)));
            inputStream = new BufferedInputStream(connection.getInputStream());
            File f = new File(filePath);
            outputStream = new BufferedOutputStream(new FileOutputStream(f));
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return f;
        }
    }
}
