package rest;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a RDF resource created from a database table.
 * Can generate a D2RQ Mapping entry.
 */
public class Entity {
    public String table;
    public String idColumn;
    public String idPrefixColumn;
    public VocabularyItem rdfClass;
    public Set<Attribute> attributes;

    public ArrayList extraConditions;
    public String qualifier = null;


    public Entity() {
    }

    /**
     * Instantiate with a qualifier.  This constructor inserts a qualifier to make the classMap() unique
     * in cases where the table & id are the same across entities.
     *
     * @param qualifier
     */
    public Entity(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Generate D2RQ Mapping Language representation of this Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
    void printD2RQ(PrintWriter pw, Mapping mapping) {
        pw.println("map:" + classMap() + " a d2rq:ClassMap;");
        pw.println("\td2rq:dataStorage " + "map:database;");
        pw.println(mapping.getColumnPrefix(this));
        //	pw.println("\td2rq:uriPattern \"" + table + "/@@" + table + "." + idColumn + "|urlify@@\";");
        pw.println("\td2rq:class <" + rdfClass.uri + ">;");
        // ensures non-null values
        pw.println("\td2rq:condition \"" + getColumn() + " <> ''\";");
        pw.println(getExtraConditions());

        //	pw.println("\td2rq:classDefinitionLabel \"" + table + "\";");
        pw.println("\t.");
        if (attributes != null) {
            for (Attribute attribute : attributes)
                attribute.printD2RQ(pw, classMap(), table);
        }
    }

    /**
     * Generate D2RQ Mapping Language ClassMap name of this Entity.
     *
     * @return D2RQ Mapping ClassMap name.
     */
    String classMap() {
        if (qualifier == null)
            return table + "_" + idColumn;
        else
            return table + "_" + idColumn + "_" + qualifier;
    }

    /**
     * Get the table.column notation
     *
     * @return
     */
    public String getColumn() {
        return table + "." + idColumn;
    }

    /**
     * Get extraConditions as a string to insert in d2rq definitions
     *
     * @return
     */
    public String getExtraConditions() {
        if (extraConditions == null) {
            return "";
        }
        Iterator it = extraConditions.iterator();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("\td2rq:condition \"");

        while (it.hasNext()) {
            if (!first)
                sb.append(" OR ");
            sb.append(table + "." + it.next() + " <> ''");
            first = false;
        }
        sb.append("\"");
        return sb.toString();
    }
}
