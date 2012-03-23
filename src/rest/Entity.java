package rest;

import java.io.PrintWriter;
import java.util.Set;

public class Entity {
	public String table;
	public String idColumn;
	public String rdfClass;
	public Set<Attribute> attributes;
	
	void printD2RQ(PrintWriter pw) {
		pw.println("map:" + classMap() + " a d2rq:ClassMap;");
		pw.println("\td2rq:dataStorage " + "map:database;");
		pw.println("\td2rq:uriColumn \"" + table + "." + idColumn + "\";");
	//	out.println("\td2rq:uriPattern \"@@" + mapping.table + "." + mapping.idColumn + "@@\";");
	//	out.println("\td2rq:uriPattern \"" + mapping.table + "/@@" + mapping.table + "." + mapping.idColumn + "|urlify@@\";");
		pw.println("\td2rq:class " + rdfClass + ";");
	//	out.println("\td2rq:classDefinitionLabel \"" + mapping.table + "\";");
		pw.println("\t.");
		for (Attribute attribute : attributes)
			attribute.printD2RQ(pw, classMap(), table);
	}
	
	String classMap() {
		return table + "_" + idColumn;
	}
}
