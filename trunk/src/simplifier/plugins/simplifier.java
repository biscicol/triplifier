package simplifier.plugins;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
import dbmap.*;
import settings.deepRoots;
import settings.deepRootsReader;
import vocabulary.VocabularyItem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Establish a common method for connecting to SQLLite datasources for various flavors of the simplifier.
 */
public abstract class simplifier {
    protected HashSet<Entity> entity;
    protected HashSet<Join> join;
    protected HashSet<Relation> relation;
    protected Dataseturi dataseturi = new Dataseturi();

    protected deepRoots dRoots;

    protected Connection connection;
    protected DatabaseSchemaInspector schemaInspector;
    protected Database database;
    protected boolean addPrefix;

    public simplifier(Connection connection, boolean addPrefix, String dRootsFile) throws IOException, URISyntaxException {
        this.connection = connection;

        database = connection.getD2RQdatabase();
        schemaInspector = database.connectedDB().schemaInspector();
        entity = new HashSet<Entity>();
        join = new HashSet<Join>();
        relation = new HashSet<Relation>();
        dRoots = new deepRootsReader().createRootData(dRootsFile);

        this.addPrefix = addPrefix;
    }

    /**
     * initializeTerms is meant to be overridden
     */
    protected void initializeTerms() throws Exception {

    }

    /**
     * Return an ArrayList of column names in a given tablename
     *
     * @param tablename
     * @return
     */
    protected ArrayList getColumns(String tablename) {
        List<Attribute> attributes = schemaInspector.listColumns(new RelationName(null, tablename));
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
    protected dbmap.Attribute setAttributeItem(String column, String uri) {
        VocabularyItem vocabularyItem = new VocabularyItem();
        vocabularyItem.name = column;
        vocabularyItem.uri = uri;
        dbmap.Attribute attribute = new dbmap.Attribute();
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
    protected HashSet<dbmap.Attribute> setAttributes(String tablename, ArrayList columnMapArrayList) {
        HashSet<dbmap.Attribute> attributes = new HashSet<dbmap.Attribute>();

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

    public HashSet<Entity> getEntity() {
        return entity;
    }

    public HashSet<Join> getJoin() {
        return join;
    }

    public HashSet<Relation> getRelation() {
        return relation;
    }

    public Dataseturi getDataseturi() {
        return dataseturi;
    }

    public dbmap.Mapping getMapping(Connection connection) {
        dbmap.Mapping mapping = new dbmap.Mapping(connection);
        mapping.entities = this.getEntity();
        mapping.joins = this.getJoin();
        mapping.relations = this.getRelation();
        mapping.dataseturi = this.getDataseturi();
        return mapping;
    }

    /**
     * Create Entity, so calling functions can work with the actual Entity (e.g., putting them into relations)
     */
    protected Entity setEntity(
            String prefix,
            VocabularyItem vocabularyItem,
            String tableName,
            String idColumn,
            ArrayList extraConditions,
            ArrayList attributes) {

        // Entity
        // Entity lEntity = new Entity(vocabularyItem.name);
        Entity lEntity = new Entity();
        lEntity.idColumn = idColumn;
        lEntity.table = tableName;
        lEntity.rdfClass = vocabularyItem;
        lEntity.idPrefixColumn = prefix;
        // Extra Conditions
        // lEntity.extraConditions = extraConditions;
        // Attributes
        if (attributes != null)
            lEntity.attributes = setAttributes(tableName, attributes);
        entity.add(lEntity);

        return lEntity;
    }

    /**
     * Create a join
     */
    protected void setJoin(
            String foreignColumn,
            String foreignTable,
            String primaryColumn,
            String primaryTable
    ) {
        Join lJoin = new Join();
        lJoin.foreignColumn = foreignColumn;
        lJoin.foreignTable = foreignTable;
        lJoin.primaryColumn = primaryColumn;
        lJoin.primaryTable = primaryTable;
        join.add(lJoin);
    }

    /**
     * setRelation
     *
     * @param subject
     * @param predicate
     * @param object
     */
    protected void setRelation(Entity subject,
                               String predicate,
                               Entity object) {
        Relation lRelation = new Relation();
        // setting relation according to the text/delimited syntax since i'm going along with the javascript methods
        // for handling this. Probably not the cleanest approach here
        lRelation.subject = subject.table + "." + subject.idColumn + "." + subject.rdfClass.name;
        lRelation.predicate = predicate;
        lRelation.object = object.table + "." + object.idColumn + "." + object.rdfClass.name;

        relation.add(lRelation);
    }

    /**
     * Map column values to a URI
     */
    protected static class columnMap {
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

    /**
     * Get the prefix associated with a particular concept.  A convenience method for hooking into the DeepRoots class.
     *
     * @param conceptAlias
     * @return
     * @throws Exception
     */
    protected String getPrefix(String conceptAlias) throws Exception {
        return dRoots.lookupPrefix(conceptAlias);
    }

}
