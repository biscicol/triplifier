import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * d2rqtesting class
 */
public class d2rqtest {
    static String gPrefix = "";

    public static void main(String args[]) {
        //runServices();
        johnTestMapping();
        //ukiTest();
    }

    private static void johnTestMapping() {
        //String format = "DWCA";
        //String inputfile = "sampledata/CanadensysTest.zip";
        //String sqliteLocation = "jdbc:sqlite:/tmp/ms_tmp/CanadensysTest.sqlite";
        //String D2RQmappingfile = "file:sampledata/CanadensysTest.n3";
        //String outputfile = "/tmp/ms_tmp/Canadensys.nt";

        String format = "EXCEL";
        String inputfile = "sampledata/biocode_template_short.xls";
        String sqliteLocation = "jdbc:sqlite:/tmp/ms_tmp/biocode_template.sqlite";
        String D2RQmappingfile = "file:sampledata/biocode_template_mapping.n3";
        String outputfile = "/tmp/ms_tmp/biocode_template.nt";


        ReaderManager readerManager = new ReaderManager();
        try {
            readerManager.loadReaders();

            TabularDataReader tdr = readerManager.openFile(inputfile,format);
            TabularDataConverter tdc = new TabularDataConverter(tdr, sqliteLocation);

            tdc.convert();
            tdr.closeFile();

            Model model = new ModelD2RQ(FileUtils.toURL(D2RQmappingfile),FileUtils.langN3, "urn:x-biscicol:");
            FileOutputStream fileOutputStream = new FileOutputStream(outputfile);

            System.out.println("Writing output to " + outputfile);
            model.write(fileOutputStream, FileUtils.langN3);
            fileOutputStream.close();

            printN3(model);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static void printN3(Model m) {
        // Set up the ModelD2RQ using a mapping file
        //Model m = new ModelD2RQ("file:sampledata/biocode_example_mapping.n3");

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

    static void ukiTest() {
        Mapping mapping = new MapParser(FileManager.get().loadModel("sampledata/biocode_example_mapping.n3"), "http://localhost/resource/").parse();

        for (Object o : mapping.classMapResources()) {
            Resource r = (Resource) o;
            ClassMap cm = mapping.classMap(r);
            System.out.println(cm.toString());
            System.out.println("  db: " + cm.database().resource());
            System.out.println("  db: " + PrettyPrinter.toString(cm.database().resource()));
            System.out.println("  rel: " + cm.relation().projections().iterator().next());
            System.out.println("  rel: " + cm.nodeMaker() + " unique: " + cm.nodeMaker().isUnique());

            for (Object oo : cm.getClasses()) {
                Resource rr = (Resource) oo;
                System.out.println("  class: " + PrettyPrinter.toString(rr));
            }
            for (Object oo : cm.getDefinitionLabels()) {
                Literal l = (Literal) oo;
                System.out.println("  defLbl: " + PrettyPrinter.toString(l));
            }
            for (Object oo : cm.propertyBridges()) {
                PropertyBridge pb = (PropertyBridge) oo;
                System.out.println("  pb: " + PrettyPrinter.toString(pb.resource()));
                //System.out.println("  pb: " + pb.relation());//.projections().iterator().next());
                System.out.println("  pb: " + pb.nodeMaker());//projections().iterator().next());
            }
            for (Object oo : cm.compiledPropertyBridges()) {
                TripleRelation tr = (TripleRelation) oo;
                System.out.println("  cpb: " + tr);
            }

        }
    }
}
