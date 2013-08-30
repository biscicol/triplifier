package rest;

import java.io.PrintWriter;
import java.util.Iterator;

/**
 * Represents a relation between the subject and object Entities.
 * Subject and object Entities are in the form 'table.idColumn'.
 * Can generate a D2RQ Mapping entry.
 */
public class Relation {
    public String subject;
    public String predicate;
    public String object;

    public Relation() {
    }

    public Relation(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    /**
     * Generate D2RQ Mapping Language representation of this Relation.
     *
     * @param pw      PrintWriter used to write output to.
     * @param mapping Mapping containing related Entities (and the Join if the related Entities come from different tables).
     */
    void printD2RQ(PrintWriter pw, Mapping mapping) {
        String subjTbl, subjClmn, objTbl, objClmn, subjQualifier, objQualifier;
        String[] subjArray = subject.split("\\."),
                objArray = object.split("\\.");
        subjTbl = subjArray[0];
        subjClmn = subjArray[1];
        objTbl = objArray[0];
        objClmn = objArray[1];
        try {
            subjQualifier = subjArray[2];
        } catch (Exception e) {
            subjQualifier = null;
        }
        try {
            objQualifier = objArray[2];
        } catch (Exception e) {
            objQualifier = null;
        }

        // check if entities exist
        Entity subjEntity = mapping.findEntity(subjTbl, subjClmn, subjQualifier);
        Entity objEntity = mapping.findEntity(objTbl, objClmn, objQualifier);

        if (subjEntity == null || objEntity == null) {
            return;
        }

        String subjClassMap = subjEntity.classMap();
        String objClassMap = objEntity.classMap();

        //System.out.println(subjTbl + "=" + objTbl + "+" + subjEntity.idPrefixColumn + "-" + objEntity.idPrefixColumn);
        if (subjTbl.equals(objTbl)) {
            pw.println("map:" + subjClassMap + "_" + objClassMap + "_rel" + " a d2rq:PropertyBridge;");
            pw.println("\td2rq:belongsToClassMap " + "map:" + subjClassMap + ";");
            pw.println("\td2rq:property " + predicate + ";");
            pw.println(mapping.getColumnPrefix(objEntity));
            pw.println("\td2rq:condition \"" + objEntity.getColumn() + " <> ''\";");
            pw.println(objEntity.getExtraConditions());
            pw.println(subjEntity.getExtraConditions());
            pw.println("\t.");
        } else {
            Join join = mapping.findJoin(subjTbl, objTbl);
            if (join == null)
                return;
            pw.println("map:" + subjClassMap + "_" + objClassMap + "_rel" + " a d2rq:PropertyBridge;");
            pw.println("\td2rq:belongsToClassMap " + "map:" + subjClassMap + ";");
            pw.println("\td2rq:property " + predicate + ";");
            pw.println("\td2rq:refersToClassMap " + "map:" + objClassMap + ";");
            pw.println("\td2rq:join \"" + join.foreignTable + "." + join.foreignColumn + " => " + join.primaryTable + "." + join.primaryColumn + "\";");
            pw.println("\t.");
        }
    }
}