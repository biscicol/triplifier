package plugins;

import java.io.File;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.StarRecord;
import org.gbif.utils.file.ClosableIterator;

public class DWCAReader implements TabularDataReader {
    ClosableIterator<StarRecord> starRecordIterator;
    File tmpdir = null;

    @Override
    public String getFormatString() {
        return "DWCA";
    }

    @Override
    public String getShortFormatDesc() {
        return "DWCA";
    }

    @Override
    public String getFormatDescription() {
        return "Darwin Core Archive";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[]{"zip"};
    }

    /**
     * See if the archive is a zipped file.  This is done by simply testing the
     * extension of the file to see if it is ".zip".  Something more robust,
     * such as testing the file with a compression utility, would be better.
     * 
     * @param filepath The path to the DwC archive.
     * 
     * @return True if the archive is compressed, false otherwise.
     */
    private boolean isZippedArchive(String filepath) {
        int index = filepath.lastIndexOf('.');
        
        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);
            
            if (ext.equals("zip"))
                return true;            
        }
        
        return false;
    }
    
    /**
     * Create a temporary directory for decompressing a DwC archive.  The
     * directory will be created in the system-default temporary directory.
     * 
     * @return A File object representing the new temporary directory.
     * 
     * @throws IOException 
     */
    private File getTempDir() throws IOException {
        tmpdir = File.createTempFile("DwCA_tmp", "");

        tmpdir.delete();
        tmpdir.mkdir();
        
        System.out.println(tmpdir.getAbsolutePath());
        
        return tmpdir;
    }
    
    @Override
    public boolean testFile(String filepath) {
        try {
            File archiveFolder = new File(filepath);
            ArchiveFactory.openArchive(archiveFolder).iterator().close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean openFile(String filepath) {
        File archive = new File(filepath);
        
        try {
            if (isZippedArchive(filepath)) {
                tmpdir = getTempDir();
                starRecordIterator = ArchiveFactory.openArchive(archive, tmpdir).iterator();
            }
            else
                starRecordIterator = ArchiveFactory.openArchive(archive).iterator();
        } catch (Exception e) {
            System.out.println(e);
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
        return starRecordIterator.hasNext();
    }

    @Override
    public String[] tableGetNextRow() {
        StarRecord starRecord = starRecordIterator.next();
        String[] row = new String[3];
        row[0] = starRecord.core().id();
        row[1] = "dwc:" + starRecord.core().value(DwcTerm.basisOfRecord);
        row[2] = starRecord.core().value("http://purl.org/dc/terms/modified");
        return row;
    }

    @Override
    public void closeFile() {
        starRecordIterator.close();
    }
}
