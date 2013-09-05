package commander;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import settings.SettingsManager;


/**
 * Based on the original "Rest" Class providing REST services and JSON-Java interaction
 * using Java.  This was forked to this class to provide a general interface for triplifying
 */
public class Triplifier {
    public static final String sqliteFolder = "sqlite";
    public static final String triplesFolder = "triples";
    private static final String vocabulariesFolder = "vocabularies";
    @Context
    private static ServletContext context;

    /**
     * Get real path of the sqlite folder in classes folder.
     *
     * @return Real path of the sqlite folder with ending slash.
     */
    static String getSqlitePath() {
        return Thread.currentThread().getContextClassLoader().getResource(sqliteFolder).getFile();
    }

    /**
     * Get real path of the triples folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the triples folder with ending slash.
     */
    public static String getTriplesPath() {
        return System.getProperty("user.dir") + File.separator;
    }


    /**
     * Write InputStream to File.
     *
     * @param inputStream Input to read from.
     * @param file        File to write to.
     */
    private void writeFile(InputStream inputStream, File file) throws Exception {
        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, 1 << 24);
        fos.close();
        System.out.println("received: " + file.getPath());
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

    /**
     * Inspect the database, return Mapping representation of schema.
     *
     * @param connection Database connection.
     * @return Mapping representation of the database schema.
     */
    public Mapping inspect(Connection connection) throws Exception {
        return new Mapping(connection);
    }

    /**
     * Translate given Mapping into D2RQ Mapping Language.
     *
     * @param mapping Mapping to translate.
     * @return URL to n3 file with D2RQ Mapping Language representation of given Mapping.
     */
    public String getMapping(Mapping mapping) throws Exception {
        return getMapping(mapping, false);
    }

    private String getMapping(Mapping mapping, Boolean verifyFile) throws Exception {
        return getMapping("output", mapping, verifyFile);
    }

    private String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) throws Exception {
        if (verifyFile)
            mapping.connection.verifyFile();

        File mapFile = createUniqueFile(filenamePrefix + ".mapping.n3", getTriplesPath());
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw);
        pw.close();
        return getTriplesPath() + mapFile.getName();
    }

    /**
     * Generate RDF triples from given Mapping.
     * As intermediate step D2RQ Mapping Language is created.
     *
     * @param mapping Mapping to triplify.
     * @return URL to n-triples file generated from given Mapping.
     */
    public String getTriples(Mapping mapping) throws Exception {
        return getTriples("output", mapping);
    }

    public String getTriples(String filenamePrefix, Mapping mapping) throws Exception {
        System.gc();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        Model model;
        if (context != null) {
            model = new ModelD2RQ(FileUtils.toURL(context.getRealPath(getMapping(mapping, true))),
                    FileUtils.langN3, sm.retrieveValue("defaultURI", "urn:x-biscicol:"));
        } else {
            model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                    FileUtils.langN3, sm.retrieveValue("defaultURI", "urn:x-biscicol:"));
        }
        File tripleFile = createUniqueFile(filenamePrefix + ".triples.n3", getTriplesPath());
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langNTriple);
        fos.close();
        return getTriplesPath() + tripleFile.getName();
    }


}
