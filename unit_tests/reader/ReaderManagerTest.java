package reader;

import java.io.FileNotFoundException;
import java.util.HashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import reader.plugins.TabularDataReader;


public class ReaderManagerTest {
    // the ReaderManager to test
    private ReaderManager rm;
    
    // a hashset for keeping a list of the default reader types
    private HashSet<String> rformats;
    
    // Set this to the location of the test data.
    private String testdatadir = "testdata";
            
    @Before
    public void setUp() {
        // Create a new ReaderManager.
        rm = new ReaderManager();
        
        // Initialize it by loading all reader plugins.
        try {
            rm.loadReaders();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        
        // Specify the default reader plugin types.
        rformats = new HashSet<String>();
        rformats.add("ODF");
        rformats.add("DwCA");
        rformats.add("CSV");
        rformats.add("EXCEL");
    }
    
    /**
     * Tests LoadReaders method of ReaderManager.  This method will verify that
     * the default reader plugins (as specified in the rformats set) are
     * recognized and loaded correctly by the ReaderManager.  Additional plugins
     * can be added to the plugins directory and this test will still pass as
     * long as the core plugins are present.
     */
    @Test
    public void testLoadReaders() throws Exception {
        // Make sure that at least the default reader plugins were loaded
        // successfully.
        for (TabularDataReader reader : rm) {
            if (rformats.contains(reader.getFormatString()))
                rformats.remove(reader.getFormatString());
        }
        
        assertTrue(rformats.isEmpty());
    }

    /**
     * Tests getSupportedFormats method of ReaderManager.  The logic of this
     * test is similar to that of testLoadReaders(): we just verify that the
     * core formats are supported, and if additional plugins are also present,
     * the test will still pass.
     */
    @Test
    public void testGetSupportedFormats() {
        // Similar to the previous test, here we just verify that at least the
        // default reader formats are returned.
        for (String format : rm.getSupportedFormats()) {
            if (rformats.contains(format))
                rformats.remove(format);
        }
        
        assertTrue(rformats.isEmpty());
    }

    /**
     * Tests getReader method of ReaderManager.
     */
    @Test
    public void testGetReader() {
        TabularDataReader reader;
        
        // Verify that requests for specific readers return the correct plugins.
        for (String rformat : rformats) {
            reader = rm.getReader(rformat);
            
            assertEquals(rformat, reader.getFormatString());
        }
    }

    /**
     * Tests openFile(filepath) method of ReaderManager.  Verifies that each
     * type of supported file is detected correctly and that an appropriate
     * reader plugin is initialized to read the file.
     */
    @Test
    public void testOpenFile_String() {
        TabularDataReader reader;

        // Test each test file to verify that it is opened properly.
        reader = rm.openFile(testdatadir + "/test.csv");
        assertEquals("CSV", reader.getFormatString());
        
        reader = rm.openFile(testdatadir + "/test.ods");
        assertEquals("ODF", reader.getFormatString());
        
        reader = rm.openFile(testdatadir + "/test.xls");
        assertEquals("EXCEL", reader.getFormatString());
        
        reader = rm.openFile(testdatadir + "/test.xlsx");
        assertEquals("EXCEL", reader.getFormatString());
        
        reader = rm.openFile(testdatadir + "/test-dwca");
        assertEquals("DwCA", reader.getFormatString());
        
        reader = rm.openFile(testdatadir + "/test.zip");
        assertEquals("DwCA", reader.getFormatString());
    }

    /**
     * Tests openFile(filepath, formatstring) method of ReaderManager.
     */
    @Test
    public void testOpenFile_String_String() {
        TabularDataReader reader;
        
        // First try to open a file by specifying the correct format.
        reader = rm.openFile(testdatadir + "/test.csv", "CSV");
        assertEquals("CSV", reader.getFormatString());
        
        // Now, verify that we can force it to use a specific plugin even if
        // the file format is wrong.
        reader = rm.openFile(testdatadir + "/test.ods", "CSV");
        assertEquals("CSV", reader.getFormatString());
    }
}
