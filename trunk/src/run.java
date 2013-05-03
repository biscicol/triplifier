import java.io.FileNotFoundException;

import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.*;


public class run {
    private static void runReader(TabularDataReader reader) {
        String[] record;

        while (reader.hasNextTable()) {
            reader.moveToNextTable();

            System.out.println("TABLE: " + reader.getCurrentTableName());

            int rowcnt = 1;
            while (reader.tableHasNextRow()) {
                record = reader.tableGetNextRow();
                for (int cnt = 0; cnt < record.length; cnt++) {
                    System.out.print(cnt > 0 ? ", " + record[cnt] : record[cnt]);
                }
                //System.out.print(rowcnt++);
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
        // Sets the path to the test files.
        //String testfilepath = "../triplifiersvn/testdata/";
        String testfilepath = "../triplifiersvn/testdata/vertnet/";
                
        //runReaders();

        // Create the ReaderManager and load the plugins
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

        // Open a file and print the data.
        //runReader(rm.openFile(testfilepath + "test.ods"));
        //System.out.println(Thread.currentThread().getContextClassLoader().getResource("sqlite").getFile());
        //System.out.println(System.getProperty("user.dir"));
        //runReader(rm.openFile(testfilepath + "fishtest.zip"));

        // Try converting an input file to a SQLite database.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            System.out.println(ex);
        }
        //TabularDataReader tdr = rm.openFile(testfilepath + "fishtest.zip");
        TabularDataReader tdr = rm.openFile(testfilepath + "uafmc_fish.zip");
        //TabularDataReader tdr = rm.openFile(testfilepath + "hsu_wildlife_birds.zip");
        TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + testfilepath + "test.sqlite");
        tdc.convert();
        tdr.closeFile();
    }
}
