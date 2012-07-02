package rest;

import java.io.PrintWriter;
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
	
    /**
     * Generate D2RQ Mapping Language representation of this Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
	void printD2RQ(PrintWriter pw) {

		pw.println("map:" + classMap() + " a d2rq:ClassMap;");
		pw.println("\td2rq:dataStorage " + "map:database;");
        // This will use the exact value from the database, whether a valid URI or not
		if (idPrefixColumn.equalsIgnoreCase("") || idPrefixColumn == null) {
            pw.println("\td2rq:uriColumn \"" + getColumn() + "\";");
        // This assigns the default urn:x-biscicol: patter before the identifier, ensuring it is a URI
        } else {
            pw.println("\td2rq:uriPattern \""+ idPrefixColumn + "@@" + getColumn() + "@@\";");
        }
	//	pw.println("\td2rq:uriPattern \"" + table + "/@@" + table + "." + idColumn + "|urlify@@\";");
		pw.println("\td2rq:class <" + rdfClass.uri + ">;");
        // ensures non-null values
        pw.println("\td2rq:condition \"" + getColumn() + " <> ''\";");

	//	pw.println("\td2rq:classDefinitionLabel \"" + table + "\";");
		pw.println("\t.");
		for (Attribute attribute : attributes)
			attribute.printD2RQ(pw, classMap(), table);
	}
	
    /**
     * Generate D2RQ Mapping Language ClassMap name of this Entity.
     *
     * @return D2RQ Mapping ClassMap name.
     */
	String classMap() {
		return table + "_" + idColumn;
	}

    /**
     * Get the table.column notation
     * @return
     */
    public String getColumn() {
        return table + "." + idColumn;
    }
}
