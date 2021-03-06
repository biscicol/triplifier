import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;

import JenaTools.rdf2dot;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
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
import dbmap.*;
import settings.PathManager;
import simplifier.plugins.*;


/**
 * A command-line tool for using the triplifier.
 * <p/>
 * This tool is in active development and is provided here as a demonstration for interacting with the triplifier at a
 * low level.
 * <p/>
 * We recommend writing a simplifier plugin and inserting in the simplifier.plugins package and extending the simplifier
 * abstract class.  You can see how this works by looking at the dwcSimplifier class.
 * <p/>
 * The genbankSimplifier does NOT extend the simplifier abstract class and shows a method where triples can be constructed
 * using brute-force methods.
 */
public class triplify {

    public static final Double VERSION = 0.3;

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

        /*   opts.addOption("fast", false, "Speed up processing by turning off the DwCA Fixer " +
                 "(see http://biscicol.org/triplifier/doc/reader/DwCAFixer.html).  This saves compute time but " +
                 "the results are not as robust.");
        */

        opts.addOption("debug", false, "Output debug statements and do not delete processing files, " +
                "enabling the user to debug application");

        opts.addOption("f", "format", true, "Set the output format to one of:" +
                "\n...N3" +
                "\n...NTriple (default)" +
                "\n...Turtle" +
                "\n...DOT (Graphviz)");

        opts.addOption("t", "type", true, "Set the input data file type to one of:" +
                "\n...genbank" +
                "\n...dwc (Darwin Core Archive)");

        opts.addOption("m", "mappingFile", true, "Provide a D2RQ mapping file (http://d2rq.org/d2rq-language). " +
                " If this option is set it will ignore all other steps," +
                " and triplify only based on the mapping file.");

        opts.addOption("roots", true, "Provide a deepRoots file (see http://code.google.com/p/bcid/wiki/deepRoots)  " +
                "This option will use Deep Roots mapping for generated GUIDs.");

        opts.addOption("hasGuids", false, "Use this if you have persistent identifiers " +
                "already in place-- that is, they are resolvable, persistent, and especially you must ensure they are all " +
                "properly formatted URIs.  If you do not add this option, the default is to use a system specific prefix.");

        opts.addOption("v", "version", false, "Print the program version and exit.");

