package simplifier.plugins;

import JenaTools.StringOutputStream;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.SystemARQ;
import com.hp.hpl.jena.util.FileUtils;
import com.sun.jersey.core.util.ThrowHelper;
import dbmap.Mapping;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import org.apache.log4j.Level;
import settings.SettingsManager;

import java.io.*;


/**
 * Based on the original "Rest" Class providing REST services and JSON-Java interaction
 * using Java.  This was forked to this class to provide a general interface for triplifying
 */
public class Triplifier {
    static File outputPath;
    private File mappingFile;

    public Triplifier(File pOutputPath) throws Exception {
        outputPath = pOutputPath;
    }

    /**
     * Get output path, making sure the directory exists (and if not, create it)
     *
     * @return Real path of the triples folder with ending slash.
     */
    public static String getOutputPath() {
        return outputPath.getAbsolutePath() + File.separator;
    }

    public File getMappingFile() {
        return mappingFile;
    }

    /**
     * Create new file in given folder, add incremental number to base if filename already exists.
     *
     * @param fileName Name of the file.
     * @param folder   Folder where the file is created.
     * @return The new file.
     */
    public File createUniqueFile(String fileName, String folder) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1)
            dotIndex = fileName.length();
        String base = fileName.substring(0, dotIndex);
        String ext = fileName.substring(dotIndex);
        File file = new File(folder + fileName);
        int i = 1;
        while (file.exists())
            file = new File(folder + base + "." + i++ + ext);
        return file;
    }

    private String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) throws Exception {
        if (verifyFile)
            mapping.connection.verifyFile();

        mappingFile = createUniqueFile(filenamePrefix + ".mapping.n3", getOutputPath());
        PrintWriter pw = new PrintWriter(mappingFile);
        mapping.printD2RQ(pw);
        pw.close();
        return getOutputPath() + mappingFile.getName();
    }

    /**
     * Access the triplifyDirect class to run D2RQ engine
     * @param filenamePrefix
     * @param mapping
     * @param lang
     * @return
     * @throws Exception
     */
    public String getTriples(String filenamePrefix, Mapping mapping, String lang) throws Exception {
        System.gc();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        triplifyDirect t = new triplifyDirect(
                new File(getMapping(filenamePrefix, mapping, true)),
                createUniqueFile(filenamePrefix + ".triples.txt", getOutputPath()),
                lang,
                sm.retrieveValue("defaultURI", "urn:x-biscicol:"));

        return t.getOutputFile().getAbsoluteFile().toString();
    }
}

