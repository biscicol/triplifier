package rest;

import java.util.Set;

public class DBtable implements Comparable<DBtable> {
	public String name;
	public Set<String> columns;
	public Set<String> pkColumns;
		
	DBtable(String name, Set<String> columns, Set<String> pkColumns) {
		this.name = name;
		this.columns = columns;
		this.pkColumns = pkColumns;
	}

	@Override
	public int compareTo(DBtable table) {
		return this.name.compareTo(table.name);
	}
}
