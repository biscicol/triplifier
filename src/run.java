
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.CSVReader;
import plugins.OpenDocReader;
import plugins.ExcelReader;
import plugins.TabularDataReader;


public class run
{
    private static void runReader(TabularDataReader reader) {
        String[] record;
        
        while (reader.hasNextRow())
        {
            record = reader.getNextRow();
            for (int cnt = 0; cnt < record.length; cnt++)
                System.out.print(cnt > 0 ? ", " + record[cnt] : record[cnt]);
            
            System.out.println();
        }
        
        System.out.println("file extension: " + reader.getFileExtensions()[0]);
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
        runReader(rm.openFile("test.csv", "CSV"));
        
        try {
            TabularDataConverter tdc = new TabularDataConverter(
                    rm.openFile("test.csv"), "jdbc:sqlite:tempdb.sqlite");
        
            tdc.convert();
        } catch (Exception e) {
            throw e;
            //System.out.println(e);
        }
    }
}