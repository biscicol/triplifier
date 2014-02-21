package simplifier.plugins;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import dbmap.Mapping;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import settings.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;


/**
 * Based on the original "Rest" Class providing REST services and JSON-Java interaction
 * using Java.  This was forked to this class to provide a general interface for triplifying
 */
public class Triplifier {
    static File outputPath;

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

        File mapFile = createUniqueFile(filenamePrefix + ".mapping.n3", getOutputPath());
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw);
        pw.close();
        return getOutputPath() + mapFile.getName();
    }

    public String getTriples(String filenamePrefix, Mapping mapping, String lang) throws Exception {
        System.gc();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        Model model;

        model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, sm.retrieveValue("defaultURI", "urn:x-biscicol:"));

        String extension = "n3";
        if (lang == null) {
            lang = FileUtils.langNTriple;
        }

        if (lang.equals(FileUtils.langTurtle)) {
            extension = "ttl";
        }

        File tripleFile = createUniqueFile(filenamePrefix + ".triples." + extension, getOutputPath());
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, lang);
        fos.close();
        return getOutputPath() + tripleFile.getName();
    }
}

