package bcid;

import java.net.URI;

/**
 * The purpose of the BCID class is to identify core identifier metadata fields for creating Biocode Commons Identifiers.
 * This class captures all of the metadata needed to create the identifier before it is passed to the service
 * that assigns the real identifier.
 */
public class bcid implements Identifier {

    public URI webAddress = null;       // URI for the webAddress, EZID calls this _target (e.g. http://biocode.berkeley.edu/specimens/MBIO56)
    public String sourceID = null;      // Source or local identifier (e.g. MBIO056)
    public ResourceType resourceType;   // The ResourceType (e.g. PhysicalObject)
    public String what = null;
    public String when = null;
    public URI doi = null;

    // HEADER to use with row() method
    public static final String HEADER = "URI\tresourceTypeIdentifier\tsourceID\twebAddress";

    /**
     * General constructor for BCIDs
     *
     * @param webAddress
     * @param sourceID
     * @param resourceTypeIdentifier passed in as an integer
     */
    public bcid(URI doi, URI webAddress, String sourceID, int resourceTypeIdentifier) {
        when = new dates().now();
        this.doi = doi;
        this.webAddress = webAddress;
        this.sourceID = sourceID;
        ResourceTypes types = new ResourceTypes();
        this.resourceType = types.get(resourceTypeIdentifier);
        what = this.resourceType.string;
    }

    /**
     * Express this identifier in triple format
     *
     * @return
     */
    public String triplify(URI identifier) {
        String result = "";
        result += "<" + identifier + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#resourceTypeIdentifier> " + "<" + resourceType.uri + ">";
        if (webAddress != null)
            result += ";\n  <http://www.w3.org/ns/ma-ont#isRelatedTo> <" + webAddress + ">";
        if (sourceID != null)
            result += ";\n  <http://purl.org/dc/elements/1.1/identifier> \"" + sourceID + "\"";
        result += " .";
        return result;
    }

    /**
     * Express this identifier as a row.
     *
     * @return
     */
    public String row(URI identifier) {
        return identifier + "\t" + resourceType.string + "\t" + sourceID + "\t" + webAddress ;
    }


    /**
     * TODO: Figure out a way to return the identifier that this is referring to!.. This presents some interesting problems
     * @return
     */
    @Override
    public URI getIdentifier() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

