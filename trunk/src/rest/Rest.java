package rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

@Path("/")
public class Rest {
	private static final String sqliteFolder = "sqlite";
	private static final String triplesFolder = "triples";
	@Context private static ServletContext context;
	
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
	static String getTriplesPath() {
		return context.getRealPath(triplesFolder) + File.separator;
	}

	/**
	 * Upload file, convert into sqlite database, create Mapping representation of tabular data. 
	 * 
	 * @param inputStream File to be uploaded.
	 * @param contentDisposition Form-data content disposition header.
	 * @return Mapping representation of tabular data in the file.
	 */
	@POST
	@Path("/upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    public Mapping uploadModel (
            @FormDataParam("file") InputStream inputStream, 
            @FormDataParam("file") FormDataContentDisposition contentDisposition) 
    		throws Exception {
    	String fileName = contentDisposition.getFileName();
    	File sqliteFile = createUniqueFile(fileName, "sqlite", getSqlitePath());
    	if (fileName.endsWith(".sqlite")) 
    		writeFile(inputStream, sqliteFile);
    	else {
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
	 * @param inputStream Source of the file.
	 * @param file File to write to.
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
	 * @param base Base part of the name of the file.
	 * @param ext Extension of the name of the file.
	 * @param folder Folder where the file is created.
	 * @return The new file.
	 */
	private File createUniqueFile(String base, String ext, String folder) {
		File file = new File(folder + base + "." + ext);
		int i = 1;
		while (file.exists())
			file = new File(folder + base + "." + i++ + "." + ext);
		return file;
	}

	@POST
	@Path("/inspect")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Mapping inspect(Connection connection) throws Exception {
		return new Mapping(connection);
	}

	@POST
	@Path("/getMapping")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getMapping(Mapping mapping) throws Exception {
		File mapFile = createUniqueFile("mapping", "n3", getTriplesPath());
		PrintWriter pw = new PrintWriter(mapFile);
		mapping.printD2RQ(pw);
		pw.close();
		return triplesFolder + "/" + mapFile.getName();
	}

	@POST
	@Path("/getTriples")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getTriples(Mapping mapping) throws Exception {
		Model model = new ModelD2RQ(new File(context.getRealPath(getMapping(mapping))).toURI().toString());
		File tripleFile = createUniqueFile("triples", "n3", getTriplesPath());
		FileOutputStream fos = new FileOutputStream(tripleFile);
		model.write(fos, "N3");
		fos.close();
		return triplesFolder + "/" + tripleFile.getName();
	}

}
