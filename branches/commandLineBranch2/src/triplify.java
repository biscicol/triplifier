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
        } catch (UnrecognizedOptionException e) {
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

            // Create SQLite file
            String pathPrefix = System.getProperty("user.dir") + File.separator + file.getName();
            sqlitefile = new File(pathPrefix + ".sqlite");
            filecounter = 1;
            while (sqlitefile.exists())
                sqlitefile = new File(pathPrefix + "_" + filecounter++ + ".sqlite");
            tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getName());
            tdc.convert();
            tdr.closeFile();

            // Create Mapping File
            Connection connection = new Connection(sqlitefile);
            Rest r = new Rest();
            crudeSimplifier s = new crudeSimplifier(connection);
            Mapping mapping = new Mapping(connection, s.join, s.entity, s.relation);

            // Triplify
            System.out.println("Triple output stored " + r.getTriples(file.getName(), mapping));
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
