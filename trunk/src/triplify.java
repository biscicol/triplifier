import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Level;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import rest.*;

import simplifier.plugins.fimsSimplifier;
import simplifier.plugins.identifierTestsSimplifier;
import simplifier.plugins.ocrSimplifier;
import simplifier.plugins.simplifier;


/**
 * Provides a command-line tool for using the triplifier.
 */
public class triplify {

    public static void main(String[] args) throws Exception {
        String processDirectory = System.getProperty("user.dir") + File.separatorChar;

        // D2RQ uses log4j... usually the DEBUG messages are annoying so here we can just get the ERROR Messages
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        Options opts = new Options();
        HelpFormatter helpf = new HelpFormatter();
        TabularDataReader tdr;
        TabularDataConverter tdc;
        boolean fixDwCA = true;

        // Add the options for the program.
        opts.addOption("h", "help", false, "print this help message and exit");
        opts.addOption("s", "sqlite", false, "output SQLite files only");
        opts.addOption("d", "dontFixDwCA", false, "In cases where we are triplifying DwC Archives, " +
                "don't attempt to them using the DwCFixer. This saves many cycles of compute time but " +
                "the results are not as robust.");
        //opts.addOption("o", "processDirectory", true, "Read and write all files to this directory. Must be fully qualified");
        opts.addOption("t", "simplifierType", true, "*Required {fims|idtest|ocr}");

        opts.addOption("m", "mappingFile", true, "Provide a mapping file.  If this option is set it will ignore all other steps," +
                "not create a SQLlite database but just go straight to triplification by reading the mapping file.");
        opts.addOption("p", "prefixRemover", false, "Do not apply a system prefix.  Use this if you have awesome identifiers" +
                "already in place.");



        // Create the commands parser and parse the command line arguments.
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // If help was requested, print the help message and exit.
        if (cl.hasOption("h") ||( cl.getArgs().length < 1 && cl.getOptions().length < 1)) {
            helpf.printHelp("java triplify input_files", opts, true);
            return;
        }

        // input mapping file option
        if (cl.hasOption("m")) {
            try {
            triplifyDirect triplifyDirect = new triplifyDirect(cl.getOptionValue("m"), cl.getOptionValue("m") + ".triples.n3");
            } catch (Exception e) {
                System.out.println("Exception occurred during processing: " + e.getMessage());
            }
            return;
        }

        if (!cl.hasOption("t")) {
            System.out.println("Must specify a simplifier type with the -t option");
            return;
        }

        // If don't fix DwCA archives then don't try and fix them, speeds it up but may lead to problems
        if (cl.hasOption("d")) {
            System.out.println("Using option: dontFixDwCA");
            fixDwCA = false;
        }

        // if (cl.hasOption("o")) {
        //     processDirectory = cl.getOptionValue("o") + File.separatorChar;
        // }

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
            String filename = processDirectory + fnames[cnt];
            file = new File(filename);

            tdr = rm.openFile(filename);
            if (tdr == null) {
                System.out.println("Error: Unable to open input file " + filename +
                        ".  Will continue trying to read any reamaining input files.");
                continue;
            }

            // Create SQLite file
            System.out.println("Beginning SQlite creation & connection");
            String pathPrefix = processDirectory + file.getName();
            sqlitefile = new File(pathPrefix + ".sqlite");
            filecounter = 1;
            while (sqlitefile.exists())
                sqlitefile = new File(pathPrefix + "_" + filecounter++ + ".sqlite");
            tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getName());
            tdc.convert(fixDwCA);
            tdr.closeFile();

            // Only run the next section if the user did not specify the "s" option
            if (!cl.hasOption("s")) {
                // Create connection to SQLlite database
                Connection connection = new Connection(sqlitefile);
                Rest r = new Rest();

                // Construct the type of simplifier
                System.out.println("Beginning simplifier instantiation");
                simplifier s = null;
                if (cl.getOptionValue("t").equals("fims")) {
                    s = new fimsSimplifier(connection);
                } else if (cl.getOptionValue("t").equals("idtest")) {
                    s = new identifierTestsSimplifier(connection);
                } else if (cl.getOptionValue("t").equals("ocr")) {
                    s = new ocrSimplifier(connection,file.getName());
                } else {
                     System.out.println(cl.getOptionValue("t") + " not a valid simplifier type");
                    return;
                }
                // Create mapping file
                System.out.println("Beginning mapping file creation");
                Mapping mapping = new Mapping(connection, s);

                // Triplify
                System.out.println("Beginning triple file creation");

                String results = r.getTriples(file.getName(), mapping);
                System.out.println("Done! see, " + results);
            }
        }
    }


}