        // Create the commands parser and parse the command line arguments.
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }

        // If help was requested or if there are no other options, print the help message and exit.
        if (cl.hasOption("h") || (cl.getOptions().length < 1)) {
            helpf.printHelp("triplify input_files", opts, true);
            return;
        }

        // Set the language options, defaulting to langNTriple
        String language = FileUtils.langNTriple;
        if (cl.hasOption("f")) {
            if (cl.getOptionValue("f").equals("N3")) {
                language = FileUtils.langN3;
            } else if (cl.getOptionValue("f").equals("NTriple")) {
                language = FileUtils.langNTriple;
            } else if (cl.getOptionValue("f").equals("Turtle")) {
                language = FileUtils.langTurtle;
            }
            // Using NTriple as as default language before creating DOT
            else if (cl.getOptionValue("f").equals("DOT")) {
                language = FileUtils.langNTriple;
            }
        }

        // Set the processing directory
        processDirectory = pm.setDirectory(System.getProperty("java.io.tmpdir") + File.separator + "triplifier");

        // User has selected the Mapping File Option
        if (cl.hasOption("m")) {
            try {
                File mappingInputFile = new File(cl.getOptionValue("m"));
                File mappingOutputFile = new File(processDirectory.getAbsoluteFile() + File.separator + mappingInputFile.getName() + ".triples.n3");
                triplifyDirect t = new triplifyDirect(mappingInputFile, mappingOutputFile, language, "urn:x-biscicol:");
                File outputFile = t.getOutputFile();

                // Print the contents of the file
                printContents(cl, outputFile.getAbsoluteFile().toString(), language);

                // Cleanup
                if (!cl.hasOption("debug")) {
                    if (!outputFile.delete()) {
                        System.err.println("Unable to delete triplified file = " + outputFile.getAbsoluteFile());
                    }
                } else {
                    System.out.println("Contents written to " + outputFile.getAbsoluteFile().toString());
                }
            } catch (Exception e) {
                System.err.println("Exception occurred during processing: " + e.getMessage());
            }
            return;
        }

        // Print the version and exit
        if (cl.hasOption("v")) {
            System.out.println("Version " + VERSION + " of the CommandLine Triplifier");
            return;
        }

        // Check the input version
        if (!cl.hasOption("t")) {
            System.err.println("Must specify an input type with the -i option");
            return;
        }

        // Check that input type is valid
        if (!cl.getOptionValue("t").equals("dwc") &&
                !cl.getOptionValue("t").equals("genbank")) {
            System.err.println("Invalid input type");
            return;
        }

        // Setup for hasGuids option
        boolean addPrefix = true;
        if (cl.hasOption("hasGuids")) {
            addPrefix = false;
        }

        /*  // DwCA archive fixer off is the same as "fast" option
     if (cl.hasOption("fast")) {
         fixDwCA = false;
     }   */

        // deepRoot options
        String dRootURL = null;
        if (cl.hasOption("roots")) {
            File dRootFile = new File(cl.getOptionValue("roots"));
            URL url = new URL("file:///" + dRootFile.getAbsolutePath());
            dRootURL = url.toString();
        }

        // Create the ReaderManager and load the plugins.
        ReaderManager rm = new ReaderManager();
        try {
            rm.loadReaders();
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not load data reader plugins.");
            return;
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            System.err.println("Error: Could not load the SQLite JDBC driver.");
            return;
        }

        // Set the input file
        File inputFile = pm.setFile(cl.getArgs()[0]);

        // Handle genbank simplifier
        if (cl.getOptionValue("t").equals("genbank")) {
            // Create TTL output file
            String pathPrefix = processDirectory + File.separator + "genbankOutput";
            File genbankFile = new File(pathPrefix + ".ttl");
            int filecounter = 1;
            while (genbankFile.exists())
                genbankFile = new File(pathPrefix + "_" + filecounter++ + ".ttl");

            genbankSimplifier s = new genbankSimplifier(inputFile, genbankFile);

            // Print the contents of the file
            printContents(cl, genbankFile.getAbsoluteFile().toString(), language);

            if (!cl.hasOption("debug")) {
                // Cleanup the mapping file
                if (!genbankFile.delete()) {
                    System.err.println("Unable to delete genbank triplified file = " + genbankFile.getAbsoluteFile());
                }
            } else {
                System.out.println("Contents written to " + genbankFile.getAbsoluteFile().toString());
            }
        }
        // Handle dwc simplifier
        else {
            int filecounter;
            File sqlitefile;

            tdr = rm.openFile(inputFile.getAbsolutePath());
            if (tdr == null) {
                System.err.println("Error: Unable to open input file " + inputFile.getAbsolutePath());
            }

            // Create SQLite file
            String pathPrefix = processDirectory + File.separator + inputFile.getName();
            sqlitefile = new File(pathPrefix + ".sqlite");
            filecounter = 1;
            while (sqlitefile.exists())
                sqlitefile = new File(pathPrefix + "_" + filecounter++ + ".sqlite");

            tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getAbsolutePath());
            tdc.setDwcFixer(fixDwCA);
            tdc.convert();
            tdr.closeFile();

            // Create connection to SQLlite database
            Connection connection = new Connection(sqlitefile);
            Triplifier r = new Triplifier(processDirectory);

            // Create the dwcSimplifier
            simplifier s = new dwcSimplifier(connection, addPrefix, dRootURL);

            // Create mapping file
            dbmap.Mapping simplifiedMapping = s.getMapping(connection);

            // Triplify and return a filename (this writes a temporary file)
            String fileName = r.getTriples(inputFile.getName(), simplifiedMapping, language);

            // Print the contents of the file
            printContents(cl, fileName, language);

            // Cleaning up
            File tripleOutputFile = new File(fileName);
            if (!cl.hasOption("debug")) {
                // Cleanup sqlite file
                if (!sqlitefile.delete()) {
                    System.err.println("Unable to delete processing file = " + sqlitefile.getAbsoluteFile());
                }

                // Cleanup the processing file from triplifier
                if (!tripleOutputFile.delete()) {
                    System.err.println("Unable to delete temporary file = " + tripleOutputFile.getAbsoluteFile());
                }

                // Cleanup the mapping file
                if (!r.getMappingFile().delete()) {
                    System.err.println("Unable to delete mapping file = " + r.getMappingFile().getAbsoluteFile());
                }
            } else {
                System.out.println("SQLite File = " + sqlitefile.getAbsoluteFile().toString());
                System.out.println("TripleOutput File = " + tripleOutputFile.getAbsoluteFile().toString());
                System.out.println("Mapping File = " + r.getMappingFile().getAbsoluteFile().toString());
            }


        }

        // Finally, clean up our temporary directory
        deleteFilesOlderThan1Hour(processDirectory);

    }

    /**
     * Print the contents of a particular file
     *
     * @param cl
     * @param fileName
     * @param language
     * @throws IOException
     */
    private static void printContents(CommandLine cl, String fileName, String language) throws IOException {
        // Print Graphviz/DOT representation
        if (cl.hasOption("f") && cl.getOptionValue("f").equals("DOT")) {
            Model rdf = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RULES_INF);
            rdf.read("file://" + fileName, "urn:", language);
            System.out.println(rdf2dot.parse(rdf));
        }

        // Print all other formats
        else {
            System.out.println(readFile(fileName));
        }
    }

    /**
     * private, local convenience method fro reading a file and passing contents back to a String
     *
     * @param pathname
     * @return
     * @throws IOException
     */
    private static String readFile(String pathname) throws IOException {

        File file = new File(pathname);
        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    /**
     * Local convenience function for writing a string to a file
     *
     * @param data
     * @param filepath
     * @return
     */
    private static String writeFile(String data, String filepath) {
        FileOutputStream fop = null;
        File file = null;

        try {

            file = new File(filepath);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes = data.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }


    private static InputStream string2InputStream(String input) throws IOException {
        return new ByteArrayInputStream(input.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Function for cleaning up our temp directory
     *
     * @param directory
     */
    public static void deleteFilesOlderThan1Hour(File directory) {
        if (directory.exists()) {

            File[] listFiles = directory.listFiles();
            long purgeTime = System.currentTimeMillis() - (60 * 60 * 1000);
            for (File listFile : listFiles) {
                if (listFile.lastModified() < purgeTime) {
                    if (!listFile.delete()) {
                        System.err.println("Unable to delete file: " + listFile);
                    }
                }
            }
        }
    }
}