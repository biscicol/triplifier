package rest;

import java.util.Set;

/**
 * Represents RDF class in a vocabulary, can have sub-classes.
 */
public class RDFClass extends VocabularyItem {

	public Set<RDFClass> subClasses;
	
	public RDFClass(String name, String uri, Set<RDFClass> subClasses) {
		super(name, uri);
		this.subClasses = subClasses;
	}

}
