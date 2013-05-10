
import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;


/**
 * Provides a command-line tool for using the triplifier.
 */
public class triplify
{
    public static void main(String[] args) throws Exception {
        Options opts = new Options();
        HelpFormatter helpf = new HelpFormatter();
        TabularDataReader tdr;
        TabularDataConverter tdc;
        
        // Add the options for the program.
        opts.addOption("s", "sqlite", false, "output SQLite files only");
        opts.addOption("h", "help", false, "print this help message and exit");
        
        // Create the commands parser and parse the command line arguments.
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(opts, args);
        } catch(UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        
        // If help was requested, print the help message and exit.
        if (cl.hasOption("h")) {
            helpf.printHelp("java triplify input_files", opts, true);
            return;
        }
        
        // Create the ReaderManager and load the plugins.
        ReaderManager rm = new ReaderManager();
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
        
        String[] fnames = cl.getArgs();
        File file, sqlitefile;
        int filecounter;
        
        // Process each input file specified on the command line.
        for (int cnt = 0; cnt < fnames.length; cnt++) {
            file = new File(fnames[cnt]);
            
            tdr = rm.openFile(fnames[cnt]);
            if (tdr == null) {
                System.out.println("Error: Unable to open input file " + fnames[cnt] +
                        ".  Will continue trying to read any reamaining input files.");
                continue;
            }
            
            // Generate an unused name for the new SQLite file.
            sqlitefile = new File(file.getName() + ".sqlite");
            filecounter = 1;
            while (sqlitefile.exists())
                sqlitefile = new File(file.getName() + "_" + filecounter++ + ".sqlite");

            tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getName());
            tdc.convert();
            tdr.closeFile();
        }
    }
}
