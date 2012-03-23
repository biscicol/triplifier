package rest;

public enum DBsystem {
	sqlite ("org.sqlite.JDBC"), 
	mysql ("com.mysql.jdbc.Driver"), 
	postgresql ("org.postgresql.Driver"), 
	oracle ("oracle.jdbc.OracleDriver"), 
	sqlserver ("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	
	final String driver;
	
	DBsystem(String driver) {
		this.driver = driver;
	}

}
