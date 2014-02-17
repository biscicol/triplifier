package dbmap;

import javax.management.relation.RelationService;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a DataSet and places the DataSet at the top level of the graph.
 * Typically, this should be represented as an option if the user wants to tie
 * their top level node to a dataset identifier.
 * Subject Entities are in the form 'table.idColumn'.
 * Can generate a D2RQ Mapping entry.
 */

public class Dataseturi {
    public String name;

    /**
     * Generate D2RQ Mapping Language representation of this Relation.
     *
     * @param pw PrintWriter used to write output to.
     */
    void printD2RQ(PrintWriter pw, Mapping mapping) {
        Set<Relation> relations = mapping.relations;

        // The commented-out code below began with the intention of inspecting
        // the defined relations to decide which entity (concept) to connect to
        // the data source identifier.  However, the functionality was never
        // completed, and it was replaced with the simpler approach implemented
        // below.  For now, the old code is left here in case this problem is
        // re-visited in the future.
        /*String subject = "", predicate = "", object = "";
        // TODO: Figure out the top level relation, for now it just gets the last one expressed.
        for (Relation relation : relations) {
            subject = relation.subject;
            predicate = relation.predicate;
            object = relation.object;
            System.out.println(relation.subject + " " + relation.predicate + " " + relation.object);
        }

        Entity subjEntity;
        // If no relations built then subject will by empty, hence, just use the first assigned Entity
        if (subject.equals("") || subject == null) {
            Iterator it = mapping.entities.iterator();
            subjEntity = (Entity) it.next();
        //
        } else {
            // Find the subject
            String subjTbl, subjClmn;
            String[] subjArray = subject.split("\\.");
            subjTbl = subjArray[0];
            subjClmn = subjArray[1];

            // check if entities exist
            subjEntity = mapping.findEntity(subjTbl, subjClmn);
            if (subjEntity == null)
                return;
        }*/
        
        // See if instances of dwc:Occurrence are defined.  If so, use those
        // instances to connect to the data source identifier.  Otherwise, just
        // use the first entity (concept) that is returned.
        Entity subjEntity = mapping.entities.iterator().next();
        for (Entity entity : mapping.entities) {
            if (entity.rdfClass.name.equals("dwc:Occurrence"))
                subjEntity = entity;
        }

        String subjClassMap = subjEntity.classMap();

        // Create a static DataSet Class Map
        pw.println("map:DataSet a d2rq:ClassMap;");
        pw.println("\td2rq:dataStorage map:database;");
        pw.println("\td2rq:constantValue <" + mapping.dataseturi.name + ">;");
        // TODO: make this not hardcoded.
        pw.println("\td2rq:class <http://rs.tdwg.org/dwc/terms/DataSet>;");
        pw.println("\t.");

        // Associate the SubjClassMap with the DataSet Class Map
        pw.println("map:DataSet_" + subjClassMap + " a d2rq:PropertyBridge;");
        pw.println("\td2rq:belongsToClassMap map:DataSet;");
        pw.println("\td2rq:property bsc:related_to;");
        pw.println(mapping.getColumnPrefix(subjEntity));
        pw.println("\t.");

    }
}

