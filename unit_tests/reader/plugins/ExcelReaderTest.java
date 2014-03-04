package reader.plugins;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class ExcelReaderTest extends ReaderTest {
    private ExcelReader reader;
    
    @Before
    public void setUp() {
        reader = new ExcelReader();
    }

    /**
     * Tests the format description methods of ExcelReader.  For simplicity, the
     * methods getFormatString(), getShortFormatDesc(), and getFormatDescription()
     * are all treated within a single test.
     */
    @Test
    public void testFormatDescriptions() {
        assertEquals("EXCEL", reader.getFormatString());
        assertEquals("Microsoft Excel", reader.getShortFormatDesc());
        assertEquals("Microsoft Excel 97-2003, 2007+", reader.getFormatDescription());
    }

    /**
     * Tests getFileExtensions method of ExcelReader.
     */
    @Test
    public void testGetFileExtensions() {
        String[] exts = reader.getFileExtensions();
        
        assertEquals(2, exts.length);
        assertEquals("xls", exts[0]);
        assertEquals("xlsx", exts[1]);
    }

    /**
     * Tests testFile method of ExcelReader.
     */
    @Test
    public void testTestFile() {
        // test valid files
        assertTrue(reader.testFile(testdatadir + "/test.xls"));
        assertTrue(reader.testFile(testdatadir + "/test.xlsx"));
        
        // test an invalid file
        assertFalse(reader.testFile(testdatadir + "/test.ods"));
    }

    /**
     * Tests openFile method of ExcelReader.
     */
    @Test
    public void testOpenFile() {
        // test a valid file
        assertTrue(reader.openFile(testdatadir + "/test.xls"));
        
        // test a file that does not exist
        assertFalse(reader.openFile(testdatadir + "/nonexistant_file.xls"));
    }

    /**
     * Tests the data reading methods of ExcelReader.  Because these methods work
     * together, they are all treated within a single test.  Both a .xls and a
     * .xlsx file are tested.
     */
    @Test
    public void testReadData() {
        // specifies the expected data for each table in the test data source
        String[][][] exp_data = {
            {
                {"header1", "header2", "header3", "header4"},
                {"data1", "quoted string", "d1", ""},
                {"data2", "another \"quoted\" string", "something_else", ""},
                {"data3", "quoted string with a comma (\",\")", "last value", ""},
                {"data4", "row with a float and an integer", "1.2346", "256.0"},
                {"data5", "row with a boolean formula", "true", ""},
                {"data6", "row with formulas", "21.0", "string cat"},
                {"data7", "row with a blank", "", "not blank"},
                {"data8", "row with two dates", "2012-01-01T00:00:00.000-07:00", "2012-02-14T02:14:00.000-07:00"}
            },
            {
                {"column 1", "column 2", "last column"},
                {"data 1,1", "data 1,2", "data 1,3"},
                {"data 2,1", "data 2,2", "data 2,3"}
            },
            {
                {"a sheet", "with", "4.0", "columns"},
                {"row 1,col 1", "row 1,col 2", "row 1,col 3", "row 1,col 4"},
                {"row 2,col 1", "row 2,col 2", "row 2,col 3", "row 2,col 4"}
            }
        };
        
        // the expected table names
        String[] exp_tnames = {"Sheet1", "2ndsheet", "2 of 3"};
        
        testReadData(reader, testdatadir + "/test.xls", exp_data, exp_tnames);
        testReadData(reader, testdatadir + "/test.xlsx", exp_data, exp_tnames);
    }    
}
