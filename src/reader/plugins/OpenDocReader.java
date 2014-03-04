package reader.plugins;


import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import org.joda.time.DateTime;
import org.jopendocument.dom.ODValueType;
import org.jopendocument.dom.spreadsheet.Cell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;


/**
 * A reader for OpenDocument Format spreadsheets (e.g., OpenOffice,
 * LibreOffice).  Unlike Excel spreadsheets, OpenDocument spreadsheets have real
 * data types for dates and times.  If a date is encountered, the reader will
 * convert it to a standard ISO8601 date/time string (yyyy-MM-ddTHH:mm:ss.SSSZZ).
 * If a time is encountered, the reader will convert it to an ISO 8601 time
 * string (HH:mm:ss.SSSZZ).  Also, the first row in each worksheet is assumed to
 * contain the column headers for the data and determines how many columns are
 * examined for all subsequent rows.
 */
public class OpenDocReader implements TabularDataReader
{
    // the entire spreadsheet file
    private SpreadSheet sprdsheet;
    
    // the active worksheet
    private Sheet odsheet;
    
    // the index of the active worksheet
    private int currsheet;
    
    // Used for tracking the dimensions of the active sheet as well as the
    // current row in the active sheet.  The number of columns in the active
    // worksheet is determined by the first row.
    private int numrows, curr_row, numcols;
    
    private boolean hasnext = false;
    private String[] nextrow;
    
    @Override
    public String getFormatString() {
        return "ODF";
    }
    
    @Override
    public String getShortFormatDesc() {
        return "OpenDocument";
    }

    @Override
    public String getFormatDescription() {
        return "OpenDocument (OpenOffice) spreadsheet";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] {"ods"};
    }

    /** See if the specified file is an OpenDocument spreadsheet file.  As
     * currently implemented, this method simply tests if the file extension is
     * "ods".  A better approach would be to actually test for a specific
     * "magic number."  This method also tests if the file actually exists.
     * 
     * @param filepath The file to test.
     * 
     * @return True if the specified file exists and appears to be an
     * OpenDocument file, false otherwise.
     */
    @Override
    public boolean testFile(String filepath) {
        // test if the file exists
        File file = new File(filepath);
        if (!file.exists())
            return false;
        
        int index = filepath.lastIndexOf('.');
        
        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);
            
            if (ext.equals("ods"))
                return true;            
        }
        
        return false;
    }
    
    @Override
    public boolean openFile(String filepath) {
        File filein = new File(filepath);
        
        try {
            sprdsheet = SpreadSheet.createFromFile(filein);
            currsheet = 0;
            curr_row = numrows = 0;
        }
        catch (IOException e) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextTable() {
        if (sprdsheet == null)
            return false;
        else
            return currsheet < sprdsheet.getSheetCount();
    }
    
    @Override
    public void moveToNextTable() {
        if (hasNextTable()) {
            odsheet = sprdsheet.getSheet(currsheet++);
            
            numrows = odsheet.getRowCount();
            curr_row = 0;
            numcols = 0;
            //System.out.println(numrows);
            testNext();
        }
        else
            throw new NoSuchElementException();
    }

    @Override
    public String getCurrentTableName() {
        return odsheet.getName();
    }

    @Override
    public boolean tableHasNextRow() {
        return hasnext;
    }

    /**
     * Internal method to see if the current sheet has another data row.  This
     * is necessary to avoid returning blank rows at the end of the sheet in
     * certain cases.  If another valid row is found, it will be parsed and
     * assigned to nextrow;
     */
    private void testNext() {
        Cell cell;
        boolean blankrow = true;
        hasnext = false;
        
        // If this is the first row in the sheet, we need to determine how many
        // columns there are.
        if (numcols == 0 && curr_row < numrows) {
            numcols = 0;
            for (int cnt = 0; cnt < odsheet.getColumnCount(); cnt++) {
                cell = odsheet.getCellAt(cnt, curr_row);
                if (!cell.getTextValue().equals(""))
                    numcols++;
                else
                    // Stop looking after the first blank cell.
                    break;
            }
        }
        
        nextrow = new String[numcols];

        // get the next row that actually contains data
        while (blankrow && (curr_row < numrows)) {
            for (int cnt = 0; cnt < numcols; cnt++) {
                cell = odsheet.getCellAt(cnt, curr_row);

                // see if this cell contains a date or time value
                if (cell.getValueType() == ODValueType.TIME) {
                    // Although not stated in the API documentation, getting the
                    // value of a time cell returns a java.util.GregorianCalendar
                    // object.  To prove this, run the line of code below.
                    //System.out.println(cell.getValue().getClass().getName());
                    
                    // convert the time value to an ISO 8601 time string
                    GregorianCalendar cal = (GregorianCalendar)cell.getValue();
                    DateTime date = new DateTime(cal.getTime());
                    nextrow[cnt] = date.toString("HH:mm:ss.SSSZZ");
                }
                else if (cell.getValueType() == ODValueType.DATE) {
                    // Although not stated in the API documentation, getting the
                    // value of a date cell returns a java.util.Date object.
                    // To prove this, run the line of code below.
                    //System.out.println(cell.getValue().getClass().getName());

                    // get the date value and convert it to an ISO 8601 string
                    DateTime date;
                    date = new DateTime(cell.getValue());
                    nextrow[cnt] = date.toString();
                }
                else {
                    nextrow[cnt] = cell.getTextValue();
                }

                if (!nextrow[cnt].equals("")) {
                    blankrow = false;
                }
            }

            curr_row++;
        }

        hasnext = !blankrow;
    }
    
    /**
     * Get the next data-containing row from the current worksheet.  The length
     * of the returned array will always be equal to the number of columns in
     * the sheet, as determined from the first row, even if the current row has
     * fewer data-containing cells.  Completely empty rows are ignored.
     * 
     * @return The data from the next row of the spreadsheet.
     */
    @Override
    public String[] tableGetNextRow() {
        if (!tableHasNextRow())
            throw new NoSuchElementException();
        
        String ret[] = nextrow;
        testNext();
        
        return ret;
    }

    @Override
    public void closeFile() {
    }
}
