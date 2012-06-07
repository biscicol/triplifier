package rest;

import java.util.Set;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Represents either a class or a property in a vocabulary.
 * Implements Comparable to enable sorting by name.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY) // omit empty (or null) fields in JSON serialization
public class VocabularyItem implements Comparable<VocabularyItem> {
	public String name; 
	public String uri;
	public Set<VocabularyItem> subItems;
	
	VocabularyItem(String name, String uri, Set<VocabularyItem> subItems) {
		this.name = name;
		this.uri = uri;
		this.subItems = subItems;
	}

	@Override
	public int compareTo(VocabularyItem vocabularyItem) {
		return this.name.compareTo(vocabularyItem.name);
	}
}
