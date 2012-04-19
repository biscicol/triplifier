
import java.io.FileNotFoundException;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.*;


public class run
{
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
        reader.openFile("test.csv");
        
        runReader(reader);
        testFile(reader, "test_file.csv");
        System.out.println();
        
        reader = new ExcelReader();
        reader.openFile("test.xls");
        runReader(reader);
        System.out.println();
        
        reader.openFile("test.xlsx");
        runReader(reader);
        System.out.println();
        
        reader = new OpenDocReader();
        reader.openFile("test.ods");
        runReader(reader);        
    }

    public static void main(String[] args) throws Exception {
        //runReaders();
        
        // create the ReaderManager and load the plugins
        ReaderManager rm = new ReaderManager();
        try {
            rm.LoadReaders();
        }
        catch (FileNotFoundException e) { System.out.println(e); }
        
        // print descriptions for all supported file formats
        for (TabularDataReader reader : rm)
            System.out.println(reader.getFormatDescription() + " ");
        System.out.println();
        
        // open a file and print the data
        //runReader(rm.openFile("/Users/jdeck/bioValidatorSpreadsheets/biocode_fishes.xls"));
        //System.out.println(rm.getReader("DWCA").testFile("test.xls"));
        runReader(rm.openFile("test.xlsx"));
        //runReader(rm.openFile("test.csv", "CSV"));
        //runReader(rm.openFile("test-archive.zip", "DWCA"));
        //runReader(rm.openFile("dwca-hsu_wildlife_mammals.zip", "DWCA"));
        //runReader(rm.openFile("dwca-nysm_mammals.zip", "DWCA"));
        //runReader(rm.openFile("test-dwca", "DWCA"));
        
        /*TabularDataReader reader;
        try {
            reader = rm.openFile("test.xlsx");
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
        }*/
    }
}
