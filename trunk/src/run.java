
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
            System.out.println("Valid " + reader.getSourceFormat() + " file.");
        else
            System.out.println("Not a " + reader.getSourceFormat() + " file.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
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
}
