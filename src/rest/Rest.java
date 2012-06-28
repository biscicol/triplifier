package rest;

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

import org.codehaus.jackson.map.ObjectMapper;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * Provides RESTful web services using Jersey JAX-RS implementation. 
 * Many of the public methods use Jackson Mapper to translate between 
 * JSON (received from/sent to the client) and Java objects.
 * Jersey POJOMappingFeature (entry in web.xml) allows to 
 * achieve this without any special annotations of mapped Java classes,
 * sometimes argument-less constructor is needed. Exception handling
 * is achieved through a custom error page for error 500 (entry in web.xml),
 * this eliminates the need for try-catch blocks.
 */
@Path("/")
public class Rest {
    private static final String sqliteFolder = "sqlite";
    private static final String triplesFolder = "triples";
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
     * Get real path of the vocabularies folder in classes folder.
     *
     * @return Real path of the vocabularies folder with ending slash.
     */
    static String getVocabulariesPath() {
        return Thread.currentThread().getContextClassLoader().getResource(vocabulariesFolder).getFile();
    }

    /**
     * Get real path of the triples folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the triples folder with ending slash.
     */
    static String getTriplesPath() {
        return context.getRealPath(triplesFolder) + File.separator;
    }

    /**
     * Upload file, convert into sqlite database, return Mapping representation of tabular data.
     *
     * @param inputStream        File to be uploaded.
     * @param contentDisposition Form-data content disposition header.
     * @return Mapping representation of tabular data in the file.
     */
    @POST
    @Path("/uploadDataSource")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Mapping uploadDataSource(
            @FormDataParam("file") InputStream inputStream,
            @FormDataParam("file") FormDataContentDisposition contentDisposition)
            throws Exception {
        String fileName = contentDisposition.getFileName();
        File sqliteFile = createUniqueFile(fileName + ".sqlite", getSqlitePath());
        if (fileName.endsWith(".sqlite"))  {
            writeFile(inputStream, sqliteFile);
        } else {
            File tempFile = File.createTempFile("upload", fileName);
            writeFile(inputStream, tempFile);
            ReaderManager rm = new ReaderManager();
            rm.LoadReaders();
            TabularDataReader tdr = rm.openFile(tempFile.getPath());
            TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqliteFile.getPath());
            tdc.convert();
            tdr.closeFile();
        }
        return inspect(new Connection(sqliteFile));
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
     * @param folder Folder where the file is created.
     * @return The new file.
     */
    private File createUniqueFile(String fileName, String folder) {
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
    @POST
    @Path("/inspect")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Mapping inspect(Connection connection) throws Exception {
        return new Mapping(connection);
    }

    /**
     * Translate given Mapping into D2RQ Mapping Language.
     *
     * @param mapping Mapping to translate.
     * @return URL to n3 file with D2RQ Mapping Language representation of given Mapping.
     */
    @POST
    @Path("/getMapping")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getMapping(Mapping mapping) throws Exception {
        File mapFile = createUniqueFile("mapping.n3", getTriplesPath());
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw);
        pw.close();
        return triplesFolder + "/" + mapFile.getName();
    }

