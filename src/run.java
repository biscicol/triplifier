import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.*;


public class run {
    private static void runReader(TabularDataReader reader) {
        String[] record;

        while (reader.hasNextTable()) {
            reader.moveToNextTable();

            System.out.println("TABLE: " + reader.getCurrentTableName());

            while (reader.tableHasNextRow()) {
                record = reader.tableGetNextRow();
                for (int cnt = 0; cnt < record.length; cnt++) {
                    System.out.print(cnt > 0 ? ", " + record[cnt] : record[cnt]);
                }

                System.out.println();
            }

            //System.out.println();
        }

        System.out.println("file extension: " + reader.getFileExtensions()[0]);

        reader.closeFile();
    }

    private static void testFile(TabularDataReader reader, String filename) {
        if (reader.testFile(filename))
            System.out.println("Valid " + reader.getShortFormatDesc() + " file.");
        else
            System.out.println("Not a " + reader.getShortFormatDesc() + " file.");
    }

    private static void runReaders() {
        TabularDataReader reader = new CSVReader();
        reader.openFile("sampledata/test.csv");

        runReader(reader);
        testFile(reader, "test_file.csv");
        System.out.println();

        reader = new ExcelReader();
        reader.openFile("sampledata/test.xls");
        runReader(reader);
        System.out.println();

        reader.openFile("sampledata/test.xlsx");
        runReader(reader);
        System.out.println();

        reader = new OpenDocReader();
        reader.openFile("sampledata/test.ods");
        runReader(reader);
    }

    public static void main(String[] args) throws Exception {
        //runReaders();

        // create the ReaderManager and load the plugins
        ReaderManager rm = new ReaderManager();
        try {
            rm.LoadReaders();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }

        // print descriptions for all supported file formats
        for (TabularDataReader reader : rm) {
            System.out.println(reader.getFormatDescription() + " ");
            System.out.println(reader.getFormatString());
        }
        System.out.println();

        // open a file and print the data
        //runReader(rm.openFile("/Users/jdeck/bioValidatorSpreadsheets/biocode_fishes.xls"));
        //System.out.println(rm.getReader("DWCA").testFile("test.xls"));
        //System.out.println(Thread.currentThread().getContextClassLoader().getResource("sqlite").getFile());
        //runReader(rm.openFile("sampledata/test.xlsx"));

        TabularDataReader tdr = rm.openFile("sampledata/biocode_template.xls");
         TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:/tmp/triples.sqlite");

        //runReader(rm.openFile("sampledata/test.xlsx"));
        //runReader(rm.openFile("test.csv", "CSV"));
        //runReader(rm.openFile("test-archive.zip", "DWCA"));
        //runReader(rm.openFile("dwca-hsu_wildlife_mammals.zip", "DWCA"));
        //runReader(rm.openFile("dwca-nysm_mammals.zip", "DWCA"));
        //runReader(rm.openFile("test-dwca", "DWCA"));



        // condensed method to test mapping file logic
        // John testing some code here to read the biocode_template.xls file and apply some sample mapping
        // that i'm tweeking on the fly with samplemapping.n3
        /*rm.LoadReaders();
        TabularDataReader tdr = rm.openFile("sampledata/biocode_template.xls");
        TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:/tmp/triples.sqlite");
        tdc.convert();
        tdr.closeFile();
        //
        Model model = new ModelD2RQ(FileUtils.toURL("sampledata/biocode_example_mapping.n3")
                );
        FileOutputStream fos = new FileOutputStream("/tmp/triples.nt");
        model.write(fos, FileUtils.langN3);
        fos.close();*/

        /*
 TabularDataReader reader;
 try {
     reader = rm.openFile("sampledata/test.xlsx");
     //reader = rm.openFile("test.ods");
     //reader = rm.openFile("test-archive.zip");
     //reader = rm.openFile("dwca-nysm_mammals.zip");
     //reader = rm.openFile("test-dwca");
     //rm.openFile("357800_biocode.xls"), "jdbc:sqlite:tempdb.sqlite");
     //rm.openFile("357800_biocode-tmp.xls"), "jdbc:sqlite:tempdb.sqlite");

     TabularDataConverter tdc = new TabularDataConverter(
             reader, "jdbc:sqlite:tempdb.sqlite");

     //tdc.setTableName("collecting_events");
     tdc.convert();

     reader.closeFile();

 } catch (Exception e) {
     throw e;
     //System.out.println(e);
 }       */
    }
}
