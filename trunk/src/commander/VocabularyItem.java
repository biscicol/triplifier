package commander;

/**
 * Represents an item in a vocabulary with name and URI only.
 * Implements Comparable to enable sorting by name.
 */
public class VocabularyItem implements Comparable<VocabularyItem> {
	public String name; 
	public String uri;
	
	/**
	 * For construction from JSON.
	 */
	public VocabularyItem() {}

	public VocabularyItem(String name, String uri) {
		this.name = name;
		this.uri = uri;
	}

	public int compareTo(VocabularyItem vocabularyItem) {
		return this.name.compareTo(vocabularyItem.name);
	}
}
