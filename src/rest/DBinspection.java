package rest;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
class DBinspection {
	@XmlElement	String dateTime;
	@XmlElement	Set<DBtable> schema;
	@XmlElement	Set<Mapping> mappings;
	
	DBinspection() {
		dateTime = DateFormat.getDateTimeInstance().format(new Date());
		schema = new HashSet<DBtable>();
		mappings = new HashSet<Mapping>();
	}
}
