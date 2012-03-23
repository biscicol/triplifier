package rest;

import java.io.PrintWriter;

public class Relation {
	public String subject;
	public String predicate;
	public String object;

	void printD2RQ(PrintWriter pw, Mapping mapping) {
		String[] subjArray = subject.split("\\."),
			objArray = object.split("\\.");
		String subjTbl = subjArray[0],
			subjClmn = subjArray[1],
			objTbl = objArray[0],
			objClmn = objArray[1];
		// check if entities exist
		Entity subjEntity = mapping.findEntity(subjTbl, subjClmn),
				objEntity = mapping.findEntity(objTbl, objClmn);
		if (subjEntity == null || objEntity == null) 
			return;
		String subjClassMap = subjEntity.classMap(),
				objClassMap = objEntity.classMap();
		if (subjTbl.equals(objTbl)) {
			pw.println("map:" + subjClassMap + "_" + objClmn + "_rel" + " a d2rq:PropertyBridge;");
			pw.println("\td2rq:belongsToClassMap " + "map:" + subjClassMap + ";");
			pw.println("\td2rq:property " + predicate + ";");
			pw.println("\td2rq:uriColumn \"" + objTbl + "." + objClmn + "\";");
			pw.println("\t.");		
		} else {
			Join join = mapping.findJoin(subjTbl, objTbl);
			if (join == null) 
				return;
			pw.println("map:" + subjClassMap + "_" + objClmn + "_rel" + " a d2rq:PropertyBridge;");
			pw.println("\td2rq:belongsToClassMap " + "map:" + subjClassMap + ";");
			pw.println("\td2rq:property " + predicate + ";");
			pw.println("\td2rq:refersToClassMap " + "map:" + objClassMap + ";");
			pw.println("\td2rq:join \"" + subjTbl + "." + join.foreignColumn + " => " + objTbl + "." + join.primaryColumn + "\";");
			pw.println("\t.");		
		}
	}
}
