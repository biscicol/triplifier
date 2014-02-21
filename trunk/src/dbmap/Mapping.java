package dbmap;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Performs two major tasks:
 * - constructor inspects SQL database and generates schema,
 * - printD2RQ translates connection, entities and relations
 * into D2RQ Mapping Language.
 */
public class Mapping {
    public String dateTime;
    public Connection connection;
    public Set<DBtable> schema;
    public Set<Join> joins;
    public Set<Entity> entities;
    public Set<Relation> relations;
    public Dataseturi dataseturi;

    /**
     * For construction from JSON.
     */
    Mapping() {
    }

    /**
     * Create Mapping with dateTime, connection and schema only
     * (joins, entities and relations are empty). Schema is based
     * on inspection of given database connection.
     *
     * @param connection SQL database connection parameters.
     */
    public Mapping(Connection connection) {
        dateTime = DateFormat.getDateTimeInstance().format(new Date());
        this.connection = connection;
        schema = new TreeSet<DBtable>();
        joins = new HashSet<Join>();
        entities = new HashSet<Entity>();
        relations = new HashSet<Relation>();

        Database database = connection.getD2RQdatabase();
        DatabaseSchemaInspector schemaInspector = database.connectedDB().schemaInspector();
//		System.out.println("tables: " + schemaInspector.listTableNames(null));
        DBtable table;
        for (RelationName relationName : schemaInspector.listTableNames(null)) {
            table = new DBtable(relationName.tableName(), new TreeSet<String>(), new HashSet<String>());
            schema.add(table);
            for (Attribute attribute : schemaInspector.listColumns(relationName))
                table.columns.add(attribute.attributeName());
            for (Attribute attribute : schemaInspector.primaryKeyColumns(relationName))
                table.pkColumns.add(attribute.attributeName());
        }

        database.connectedDB().close();
    }

    /**
     * Generate D2RQ Mapping Language representation of this Mapping's connection, entities and relations.
     *
     * @param pw PrintWriter used to write output to.
     */
    public void printD2RQ(PrintWriter pw) throws SQLException {
        printPrefixes(pw);
        connection.printD2RQ(pw);
        for (Entity entity : entities)
            entity.printD2RQ(pw, this);
        for (Relation relation : relations)
            relation.printD2RQ(pw, this);
        if (dataseturi != null)
            dataseturi.printD2RQ(pw, this);
    }

    /**
     * Find the database table join(s) that connect table1 and table2.  This
     * method first checks for a single join that connects table1 and table2.
     * If such a join is found, it is returned as the result.  If a single join
     * cannot be found, the method then checks if table1 and table2 can be
     * connected by two joins to an intermediate table (e.g., many-to-many
     * relationships are typically set up this way).
     *
     * @param table1 Table name.
     * @param table2 Table name.
     * @return An array with the joins that connect the two tables; null if no
     * satisfactory joins could be found.
     */
    Join[] findJoins(String table1, String table2) {
        Join[] mjoins;
        Join join1;

        // First check for a single join that connects table1 and table2.
        join1 = findSingleJoin(table1, table2);
        if (join1 != null) {
            mjoins = new Join[1];
            mjoins[0] = join1;

            return mjoins;
        }

        // No single join matched, so see if two joins can connect table1 and
        // table2 via an intermediate table.
        join1 = null;
        for (Join join : joins) {
            if (table1.equals(join.foreignTable)) {
                join1 = findSingleJoin(join.primaryTable, table2);
            } else if (table1.equals(join.primaryTable)) {
                join1 = findSingleJoin(join.foreignTable, table2);
            }

            if (join1 != null) {
                mjoins = new Join[2];
                mjoins[0] = join;
                mjoins[1] = join1;
                return mjoins;
            }
        }

        return null;
    }

    /**
     * Searches for a single join that connects table1 and table2.
     * 
     * @param table1 table name
     * @param table2 table name
     * @return The matching join or null if none could be found.
     */
    private Join findSingleJoin(String table1, String table2) {
        for (Join join : joins) {
            if (table1.equals(join.foreignTable) && table2.equals(join.primaryTable) ||
                    table1.equals(join.primaryTable) && table2.equals(join.foreignTable)) {
                return join;
            }
        }
        
        return null;
    }

    /**
     * Find Entity defined by given table and idColumn.
     *
     * @param table    Table name.
     * @param idColumn IdColumn name.
     * @return Matching Entity or null if not found.
     */
    Entity findEntity(String table, String idColumn) {
        for (Entity entity : entities)
            if (table.equals(entity.table) && idColumn.equals(entity.idColumn))
                return entity;
        return null;
    }

    /**
     * Sets the URI as a prefix to a column, or not, according to D2RQ conventions
     * @param entity
     * @return
     */
    String getColumnPrefix(Entity entity) {
        String result = "";
        
        if (entity.idPrefixColumn.equalsIgnoreCase("") || entity.idPrefixColumn == null) {
            result += "\td2rq:uriColumn \"" + entity.getColumn() + "\";";
            // This assigns the default urn:x-biscicol: pattern before the identifier, ensuring it is a URI.
        } else {
            result += "\td2rq:uriPattern \"" + entity.idPrefixColumn + "@@" + entity.getColumn() + "@@\";";
        }        
        return result;
    }

    /**
     * Generate all possible RDF prefixes.
     *
     * @param pw PrintWriter used to write output to.
     */
    private void printPrefixes(PrintWriter pw) {
        pw.println("@prefix map: <" + "" + "> .");
        pw.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
        pw.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
        pw.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
        pw.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
        pw.println("@prefix jdbc: <http://d2rq.org/terms/jdbc/> .");
        pw.println("@prefix ro: <http://www.obofoundry.org/ro/ro.owl#> .");
        pw.println("@prefix bsc: <http://biscicol.org/terms/index.html#> .");
        pw.println();
    }
}
