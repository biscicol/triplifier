import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import com.sun.org.apache.bcel.internal.generic.NEW;
import de.fuberlin.wiwiss.d2rq.algebra.*;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Level;
import org.joda.time.DateTime;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import rest.*;
import rest.Attribute;
import rest.Join;
import rest.Relation;


/**
 * Provides a command-line tool for using the triplifier.
 */
public class triplify {

    public static void main(String[] args) throws Exception {

        // D2RQ uses log4j... usually the DEBUG messages are annoying so here we can just get the ERROR Messages
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        Options opts = new Options();
        HelpFormatter helpf = new HelpFormatter();
        TabularDataReader tdr;
        TabularDataConverter tdc;
        boolean fixDwCA = true;

        // Add the options for the program.
        opts.addOption("s", "sqlite", false, "output SQLite files only");
        opts.addOption("d", "dontFixDwCA", false, "In cases where we are triplifying DwC Archives, " +
                "don't attempt to them using the DwCFixer. This saves many cycles of compute time but " +
                "the results are not as robust.");
        opts.addOption("h", "help", false, "print this help message and exit");
        //opts.addOption("o", "processDirectory", true, "Read and write all files to this directory. Must be fully qualified");

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
        if (cl.hasOption("h") || cl.getArgs().length < 1) {
            helpf.printHelp("java triplify input_files", opts, true);
            return;
        }


        // If don't fix DwCA archives then don't try and fix them, speeds it up but may lead to problems
        if (cl.hasOption("d")) {
            System.out.println("Using option: dontFixDwCA");
            fixDwCA = false;
        }

        String processDirectory = System.getProperty("user.dir") +  File.separatorChar;
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
            file = new File( filename);

            tdr = rm.openFile(filename);
            if (tdr == null) {
                System.out.println("Error: Unable to open input file " + filename +
                        ".  Will continue trying to read any reamaining input files.");
                continue;
            }

            // Create SQLite file
            System.out.println("Beginning SQlite creation & connection : " + DateTime.now());
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

                // Construct the crudeSimplifier
                System.out.println("Beginning simplifier instantiation: " + DateTime.now());
                crudeSimplifier s = new crudeSimplifier(connection);

                // Create mapping file
                System.out.println("Beginning mapping file creation : " + DateTime.now());
                Mapping mapping = new Mapping(connection, s.join, s.entity, s.relation);

                // Triplify
                System.out.println("Beginning triple file creation : " + DateTime.now());
                String results = r.getTriples(file.getName(), mapping);
                System.out.println("Done! see, " + results + ", ending time : " + DateTime.now());
            }
        }
    }


    /**
     * A crude set of simplification rules to simplify DwCA for testing against, VN, Morphbank, etc.
     * For now, we're just focusing on the mainTable and taxon.
     * Ultimately this will need to be re-coded with more robust logic to handle DwCA as constructed by the simplifier
     * in the Javascript code that Brian Stucky wrote.
     */
    public static class crudeSimplifier {
        HashSet<Entity> entity;
        HashSet<Join> join;
        HashSet<Relation> relation;
        HashSet<Attribute> attribute;
        Connection connection;
        DatabaseSchemaInspector schemaInspector;
        Database database;

        crudeSimplifier(Connection connection) {
            database = connection.getD2RQdatabase();
            schemaInspector = database.connectedDB().schemaInspector();

            this.connection = connection;
            this.entity = new HashSet<Entity>();
            setOccurrenceEntity();

            /*
            this.join = new HashSet<Join>();
            this.relation = new HashSet<Relation>();
            this.attribute = new HashSet<Attribute>();
            setTaxonEntity();
            setJoin();
            setRelation();
            */
            //
        }

        /**
         * Return an ArrayList of column names in a given tablename
         *
         * @param tablename
         * @return
         */
        private ArrayList getColumns(String tablename) {
            List<de.fuberlin.wiwiss.d2rq.algebra.Attribute> attributes = schemaInspector.listColumns(new RelationName(null, tablename));
            Iterator it = attributes.iterator();
            ArrayList arrayList = new ArrayList();
            while (it.hasNext()) {

                de.fuberlin.wiwiss.d2rq.algebra.Attribute a = (de.fuberlin.wiwiss.d2rq.algebra.Attribute) it.next();
                arrayList.add(a.attributeName());
            }
            return arrayList;
        }

        /**
         * @param column
         * @param uri
         * @return
         */
        private Attribute setAttributeItem(String column, String uri) {
            VocabularyItem vocabularyItem = new VocabularyItem();
            vocabularyItem.name = column;
            vocabularyItem.uri = uri;
            Attribute attribute = new Attribute();
            attribute.column = column;
            attribute.rdfProperty = vocabularyItem;
            return attribute;
        }

        private void setTaxonEntity() {
            Entity taxonEntity = new Entity();
            taxonEntity.idColumn = "id";
            taxonEntity.table = "taxon";
            taxonEntity.rdfClass = new VocabularyItem("taxon", "dwc:Taxon");
            taxonEntity.idPrefixColumn = "urn:x-biscicol:taxon.taxonID_";

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("scientificName", "dwc:scientificName");

            taxonEntity.attributes = setAttributes("taxon", map);
            ;
            entity.add(taxonEntity);
        }

        private HashSet<Attribute> setAttributes(String tablename, HashMap<String, String> columns) {
            HashSet<Attribute> attributes = new HashSet<Attribute>();

            ArrayList availableColumns = getColumns(tablename);
            Iterator it = columns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                if (availableColumns.contains(pairs.getKey())) {
                    attributes.add(setAttributeItem(pairs.getKey().toString(), pairs.getValue().toString()));
                }
                it.remove();
            }
            return attributes;
        }

        private void setOccurrenceEntity() {
            ArrayList columns = getColumns("maintable");

            Entity mainTableEntity = new Entity();
            if (columns.contains("id"))
                mainTableEntity.idColumn = "id";
            else if (columns.contains("occurrenceID"))
                mainTableEntity.idColumn = "occurrenceID";

            mainTableEntity.table = "mainTable";
            mainTableEntity.rdfClass = new VocabularyItem("mainTable", "dwc:Occurrence");
            // TODO: Assign ARKs here?
            mainTableEntity.idPrefixColumn = "urn:x-biscicol:maintable.occurrenceID_";

            //HashSet<Attribute> attributes = new HashSet<Attribute>();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("institutionCode", "dwc:institutionCode");
            map.put("collectionCode", "dwc:collectionCode");
            map.put("catalogNumber", "dwc:catalogNumber");
            map.put("datasetID", "dwc:datasetID");
            map.put("institutionID", "dwc:institutionID");
            map.put("collectionID", "dwc:collectionID");
            map.put("occurrenceID", "dwc:occurrenceID");
            map.put("associatedSequences", "dwc:associatedSequences");
            map.put("associatedOccurrences", "dwc:associatedOccurrences");
            map.put("otherCatalogNumbers", "dwc:otherCatalogNumbers");

            mainTableEntity.attributes = setAttributes("maintable", map);
            entity.add(mainTableEntity);
        }

        private void setJoin() {
            Join eventLocationJoin = new Join();
            eventLocationJoin.foreignColumn = "id";
            eventLocationJoin.foreignTable = "taxon";
            eventLocationJoin.primaryColumn = "id";
            eventLocationJoin.primaryTable = "mainTable";
            join.add(eventLocationJoin);
        }

        private void setRelation() {
            Relation eventLocationRelation = new Relation();
            eventLocationRelation.subject = "mainTable.id";
            eventLocationRelation.predicate = "<bsc:related_to>";
            eventLocationRelation.object = "taxon.id";
            relation.add(eventLocationRelation);
        }

    }
}