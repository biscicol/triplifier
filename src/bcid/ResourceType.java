package bcid;

/**
 * Components used to describe individual resource types.
 */
public class ResourceType {
    public  String string;
    public  String uri;
    public  String description;

    /**
     *
     * @param string String is the short description (e.g. PhysicalObject, Image, Text)
     * @param uri URI represents the URI that describes this particular resource.
     * @param description Expanded text describing what this refers to.
     */
    public ResourceType(String string, String uri, String description) {
        this.string = string;
        this.uri = uri;
        this.description = description;
    }
}
