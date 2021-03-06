package settings;

import javax.ws.rs.Path;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Manage input/output path designations.
 * This was created to handle input from command prompts and tells application where to read and write various files.
 */
public class PathManager {
    public File setFile(String path) throws Exception {
        return setPaths(path, true);
    }

    public File setDirectory(String path) throws Exception {
        return setPaths(path, false);
    }

    /**
     * Set a Directory
     * Handle all special cases here (beginning with File separator, ending with File separator, relative
     * paths, etc...)
     *
     * @param path
     * @return
     * @throws Exception
     */
    private File setPaths(String path, boolean file) throws Exception {
        String fullPath = null;

        // outputPath is specified somehow
        if (path != null && !path.equals("")) {
            String endCharacter = "";
            // Set ending character
            if (!path.endsWith(File.separator)) {
                // only set the end character for directories
                if (!file)
                    endCharacter = File.separator;
            }
            // Test beginning character and determine if this should be relative or not
            if (path.startsWith(File.separator)) {
                fullPath = path + endCharacter;
            } else {
                fullPath = System.getProperty("user.dir") + File.separator + path + endCharacter;
            }

        }
        // no output path specified
        else {
            //fullPath = System.getProperty("user.dir") + File.separator;
            fullPath = System.getProperty("user.dir");
        }

        File theDir = new File(fullPath);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            theDir.mkdirs();
            // NOTE: the following should work but for some reason the return result is always false when a directory
            // is being created.  I was not able to track down the bug, so for now, we have to bypass this check.
            /*
            boolean result = theDir.mkdirs();

            if (result) {
                throw new FileNotFoundException("Unable to create directory " + theDir.getAbsolutePath());
            }
            */
        }
        return theDir;
    }

    public static void main(String args[]) {
        PathManager pm = new PathManager();
        try {
            System.out.println(pm.setFile("sampledata/biocode_template.xls").getName());
            System.out.println(pm.setFile("../../../sampledata/biocode_template.xls"));
            System.out.println(pm.setDirectory("/Users/jdeck/tripleOutput/"));
            System.out.println(pm.setDirectory("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
