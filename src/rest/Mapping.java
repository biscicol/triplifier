package rest;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;

public class Mapping { 
	public String dateTime;
	public Connection connection;
	public Set<DBtable> schema;
	public Set<Join> joins;
	public Set<Entity> entities;
	public Set<Relation> relations;

	Mapping() {} // for construction from JSON

	Mapping(Connection connection) {
		dateTime = DateFormat.getDateTimeInstance().format(new Date());
		this.connection = connection;
		schema = new TreeSet<DBtable>();
		joins = new HashSet<Join>();
		entities = new HashSet<Entity>();
		relations = new HashSet<Relation>();

		Database database = connection.getD2RQdatabase();
		DatabaseSchemaInspector schemaInspector = database.connectedDB().schemaInspector();
//		System.out.println("tables: " + schemaInspector.listTableNames(null));
		DBtable table;
		for (RelationName relationName : schemaInspector.listTableNames(null)) {
			table = new DBtable(relationName.tableName(), new TreeSet<String>(), new HashSet<String>());
			schema.add(table);
			for (Attribute attribute : schemaInspector.listColumns(relationName)) 
				table.columns.add(attribute.attributeName());
			for (Attribute attribute : schemaInspector.primaryKeyColumns(relationName)) 
				table.pkColumns.add(attribute.attributeName());
		}
		database.connectedDB().close();
	}

	void printD2RQ(PrintWriter pw) throws SQLException {
		printPrefixes(pw);
		connection.printD2RQ(pw);
		for (Entity entity : entities)
			entity.printD2RQ(pw);
		for (Relation relation : relations)
			relation.printD2RQ(pw, this);
	}

	Join findJoin(String foreignTable, String primaryTable) {
		for (Join join : joins) 
			if (foreignTable.equals(join.foreignTable) && primaryTable.equals(join.primaryTable)) 
				return join;
		return null;
	}

	Entity findEntity(String table, String idColumn) {
		for (Entity entity : entities) 
			if (table.equals(entity.table) && idColumn.equals(entity.idColumn)) 
				return entity;
		return null;
	}

	private void printPrefixes(PrintWriter pw) {
		pw.println("@prefix map: <" + "" + "> .");
		//	out.println("@prefix db: <" + "" + "> .");
		//	out.println("@prefix vocab: <" + "vocab" + "> .");
		pw.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		pw.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		pw.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		pw.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
		pw.println("@prefix jdbc: <http://d2rq.org/terms/jdbc/> .");
		pw.println("@prefix dwc: <http://rs.tdwg.org/dwc/terms/index.htm#> .");
		pw.println("@prefix bsc: <http://biscicol.org/biscicol.rdf#> .");
		pw.println("@prefix dcterms: <http://purl.org/dc/terms/> .");
		pw.println("@prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> .");
		pw.println();
	}
}
