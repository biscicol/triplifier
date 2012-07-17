package reader.plugins;

import java.util.NoSuchElementException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class CSVReaderTest {
    private CSVReader reader;
    
    @Before
    public void setUp() {
        reader = new CSVReader();
    }

    /**
     * Tests the format description methods of CSVReader.  For simplicity, the
     * methods getFormatString(), getShortFormatDesc(), and getFormatDescription()
     * are all treated within a single test.
     */
    @Test
    public void testFormatDescriptions() {
        assertEquals("CSV", reader.getFormatString());
        assertEquals("CSV", reader.getShortFormatDesc());
        assertEquals("comma-separated values", reader.getFormatDescription());
    }

    /**
     * Tests getFileExtensions method of CSVReader.
     */
    @Test
    public void testGetFileExtensions() {
        String[] exts = reader.getFileExtensions();
        
        assertEquals(1, exts.length);
        assertEquals("csv", exts[0]);
    }

    /**
     * Tests testFile method of CSVReader.
     */
    @Test
    public void testTestFile() {
        // test a valid file
        assertTrue(reader.testFile("testdata/test.csv"));
        
        // test an invalid file
        assertFalse(reader.testFile("testdata/test.ods"));
    }

    /**
     * Tests openFile method of CSVReader.
     */
    @Test
    public void testOpenFile() {
        // test a valid file
        assertTrue(reader.openFile("testdata/test.csv"));
        
        // test a file that does not exist
        assertFalse(reader.openFile("testdata/nonexistant_file.csv"));
    }

    /**
     * Tests the data reading methods of CSVReader.  Because these methods work
     * together, they are all treated within a single test.
     */
    @Test
    public void testReadData() {
        String[][] expvals = {
            {"header1","header2","header3","header4"},
            {"data1","quoted string","d1"},
            {"data2","another \"quoted\" string","something_else"},
            {"data3","quoted string with a comma (\",\")","last value"},
            {"data4","row with a blank","","not blank"}
        };
        
        // make sure these all fail before an input file is opened
        assertFalse(reader.hasNextTable());
        assertFalse(reader.tableHasNextRow());
        try {
            reader.moveToNextTable();
            fail("Expected NoSuchElementException.");
        } catch (NoSuchElementException e) { }
        try {
            reader.tableGetNextRow();
            fail("Expected NoSuchElementException.");
        } catch (NoSuchElementException e) { }
        
        // open a test data file
        assertTrue(reader.openFile("testdata/test.csv"));
        
        // verify that a table with data is available to read
        assertTrue(reader.hasNextTable());
        reader.moveToNextTable();
        assertTrue(reader.tableHasNextRow());
        
        // check the table name
        assertEquals("table1", reader.getCurrentTableName());
        
        // now verify that we get the expected data from the file
        String[] row;
        for (int cnt = 0; cnt < expvals.length; cnt++) {
            assertTrue(reader.tableHasNextRow());
            row = reader.tableGetNextRow();
            
            // verify that the expected number of row elements were returned
            assertEquals(expvals[cnt].length, row.length);
            
            // check each row element
            for (int col = 0; col < row.length; col++) {
                assertEquals(expvals[cnt][col], row[col]);
            }
        }
        
        // make sure there is no data left to read
        assertFalse(reader.tableHasNextRow());
        assertFalse(reader.hasNextTable());
    }
}
