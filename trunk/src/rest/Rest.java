package rest;
import java.util.HashSet;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

@Path("/")
public class Rest {
	@GET
	@Path("/inspect")
	@Produces(MediaType.APPLICATION_JSON)
	public DBinspection inspect() {
		Database database = new Database(ResourceFactory.createResource());
		database.setJDBCDSN("jdbc:sqlite:" + Thread.currentThread().getContextClassLoader().getResource("biocode_example.sqlite").getFile());
		database.setJDBCDriver("org.sqlite.JDBC");
		//database.setUsername(this.databaseUser);
		//database.setPassword(this.databasePassword);
		DatabaseSchemaInspector schemaInspector = database.connectedDB().schemaInspector();
		//System.out.println("tables: " + schemaInspector.listTableNames());
		DBinspection inspection = new DBinspection();
		DBtable table;
		for (Object o : schemaInspector.listTableNames()) {
			RelationName relationName = (RelationName) o;
			table = new DBtable(relationName.tableName(), new HashSet<String>(), new HashSet<String>());
			inspection.schema.add(table);
			for (Object oo : schemaInspector.listColumns(relationName)) {
				Attribute attribute = ((Attribute) oo); 
				table.columns.add(attribute.attributeName());
			}
			for (Object oo : schemaInspector.primaryKeyColumns(relationName)) {
				Attribute attribute = ((Attribute) oo); 
				table.pkColumns.add(attribute.attributeName());
			}
		}
		database.connectedDB().close();
		return inspection;
	}

}
