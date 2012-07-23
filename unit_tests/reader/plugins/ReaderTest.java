package reader.plugins;

import java.util.NoSuchElementException;
import static org.junit.Assert.*;


/**
 * A base class for implementing test suites for Triplifier reader plugins.
 * Meant to be subclassed and not run directly.
 */
public class ReaderTest {
    /**
     * Test the data from a reader plugin against an expected set of data and
     * data source table names.  This is intended for use by child classes in
     * order to comprehensively test individual plugins.  The expected data
     * should be provided as a three-dimensional array: each table is given as
     * an array of string arrays (one array for each row in the table), and each
     * table is an element of the "outer" array.  An example follows.
     * 
       String[][][] exp_data = {
          {
              {"header1","header2","header3","header4"},
              {"data1","quoted string","d1"},
              {"data2","another \"quoted\" string","something_else"},
              {"data3","quoted string with a comma (\",\")","last value"},
              {"data4","row with a blank","","not blank"}
          }
       };

     * This example specifies one table with five rows.
     * 
     * @param tdreader A reader to test.
     * @param datafile The path to the source data to test.
     * @param exp_data The expected data.
     * @param exp_tnames The expected table names.
     */
    protected void testReadData(TabularDataReader tdreader, String datafile, String[][][] exp_data, String[] exp_tnames) {        
        // the number of expected tables
        int exp_numtables = exp_data.length;
        
        // make sure these all fail before an input file is opened
        assertFalse(tdreader.hasNextTable());
        assertFalse(tdreader.tableHasNextRow());
        try {
            tdreader.moveToNextTable();
            fail("Expected NoSuchElementException.");
        } catch (NoSuchElementException e) { }
        try {
            tdreader.tableGetNextRow();
            fail("Expected NoSuchElementException.");
        } catch (NoSuchElementException e) { }
        
        // open a test data file
        assertTrue(tdreader.openFile(datafile));
        
        // variables for the expected and retrieved data
        String[] row, exp_row;
        int exp_numrows;
        
        // now verify that we get the expected data from the file
        // loop through each expected table
        for (int tablecnt = 0; tablecnt < exp_numtables; tablecnt++) {
            // verify that a table with data is available to read
            assertTrue(tdreader.hasNextTable());
            tdreader.moveToNextTable();
            assertTrue(tdreader.tableHasNextRow());
        
            // check the table name
            assertEquals(exp_tnames[tablecnt], tdreader.getCurrentTableName());

            // get the number of rows we expect in the current table
            exp_numrows = exp_data[tablecnt].length;
            
            // now check each row in the current table
            for (int rowcnt = 0; rowcnt < exp_numrows; rowcnt++) {
                assertTrue(tdreader.tableHasNextRow());
                row = tdreader.tableGetNextRow();
                
                exp_row = exp_data[tablecnt][rowcnt];
            
                // verify that the expected number of row elements were returned
                assertEquals(exp_row.length, row.length);
            
                // check each row element
                for (int col = 0; col < row.length; col++) {
                    assertEquals(exp_row[col], row[col]);
                }
            }
            
            // make sure there are no data left to read in the current table
            assertFalse(tdreader.tableHasNextRow());
        }
        
        // make sure there are no tables left to read
        assertFalse(tdreader.hasNextTable());
    }
}
