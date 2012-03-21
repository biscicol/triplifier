package reader.plugins;

import java.io.File;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.ConceptTerm;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;


/**
 * A reader plugin for Darwin Core Archives.  This plugin supports "zipped"
 * multi-file archives, uncompressed multi-file archives (i.e., as a directory),
 * and single-file archives.  Each data file in the archive will be processed as
 * long as it is included in the archive's meta.xml.  For column names, the
 * proper Darwin Core terms, as mapped in meta.xml, are used.  If an ID field is
 * indicated in meta.xml, the column name "ID" will be used for the core table,
 * and "CORE_ID" is used for any "extension" tables.
 */
public class DWCAReader implements TabularDataReader {
    // iterator for records within a table (ArchiveFile)
    private Iterator<Record> rec_iter;
    // iterator for extension tables in an archive
    private Iterator<ArchiveFile> ext_iter;
    // the field (column) names in a table
    private Set<ConceptTerm> fields;
    // the entire archive
    private Archive dwcarchive;
    // the currently active table
    private ArchiveFile currfile;
    // temporary directory for uncompressing archive files
    private File tmpdir = null;
    // indicates if an id field was specified for the active table
    private boolean has_id;
    // keep track of how many tables have been processed
    private int tablecnt;
    // keep track of how many records in the active table have been processed
    private int reccnt;

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
        return filepath.endsWith(".zip");
    }
    
    /**
     * Create a temporary directory for decompressing a DwC archive.  The
     * directory will be created in the system-default temporary directory.
     * 
     * @return A File object representing the new temporary directory.
     * 
     * @throws IOException 
     */
    private void setTempDir() throws IOException {
        // The method below does not require the guava library, but is not
        // thread safe.
        /* tmpdir = File.createTempFile("DwCA_tmp", "");
        tmpdir.delete();
        tmpdir.mkdir(); */
        
        // This should be thread safe.
        tmpdir = com.google.common.io.Files.createTempDir();
        
        //System.out.println(tmpdir.getAbsolutePath());
    }
    
    @Override
    public boolean testFile(String filepath) {
        File archive = new File(filepath);

        // Oddly, ArchiveFactory will open an Excel 97-2003 file without
        // throwing an exception, even though reading the file results in
        // garbage.  To deal with this, check the extension of the file to make
        // sure it is not "xls".
        if (filepath.endsWith(".xls"))
        	return false;
        
        try {
            if (isZippedArchive(filepath)) {
                setTempDir();
                ArchiveFactory.openArchive(archive, tmpdir);
            }
            else
                ArchiveFactory.openArchive(archive);
            
        } catch (Exception e) {
            return false;
        } finally {
        	closeFile();
        }
        
        return true;
    }

    @Override
    public boolean openFile(String filepath) {
        File archive = new File(filepath);
        
        try {
            if (isZippedArchive(filepath)) {
                setTempDir();
                dwcarchive = ArchiveFactory.openArchive(archive, tmpdir);
            }
            else
                dwcarchive = ArchiveFactory.openArchive(archive);
            
            ext_iter = dwcarchive.getExtensions().iterator();
            tablecnt = 0;
            currfile = null;
            
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        
        return true;
    }

    @Override
    public boolean hasNextTable() {
        // If there are tables left to process, then either the core table has
        // not been processed yet (tablecnt == 0) or there are extension tables
        // waiting for processing.
        return (tablecnt == 0) || ext_iter.hasNext();
    }

    @Override
    public void moveToNextTable() {
        if (hasNextTable()) {
            if (tablecnt == 0)
                // the core table is the first table to process
                currfile = dwcarchive.getCore();
            else
                // get the next extension table
                currfile = ext_iter.next();
            
            fields = currfile.getFields().keySet();
            rec_iter = currfile.iterator();
            has_id = currfile.getId() != null;
            reccnt = 0;
            tablecnt++;
        }
        else
            throw new NoSuchElementException();
    }

    @Override
    public String getCurrentTableName() {
        return currfile.getLocationFile().getName();
    }

    @Override
    public boolean tableHasNextRow() {
        if (currfile != null)
            return (reccnt == 0) || rec_iter.hasNext();
        else
            return false;
    }

    @Override
    public String[] tableGetNextRow() {
        String[] row;
        
        if (has_id)
            row = new String[fields.size() + 1];
        else
            row = new String[fields.size()];
            
        int fieldcnt = 0;
        
        if (!tableHasNextRow())
            throw new NoSuchElementException();
 
        if (reccnt == 0) {
            // This is the first row of the table, so generate a header row
            // with all DwC terms used by this table.
            
            // add an ID column if the table has one
            if (has_id) {
                if (tablecnt == 1)
                    row[fieldcnt++] = "ID";
                else
                    row[fieldcnt++] = "CORE_ID";
            }
            
            for (ConceptTerm term : fields) {
                row[fieldcnt] = term.simpleName();
                fieldcnt++;
            }
        } else {
            // Return the next data row.
            
            Record rec = rec_iter.next();

            // add the ID value if the table has one
            if (has_id)
                row[fieldcnt++] = rec.id();
            
            for (ConceptTerm term : fields) {
                row[fieldcnt] = rec.value(term);
                
                if (row[fieldcnt] == null)
                    row[fieldcnt] = "";
                
                fieldcnt++;
            }
        }

        reccnt++;
        
        return row;
    }

    /**
     * Deletes a directory and all of its contents.  This method assumes that
     * the directory only contains files, not nested directories.
     * 
     * @param dir The directory to remove.
     */
    private void removeDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                // first, remove all files in the directory
                for (File afile : dir.listFiles())
                    afile.delete();
                
                // delete the directory
                dir.delete();
            }
        }
    }
    
    @Override
    public void closeFile() {
        // If a temporary directory was used to uncompress a DwCA, delete it.
        removeDir(tmpdir);
    }
}
