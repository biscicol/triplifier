package rest;

import java.io.PrintWriter;

public class Attribute {
	public String column;
	public String predicate;

	void printD2RQ(PrintWriter pw, String classMap, String table) {
			pw.println("map:" + classMap + "_" + column + " a d2rq:PropertyBridge;");
			pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
			pw.println("\td2rq:property " + predicate + ";");
			pw.println("\td2rq:column \"" + table + "." + column + "\";");
			pw.println("\t.");	
	}

}
