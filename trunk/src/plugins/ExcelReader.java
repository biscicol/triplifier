package plugins;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.IOException;
import java.util.Iterator;
//import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import java.util.NoSuchElementException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;



/**
 * TabularDataReader for Excel-format spreadsheet files.  Both Excel 97-2003
 * format (*.xls) and Excel XML (*.xlsx) format files are supported.
 * 
 * This class makes the simplifying assumption that all data is contained in the
 * first sheet of the "workbook".
 */
public class ExcelReader implements TabularDataReader
{
    Iterator<Row> rowiter;
    
    @Override
    public String getShortFormatDesc() {
        return "Microsoft Excel";
    }

    @Override
    public String getFormatString() {
        return "EXCEL";
    }
    
    @Override
    public String getFormatDescription() {
        return "Microsoft Excel 97-2003, 2007+";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] {"xls", "xlsx"};
    }

    /** See if the specified file is an Excel file.  As currently implemented,
     * this method simply tests if the file extension is "xls" or "xlsx".  A
     * better approach would be to actually test for a specific "magic number."
     * This method also tests if the file actually exists.
     * 
     * @param filepath The file to test.
     * 
     * @return True if the specified file exists and appears to be an Excel
     * file, false otherwise.
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
            
            if (ext.equals("xls") || ext.equals("xlsx"))
                return true;            
        }
        
        return false;
    }
    
    @Override
    public boolean openFile(String filepath) {
        FileInputStream is;
        
        try {
            is = new FileInputStream(filepath);
        }
        catch (FileNotFoundException e) {
            return false;
        }
        
        try {
            Workbook excelwb = WorkbookFactory.create(is);
            Sheet exsheet = excelwb.getSheetAt(0);
            rowiter = exsheet.rowIterator();
        }
        catch (Exception e) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextTable() {
        return false;
    }
    
    @Override
    public void moveToNextTable() {
        throw new NoSuchElementException();
    }

    @Override
    public String getCurrentTableName() {
        return "table1";
    }

    @Override
    public boolean tableHasNextRow() {
        return rowiter.hasNext();
    }

    @Override
    public String[] tableGetNextRow() {
        Row row = rowiter.next();
        Cell cell;

        String[] ret = new String[row.getLastCellNum()];
        
        // Unfortuantely, we can't use a cell iterator here because, as
        // currently implemented in POI, iterating over cells in a row will
        // silently skip blank cells.
        for (int cnt = 0; cnt < row.getLastCellNum(); cnt++) {
            cell = row.getCell(cnt, Row.CREATE_NULL_AS_BLANK);
            
            // inspect the data type of this cell and act accordingly
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    ret[cnt] = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    ret[cnt] = Double.toString(cell.getNumericCellValue());
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    if (cell.getBooleanCellValue())
                        ret[cnt] = "true";
                    else
                        ret[cnt] = "false";
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    ret[cnt] = cell.getCellFormula();
                    break;
                default:
                    ret[cnt] = "";
            }
        }
        
        return ret;
    }

    @Override
    public void closeFile() {
    }    
}
