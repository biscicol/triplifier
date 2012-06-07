package rest;

import java.util.Set;

/**
 * Represents a vocabulary, where classes can be used to describe Entity.rdfClass
 * and properties can be used to describe Attribute.predicate.
 */
public class Vocabulary {
	public String name;
	public String prefix;
	public Set<VocabularyItem> properties;
	public Set<VocabularyItem> classes;

	Vocabulary(String name, String prefix, Set<VocabularyItem> properties, Set<VocabularyItem> classes) {
		this.name = name;
		this.prefix = (prefix != null ? prefix : "");
		this.properties = properties;
		this.classes = classes;
	}
}
