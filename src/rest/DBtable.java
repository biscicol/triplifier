package rest;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
class DBtable {
	@XmlElement	String name;
	@XmlElement	Set<String> columns;
	@XmlElement	Set<String> pkColumns;
		
	DBtable(String name, Set<String> columns, Set<String> pkColumns) {
		this.name = name;
		this.columns = columns;
		this.pkColumns = pkColumns;
	}
}
