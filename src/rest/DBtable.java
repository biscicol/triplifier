package rest;

import java.util.Set;

public class DBtable {
	public String name;
	public Set<String> columns;
	public Set<String> pkColumns;
		
	DBtable(String name, Set<String> columns, Set<String> pkColumns) {
		this.name = name;
		this.columns = columns;
		this.pkColumns = pkColumns;
	}
}
