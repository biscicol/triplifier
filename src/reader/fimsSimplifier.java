package reader;

import de.fuberlin.wiwiss.d2rq.algebra.*;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

import triplifyEngine.*;
import triplifyEngine.Attribute;
import triplifyEngine.Join;
import triplifyEngine.Relation;
import triplifyEngine.VocabularyItem;
import triplifyEngine.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A crude set of simplification rules to simplify DwCA for testing against, VN, Morphbank, etc.
 * For now, we're just focusing on the mainTable and taxon.
 * Ultimately this will need to be re-coded with more robust logic to handle DwCA as constructed by the simplifier
 * in the Javascript code that Brian Stucky wrote.
 */
public class fimsSimplifier {
    public HashSet<Entity> entity;
    public HashSet<Join> join;
    public HashSet<Relation> relation;
    HashSet<Attribute> attribute;

    Connection connection;
    DatabaseSchemaInspector schemaInspector;
    Database database;

    ArrayList taxonExtraConditions = new ArrayList();
    ArrayList taxonProperties = new ArrayList();
    ArrayList specimenProperties = new ArrayList();
    ArrayList identificationProperties = new ArrayList();
    ArrayList tissueProperties = new ArrayList();

    private void initializeTerms() {
        this.entity = new HashSet<Entity>();
        this.relation = new HashSet<Relation>();

        // Taxon Extra Conditions
        taxonExtraConditions.add("SpecificEpithet");
        taxonExtraConditions.add("Phylum");

        // Taxon Attributes
        taxonProperties.add(new columnMap("SpecificEpithet", "dwc:scientificName"));
        taxonProperties.add(new columnMap("Phylum", "dwc:phylum"));

        // Specimen Attributes
        specimenProperties.add(new columnMap("preservative", "bsc:preservative"));
        specimenProperties.add(new columnMap("Host", "dwc:host"));
        specimenProperties.add(new columnMap("relaxant", "bsc:relaxent"));

        // Tissue Attributes
        tissueProperties.add(new columnMap("format_name96", "bsc:plate"));
        tissueProperties.add(new columnMap("well_number96", "bsc:well"));

        // IdentificationProcess
        identificationProperties.add(new columnMap("IdentifiedBy", "dwc:identifiedBy"));

        // EventProcess
        // TODO: insert Event items....

        // Define relations\
        relation.add(new Relation("Specimens.Specimen_Num_Collector.identificationProcess", "<bsc:depends_on>", "Specimens.Specimen_Num_Collector.specimen"));
        relation.add(new Relation("Specimens.Specimen_Num_Collector.informationContentEntity", "<bsc:depends_on>", "Specimens.Specimen_Num_Collector.identificationProcess"));
        relation.add(new Relation("Specimens.Specimen_Num_Collector.tissue", "<bsc:derives_from>", "Specimens.Specimen_Num_Collector.specimen"));

        // TODO: join Event Table to Specimens table
        // TODO: relation of "Specimen bsc:depends_on Event"
        // TODO: relation of "Event bsc:related_to Location"
    }

    /**
     * TODO: Lookup ARKs in a particular table
     * TODO: Think about how i can integrate the above components with a validation engine / parser
     *
     * @param connection
     */
    public fimsSimplifier(Connection connection) {
        initializeTerms();

        database = connection.getD2RQdatabase();
        schemaInspector = database.connectedDB().schemaInspector();
        this.connection = connection;

        // Set Entities
        setEntity(
                "ark:/21547/TEMPIdentificationProcess2_",
                new VocabularyItem("identificationProcess", "http://purl.obolibrary.org/obo/bco_0000042"),
                "Specimens",
                "Specimen_Num_Collector",
                null,
                this.identificationProperties
        );
        setEntity(
                "ark:/21547/Q2_",
                new VocabularyItem("tissue", "http://purl.obolibrary.org/obo/OBI_0100051"),
                "Specimens",
                "Specimen_Num_Collector",
                null,
                this.tissueProperties
        );
        setEntity(
                "ark:/21547/R2_",
                new VocabularyItem("specimen", "http://purl.obolibrary.org/obo/OBI_0100051"),
                "Specimens",
                "Specimen_Num_Collector",
                null,
                this.specimenProperties);

        setEntity(
                "ark:/21547/TEMPICE2_",
                new VocabularyItem("informationContentEntity", "http://purl.obolibrary.org/obo/IAO_0000030"),
                "Specimens",
                "Specimen_Num_Collector",
                this.taxonExtraConditions,
                this.taxonProperties
        );

        /*
        // Set Joins
        this.join = new HashSet<Join>();
        setJoin();
        */
    }

    /**
     * Create Entity
     */
    private void setEntity(
            String prefix,
            VocabularyItem vocabularyItem,
            String tableName,
            String idColumn,
            ArrayList extraConditions,
            ArrayList attributes) {

        // Entity
        Entity lEntity = new Entity(vocabularyItem.name);
        lEntity.idColumn = idColumn;
        lEntity.table = tableName;
        lEntity.rdfClass = vocabularyItem;
        lEntity.idPrefixColumn = prefix;
        // Extra Conditions
        lEntity.extraConditions = extraConditions;
        // Attributes
        lEntity.attributes = setAttributes(tableName, attributes);
        entity.add(lEntity);
    }

    private void setJoin() {
        Join eventLocationJoin = new Join();
        eventLocationJoin.foreignColumn = "id";
        eventLocationJoin.foreignTable = "taxon";
        eventLocationJoin.primaryColumn = "id";
        eventLocationJoin.primaryTable = "mainTable";
        join.add(eventLocationJoin);
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

    /**
     * Set attributes, but only for columns that exist in this particular table
     *
     * @param tablename
     * @param columnMapArrayList
     * @return
     */
    //private HashSet<Attribute> setAttributes(String tablename, HashMap<String, String> mappedConcepts) {
    private HashSet<Attribute> setAttributes(String tablename, ArrayList columnMapArrayList) {
        HashSet<Attribute> attributes = new HashSet<Attribute>();

        Iterator iteratorAvailableColumns = getColumns(tablename).iterator();
        //Iterator iteratorColumnMap = columnMapArrayList.iterator();

        while (iteratorAvailableColumns.hasNext()) {
            String fieldName = (String) iteratorAvailableColumns.next();
            boolean mapped = false;
            for (int i = 0; i < columnMapArrayList.size(); i++) {
                if (!mapped) {
                    columnMap cM = (columnMap) columnMapArrayList.get(i);
                    if (cM.getColumnKey().equals(fieldName)) {
                        attributes.add(setAttributeItem(cM.getColumnKey(), cM.getUriValue()));
                        mapped = true;
                    }
                }
            }
            if (!mapped) {
                //System.out.println("an unmapped column, yay!, now do something for " + fieldName);
            }
        }

        return attributes;
    }


    public static class columnMap {
        private String columnKey;
        private String uriValue;

        public columnMap(String columnKey, String uriValue) {
            this.columnKey = columnKey;
            this.uriValue = uriValue;
        }

        public String getColumnKey() {
            return columnKey;
        }

        public String getUriValue() {
            return uriValue;
        }
    }
}