    /**
     * Generate RDF triples from given Mapping. 
     * As intermediate step D2RQ Mapping Language is created.
     *
     * @param mapping Mapping to triplify.
     * @return URL to n-triples file generated from given Mapping.
     */
    @POST
    @Path("/getTriples")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getTriples(Mapping mapping) throws Exception {
        System.gc();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        Model model = new ModelD2RQ(FileUtils.toURL(context.getRealPath(getMapping(mapping))),"N3",sm.retrieveValue("defaultURI","urn:x-biscicol:"));

        File tripleFile = createUniqueFile("triples.nt", getTriplesPath());
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langNTriple);
        fos.close();
        return triplesFolder + "/" + tripleFile.getName();
    }

    /**
     * Upload local RDF file, 
     * place the file in vocabularies folder,
     * extract vocabulary from the file. 
     * 
     * @param inputStream File to be uploaded.
     * @param contentDisposition Form-data content disposition header.
     * @return Vocabulary extracted from the uploaded file.
     * @throws Exception 
     */
    @POST
    @Path("/uploadVocabulary")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Vocabulary uploadVocabulary(
            @FormDataParam("file") InputStream inputStream,
            @FormDataParam("file") FormDataContentDisposition contentDisposition) throws Exception {
    	File file = createUniqueFile(contentDisposition.getFileName(), getVocabulariesPath());
        writeFile(inputStream, file);
        return getVocabulary(file.getName());      
    }
    
    /**
     * Upload RDF file from URL,
     * place the file in vocabularies folder,
     * extract vocabulary from the file. 
     * 
     * @param urlString URL of file to be uploaded.
     * @return Vocabulary extracted from the uploaded file.
     * @throws Exception 
     */
    @POST
    @Path("/uploadVocabulary")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Vocabulary uploadVocabulary(@FormParam("url") String urlString) throws Exception {
    	// assume http if no protocol provided
    	if (!urlString.contains("://"))
    		urlString = "http://" + urlString;
    
    	// open URLConnection
    	URLConnection connection = new URL(urlString).openConnection();
        InputStream inputStream = connection.getInputStream();

        // try to read filename from Content-Disposition header
        String fileName = connection.getHeaderField("Content-Disposition");
        if (fileName != null) {
            int start = fileName.indexOf("filename=\"") + 10;
            int end = fileName.indexOf("\"", start);
            fileName = fileName.substring(start, end);
        } 
        // if above fails, try to read filename from URL
        if (fileName == null || fileName.isEmpty())
            fileName = urlString.substring(urlString.lastIndexOf("/") + 1);

        // if above fails, use some default filename
        if (fileName.isEmpty())
            fileName = "upload.rdf";

    	File file = createUniqueFile(fileName, getVocabulariesPath());
        writeFile(inputStream, file);
        return getVocabulary(file.getName());      
    }

    /**
     * Extract vocabulary from given file. 
     * 
     * @param fileName Name of the RDF file in vocabularies folder.
     * @return Vocabulary extracted from the file.
     * @throws Exception 
     */
    @POST
    @Path("/getVocabulary")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Vocabulary getVocabulary(String fileName) throws Exception {
        Vocabulary vocabulary= new RDFReader(fileName).getVocabulary();
        return vocabulary;
    }
    
    /**
     * Return a Map of available RDF files defined in triplifiersettings.props: 
     * "vocabularies", each with its "displayName" property,
     * plus given user vocabulary files if they exist in vocabularies folder. 
     * 
     * @param userVocabularies Names of user vocabulary files.
     * @return Available vocabularies.
     * @throws Exception 
     */
    @POST
    @Path("/getVocabularies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getVocabularies(List<String> userVocabularies) throws Exception {
        Map<String, String> vocabulariesMap = new LinkedHashMap<String, String>();

        
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        for (Entry<String, String> vocabularyEntry : sm.retrieveJsonMap("vocabularies").entrySet())
        	vocabulariesMap.put(vocabularyEntry.getKey(), 
        			sm.retrieveJsonMap(vocabularyEntry.getValue()).get("displayName"));

        String vocabulariesPath = getVocabulariesPath();
    	for (String vocabularyFileName : userVocabularies)
	   		if (new File(vocabulariesPath + vocabularyFileName).exists())
	        	vocabulariesMap.put(vocabularyFileName, vocabularyFileName);

    	return vocabulariesMap;
    }

    /**
     * Download a file with a given filename and content.
     * 
     * @param filename   Name of the file.
     * @param content    Content of the file.
     * @return Response with 'attachment' Content-Disposition header.
     */
    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@FormParam("filename") String filename, @FormParam("content") String content) {
        return Response
        		.ok(content)
        		.header("Content-Disposition", "attachment; filename=" + filename)
        		.build();
    }

}
