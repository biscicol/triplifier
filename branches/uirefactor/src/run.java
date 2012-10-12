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
            rm.loadReaders();
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
        //runReader(rm.openFile("sampledata/test.xlsx"));
        //runReader(rm.openFile("test.csv", "CSV"));
        //runReader(rm.openFile("test-archive.zip", "DWCA"));
        //runReader(rm.openFile("dwca-hsu_wildlife_mammals.zip", "DWCA"));
        //runReader(rm.openFile("dwca-nysm_mammals.zip", "DWCA"));
        //runReader(rm.openFile("testdata/test-dwca", "DWCA"));
        //runReader(rm.openFile("testdata/CanadensysTest.zip", "DWCA"));
    }
}
