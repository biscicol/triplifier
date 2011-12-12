
import java.io.FileInputStream;
import java.io.FileNotFoundException;
//import java.io.IOException;
import java.util.Iterator;
//import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
    Sheet exsheet;
    Iterator<Row> rowiter;
    
    @Override
    public String getSourceFormat() {
        return "Microsoft Excel";
    }

    @Override
    public String getFormatDescription() {
        return "Microsoft Excel 97-2003, 2007+";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] {"xls", "xlsx"};
    }

    @Override
    public boolean testFile(String filepath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean openFile(String filepath) {
        FileInputStream is;
        
        try {
            is = new FileInputStream("test.xls");
        }
        catch (FileNotFoundException e) {
            return false;
        }
        
        try {
            Workbook excelwb = WorkbookFactory.create(is);
            exsheet = excelwb.getSheetAt(0);
            rowiter = exsheet.rowIterator();
        }
        catch (Exception e) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextRow() {
        return rowiter.hasNext();
    }

    @Override
    public String[] getNextRow() {
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
