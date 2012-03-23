package rest;

import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.map.Database;

public class Connection {
	public DBsystem system;
	public String host;
	public String database;
	public String password;
	public String username;

	Connection() {} // for construction from JSON

	Connection(File sqliteFile) {
		system = DBsystem.sqlite;
		host = sqliteFile.getParent().replace("\\", "/");
		database = sqliteFile.getName();
	}
	
	String getJdbcUrl() {
		switch(system) {
			case mysql:
				return "jdbc:mysql://" + host + "/" + database;
			case postgresql:
				return "jdbc:postgresql://" + host + "/" + database;
			case oracle:
				return "jdbc:oracle:thin:@" + host + ":" + database;
			case sqlserver:
				return "jdbc:sqlserver://" + host + ";databaseName=" + database;
			case sqlite:
				return "jdbc:sqlite:" + host + "/" + database;
		}
		return null;
	}
	
	Database getD2RQdatabase() {
		Database database = new Database(ResourceFactory.createResource());
		database.setJDBCDSN(getJdbcUrl());
		database.setUsername(username);
		database.setPassword(password);
		return database;
}
	void printD2RQ(PrintWriter pw) throws SQLException {
		pw.println("map:database a d2rq:Database;");
		pw.println("\td2rq:jdbcDriver \"" + system.driver + "\";");
		pw.println("\td2rq:jdbcDSN \"" + getJdbcUrl() + "\";");
		if (username != null && !username.isEmpty()) 
			pw.println("\td2rq:username \"" + username + "\";");
		if (password != null && !password.isEmpty()) 
			pw.println("\td2rq:password \"" + password + "\";");
		pw.println("\t.");
	}
	
}
