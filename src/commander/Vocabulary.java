package commander;

import java.util.Set;

/**
 * Represents a vocabulary, where classes can be used to describe Entity.rdfClass
 * and properties can be used to describe Attribute.rdfProperty.
 */
public class Vocabulary {
	public String name;
	public Set<RDFproperty> properties;
	public Set<RDFclass> classes;

	Vocabulary(String name, Set<RDFproperty> properties, Set<RDFclass> classes) {
		this.name = name;
		this.properties = properties;
		this.classes = classes;
	}
}
