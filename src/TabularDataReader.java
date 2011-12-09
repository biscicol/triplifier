

public interface TabularDataReader
{
    public String getSourceFormat();
    
    public String getFormatDescription();
    
    public String getFileExtension();
    
    public boolean testFile(String filepath);
    
    public boolean openFile(String filepath);
    
    //public String[] getColNames();
    
    public boolean hasNextRow();
    
    public String[] getNextRow();
    
    public void closeFile();
}
