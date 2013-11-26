import java.io.File;
import java.io.FileNotFoundException;

import de.fuberlin.wiwiss.d2rq.SystemLoader;
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
import commander.*;

import settings.PathManager;
import simplifier.plugins.*;


/**
 * Provides a command-line tool for using the triplifier.
 */
public class triplify {

    public static void main(String[] args) throws Exception {
        File processDirectory = null;//System.getProperty("user.dir") + File.separatorChar;
        PathManager pm = new PathManager();

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
        opts.addOption("o", "outputDirectory", true, "Output all files to this directory. Default is to the use a directory " +
                "called 'tripleOutput' which is a child of the application root");
        opts.addOption("t", "simplifierType", true, "*Required {fims|idtest|ocr|genbank}");
        opts.addOption("m", "mappingFile", true, "Provide a mapping file.  If this option is set it will ignore all other steps," +
                "not create a SQLlite database but just go straight to triplification by reading the mapping file.");
        opts.addOption("p", "prefixRemover", false, "Do not apply a system prefix.  Use this if you have awesome identifiers" +
                "already in place-- that is, they are resolvable, persistent, and especially you must ensure they are all " +
                "properly formatted URIs.  If you do not add this option, the default is to use a system specific prefix." +
                "");

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
        if (cl.hasOption("h") || (cl.getArgs().length < 1 && cl.getOptions().length < 1)) {
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

        // Remove or add prefix option.  Default i
        boolean addPrefix = true;
        if (cl.hasOption("p")) {
            addPrefix = false;
        }

        // If don't fix DwCA archives then don't try and fix them, speeds it up but may lead to problems
        if (cl.hasOption("d")) {
            System.out.println("Using option: dontFixDwCA");
            fixDwCA = false;
        }

        // Set the processing directory
        if (cl.hasOption("o")) {
            processDirectory = pm.setDirectory(cl.getOptionValue("o"));
        } else {
            processDirectory = pm.setDirectory(System.getProperty("user.dir") + File.separator + "tripleOutput");
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
            //String filename = processDirectory + fnames[cnt];
            //file = new File(filename);
            file = pm.setFile(fnames[cnt]);

            // Handle genbank simplifier separately here.  We don't need alot of the other options for this case
            if (cl.getOptionValue("t").equals("genbank")) {
                // Create TTL output file
                String pathPrefix = processDirectory + File.separator + file.getName();
                File genbankFile = new File(pathPrefix + ".ttl");
                filecounter = 1;
                while (genbankFile.exists())
                    genbankFile = new File(pathPrefix + "_" + filecounter++ + ".ttl");

                System.out.println("Reading file " + file.getAbsolutePath());
                new genbankSimplifier(file, genbankFile);
                System.out.println("  Generated output file " + genbankFile.getAbsolutePath());

            }
            // all other cases/ simplifiers
            else {
                tdr = rm.openFile(file.getAbsolutePath());
                if (tdr == null) {
                    System.out.println("Error: Unable to open input file " + file.getAbsolutePath() +
                            ".  Will continue trying to read any reamaining input files.");
                    continue;
                }

                // Create SQLite file
                System.out.println("Beginning SQlite creation & connection");
                String pathPrefix = processDirectory + File.separator + file.getName();
                sqlitefile = new File(pathPrefix + ".sqlite");
                filecounter = 1;
                while (sqlitefile.exists())
                    sqlitefile = new File(pathPrefix + "_" + filecounter++ + ".sqlite");

                tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getAbsolutePath());
                tdc.convert();
                tdr.closeFile();

                // Only run the next section if the user did not specify the "s" option
                if (!cl.hasOption("s")) {
                    // Create connection to SQLlite database
                    Connection connection = new Connection(sqlitefile);
                    Triplifier r = new Triplifier(processDirectory);

                    // Construct the type of simplifier
                    System.out.println("Beginning simplifier instantiation");
                    simplifier s = null;
                    if (cl.getOptionValue("t").equals("fims")) {
                        s = new fimsSimplifier(connection, addPrefix);
                    } else if (cl.getOptionValue("t").equals("idtest")) {
                        s = new identifierTestsSimplifier(connection, addPrefix);
                    } else if (cl.getOptionValue("t").equals("ocr")) {
                        s = new ocrSimplifier(connection, file.getName(), addPrefix);
                    } else {
                        System.out.println(cl.getOptionValue("t") + " not a valid simplifier type");
                        return;
                    }
                    // Create mapping file
                    System.out.println("Beginning mapping file creation");
                    commander.Mapping mapping = new commander.Mapping(connection, s);

                    // Triplify
                    System.out.println("Beginning triple file creation");

                    String results = r.getTriples(file.getName(), mapping);
                    System.out.println("Done! see, " + results);
                }
            }
        }
    }


}