package plugins;



public interface TabularDataReader
{
    /**
     * Get a short string identifying the file format(s) supported by this
     * reader.  This string can be treated as a constant that is used to request
     * this reader from a ReaderManager, via ReaderManager's getReader() method.
     * 
     * @return A short string that identifies the file format(s) supported by
     * this reader.
     */
    public String getFormatString();
    
    /**
     * Get a short, human-friendly description of the file format(s) supported
     * by this reader.  The value returned by this method should be appropriate
     * for use in dialogs, such as file choosers.
     * 
     * @return A short, human-readable description of the file format(s)
     * supported by this reader.
     */
    public String getShortFormatDesc();
    
    /**
     * Get a human-friendly description of the file format(s) supported by this
     * reader.  This should be a longer, more informative description than the
     * value returned by getFormatString().
     * 
     * @return A human-readable description of the file format(s) supported by
     * this reader.
     */
    public String getFormatDescription();
    
    /**
     * Get the standard file extension(s) for the file formats supported by this
     * TabularDataReader.
     * 
     * @return An array of file extensions (given as Strings) for the file
     * formats supported by this reader.
     */
    public String[] getFileExtensions();
    
    /**
     * Test the specified file to see if it is in a format supported by this
     * TabularDataReader.  If so, return true, otherwise, return false.
     * 
     * @param filepath The path to a source data file.
     * @return True if the file format is supported by this reader, false
     * otherwise.
     */
    public boolean testFile(String filepath);
    
    /**
     * Open the specified file for reading.  Returns true if the file was opened
     * successfully.
     * 
     * @param filepath A file from which to read data.
     * @return True if the file was opened and is ready to read data from; false
     * otherwise.
     */
    public boolean openFile(String filepath);
    
    public boolean hasNextTable();
    public void moveToNextTable();
    public String getCurrentTableName();

    /**
     * Test if there is at least one more row of data waiting to be read from
     * the current table of the opened data source.
     * 
     * @return True if the data source has at least one more row of data to
     * read; false otherwise.
     */
    public boolean tableHasNextRow();
    
    /**
     * Get the next row of data from current table of the data source.  The row
     * is returned as an array of Strings, where each element of the array
     * represents one column in the source data.
     * 
     * @return The next row of data from the data source.
     */
    public String[] tableGetNextRow();
    
    /**
     * Close the open data source, if there is one.
     */
    public void closeFile();
}
