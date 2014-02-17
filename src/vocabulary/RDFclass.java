package vocabulary;

import java.util.Set;

/**
 * Represents RDF class in a vocabulary, can have sub-classes.
 */
public class RDFclass extends VocabularyItem {

	public Set<RDFclass> subClasses;
	
	public RDFclass(String name, String uri, Set<RDFclass> subClasses) {
		super(name, uri);
		this.subClasses = subClasses;
	}

}
