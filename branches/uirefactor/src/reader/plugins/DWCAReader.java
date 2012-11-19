package reader.plugins;

import java.io.File;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.ConceptTerm;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveField;
import org.gbif.dwc.text.ArchiveFile;


/**
 * A reader plugin for Darwin Core Archives.  This plugin supports "zipped"
 * multi-file archives, uncompressed multi-file archives (i.e., as a directory),
 * and single-file archives.  Each data file in the archive will be processed as
 * long as it is included in the archive's meta.xml.  For column names, the
 * proper Darwin Core terms, as mapped in meta.xml, are used.  If an ID field is
 * indicated in meta.xml, the plugin checks if a proper term name is also
 * provided (via the <field> element).  If not, "ID" is used as the column name
 * for the ID field in the core table, and "CORE_ID" is used for the ID column
 * of any "extension" tables.  ID fields will always be returned as the first
 * column in their table.
 */
public class DWCAReader implements TabularDataReader {
    // iterator for records within a table (ArchiveFile)
    private Iterator<Record> rec_iter;
    // iterator for extension tables in an archive
    private Iterator<ArchiveFile> ext_iter;
    // the field (column) names in a table
    private List<ArchiveField> fields;
    // the entire archive
    private Archive dwcarchive;
    // the currently active table
    private ArchiveFile currfile;
    // temporary directory for uncompressing archive files
    private File tmpdir = null;
    // indicates if an id field was specified for the active table
    private boolean has_id;
    // the index of the ID field
    private int id_index;
    // whether or not a <field> definition was provided for the ID field
    private boolean id_has_field;
    // the term name for the ID field, if provided
    private String id_field_term;
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
        if (filepath.endsWith(".xls") || filepath.endsWith(".xlsx"))
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
            
            fields = currfile.getFieldsSorted();
            rec_iter = currfile.iterator();
            has_id = currfile.getId() != null;
            id_index = -1;
            id_has_field = false;
            id_field_term = "";
            reccnt = 0;
            tablecnt++;
            
            // If this table has an ID, we need to figure out if the ID field
            // type was also provided via a <field> definition.
            if (has_id) {
                id_index = currfile.getId().getIndex();
                
                for (ArchiveField field : fields) {
                    if (field.getIndex() == id_index) {
                        id_has_field = true;
                        id_field_term = field.getTerm().simpleName();
                    }
                }
            }
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

        // If no <field> definition was provided for the ID field, we need to
        // account for this in the row size.
        if (has_id && !id_has_field)
            row = new String[fields.size() + 1];
        else
            row = new String[fields.size()];
            
        int fieldcnt = 0;
        
        if (!tableHasNextRow())
            throw new NoSuchElementException();
 
        if (reccnt == 0) {
            // This is the first row of the table, so generate a header row
            // with all DwC terms used by this table.
            
            // Add the ID field if there is one.
            if (has_id) {
                if (id_has_field)
                    row[fieldcnt++] = id_field_term;
                else {
                    if (tablecnt == 1)
                        row[fieldcnt++] = "ID";
                    else
                        row[fieldcnt++] = "CORE_ID";
                }
            }
            
            for (ArchiveField field : fields) {
                // Ignore the ID field since it was already covered.
                if (id_index != field.getIndex()) {
                    row[fieldcnt] = field.getTerm().simpleName();
                    fieldcnt++;
                }
            }
        } else {
            // Return the next data row.
            
            Record rec = rec_iter.next();

            // Add the ID value if there is one.
            if (has_id)
                row[fieldcnt++] = rec.id();
            
            for (ArchiveField field : fields) {
                // Ignore the ID field since it was already covered.
                if (id_index != field.getIndex()) {
                    row[fieldcnt] = rec.value(field.getTerm());
                
                    if (row[fieldcnt] == null)
                        row[fieldcnt] = "";
                
                    fieldcnt++;
                }
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
