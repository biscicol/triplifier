
public class run
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CSVReader csvr = new CSVReader();
        String[] record;
        
        csvr.openFile("test.csv");
        
        while (csvr.hasNextRow())
        {
            record = csvr.getNextRow();
            for (int cnt = 0; cnt < record.length; cnt++)
                System.out.print(cnt > 0 ? ", " + record[cnt] : record[cnt]);
            
            System.out.println();
        }
        
        if (csvr.testFile("test_file.csv"))
            System.out.println("Valid CSV file.");
        else
            System.out.println("Not a CSV file.");
    }
}
