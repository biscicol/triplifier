package rest;

import java.util.Set;

/**
 * Represents a vocabulary, where classes can be used to describe Entity.rdfClass
 * and properties can be used to describe Attribute.rdfProperty.
 */
public class Vocabulary {
	public String name;
	public Set<RDFproperty> properties;
	public Set<RDFClass> classes;

	Vocabulary(String name, Set<RDFproperty> properties, Set<RDFClass> classes) {
		this.name = name;
		this.properties = properties;
		this.classes = classes;
	}
}