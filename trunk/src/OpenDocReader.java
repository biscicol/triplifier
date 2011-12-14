
import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;


/**
 * A reader for OpenDocument Format spreadsheets (e.g., OpenOffice,
 * LibreOffice).  Only the first sheet of the document is examined, so all data
 * must be in the first sheet.
 */
public class OpenDocReader implements TabularDataReader
{
    private int numrows, curr_row, numcols;
    private Sheet odsheet;
    
    @Override
    public String getSourceFormat() {
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

    @Override
    public boolean testFile(String filepath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean openFile(String filepath) {
        File filein = new File(filepath);
        
        try {
            SpreadSheet odss = SpreadSheet.createFromFile(filein);
            odsheet = odss.getSheet(0);
            
            numrows = odsheet.getRowCount();
            curr_row = 0;
            numcols = odsheet.getColumnCount();
            
            //System.out.println(numrows);
            //System.out.println(numcols);
        }
        catch (IOException e) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextRow() {
        return curr_row < numrows;
    }

    /**
     * Get the next data-containing row from the document.  The length of the
     * returned array will always be equal to the length of the longest row
     * in the document, even if the current row has fewer data-containing cells.
     * Completely empty rows are ignored, unless the last defined row in the
     * sheet is empty, in which case an array of empty strings is returned.
     * 
     * @return The data from the next row of the spreadsheet.
     */
    @Override
    public String[] getNextRow() {
        MutableCell cell;
        boolean blankrow = true;
        
        String[] ret = new String[numcols];

        // get the next row that actually contains data
        while (blankrow && (curr_row < numrows)) {
            for (int cnt = 0; cnt < numcols; cnt++) {
                cell = odsheet.getCellAt(cnt, curr_row);
                ret[cnt] = cell.getTextValue();
                
                if (!ret[cnt].equals(""))
                    blankrow = false;
            }
        
            curr_row++;
        }
        
        return ret;
    }

    @Override
    public void closeFile() {
    }    
}
