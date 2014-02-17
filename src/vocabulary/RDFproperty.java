package vocabulary;

import java.util.Set;

/**
 * Represents RDF property in a vocabulary, 
 * can have sub-properties, domain and range.
 */
public class RDFproperty extends VocabularyItem {
	
	public Set<RDFproperty> subProperties;
	public Set<String> domain;
	public Set<String> range;
	
	RDFproperty(String name, String uri, Set<RDFproperty> subProperties, Set<String> domain, Set<String> range) {
		super(name, uri);
		this.subProperties = subProperties;
		this.domain = domain;
		this.range = range;
	}

}
