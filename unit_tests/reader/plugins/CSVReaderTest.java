package reader.plugins;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class CSVReaderTest extends ReaderTest {
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
        assertTrue(reader.testFile(testdatadir + "/test.csv"));
        
        // test an invalid file
        assertFalse(reader.testFile(testdatadir + "/test.ods"));
    }

    /**
     * Tests openFile method of CSVReader.
     */
    @Test
    public void testOpenFile() {
        // test a valid file
        assertTrue(reader.openFile(testdatadir + "/test.csv"));
        
        // test a file that does not exist
        assertFalse(reader.openFile(testdatadir + "/nonexistant_file.csv"));
    }

    /**
     * Tests the data reading methods of CSVReader.  Because these methods work
     * together, they are all treated within a single test.
     */
    @Test
    public void testReadData() {
        // specifies the expected data for each table in the test data source
        String[][][] exp_data = {
            {
                {"header1","header2","header3","header4"},
                {"data1","quoted string","d1"},
                {"data2","another \"quoted\" string","something_else"},
                {"data3","quoted string with a comma (\",\")","last value"},
                {"data4","row with a blank","","not blank"}
            }
        };
        
        // the expected table names
        String[] exp_tnames = {"table1"};
        
        testReadData(reader, testdatadir + "/test.csv", exp_data, exp_tnames);
    }    
}
