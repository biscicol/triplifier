package rest;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Mapping {
	@XmlElement String table;
	@XmlElement String idColumn;
	@XmlElement(name="class") String type;
	@XmlElement Set<Attribute> attributes;
}
