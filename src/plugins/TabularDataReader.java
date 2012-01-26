package plugins;



public interface TabularDataReader
{
    public String getFormatString();
    
    public String getShortFormatDesc();
    
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
    
    public boolean openFile(String filepath);
    
    public boolean hasNextRow();
    
    public String[] getNextRow();
    
    public void closeFile();
}
