package  commander;

import java.io.PrintWriter;

/**
 * Represents a RDF literal property created from a database table.
 * Can generate a D2RQ Mapping entry.
 */
public class Attribute {
	public String column;
	public VocabularyItem rdfProperty;

    /**
     * Generate D2RQ Mapping Language representation of this Attribute.
     *
     * @param pw PrintWriter used to write output to.
     * @param classMap D2RQ Mapping Language ClassMap that this Attribute belongs to.
     * @param table Database table that this Attribute comes from.
     */
	void printD2RQ(PrintWriter pw, String classMap, String table) {
			pw.println("map:" + classMap + "_" + column + " a d2rq:PropertyBridge;");
			pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
			pw.println("\td2rq:property <" + rdfProperty.uri + ">;");
			pw.println("\td2rq:column \"" + table + "." + column + "\";");
			pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");
			pw.println("\t.");	
	}

}
