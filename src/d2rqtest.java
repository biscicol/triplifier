import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class d2rqtest {
    public static void main(String args[]) {
        // Set up the ModelD2RQ using a mapping file
        Model m = new ModelD2RQ("file:biocode_example_mapping.n3");

        // list the statements in the Model
        StmtIterator iter = m.listStatements();

        // print out the predicate, subject and object of each statement
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();  // get next statement
            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object

            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
                System.out.print(object.toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
            }

            System.out.println(" .");
        }
    }
}
