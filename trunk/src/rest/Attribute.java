package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Attribute {
	@XmlElement String column;
	@XmlElement String mode;
	@XmlElement String predicate;
	@XmlElement String target;
}
