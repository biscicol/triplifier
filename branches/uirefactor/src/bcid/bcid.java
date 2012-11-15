package bcid;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * The purpose of the BCID class is to identify core identifier metadata fields and to provide basic methods
 * for working with identifers.  This is primarily EZIDs (DOIs or ARKs) but may also be urn:UUIDs
 */
abstract class bcid {

    public URI webAddress = null;       // URI for the webAddress, EZID calls this _target (e.g. http://biocode.berkeley.edu/specimens/MBIO56)
    public String sourceID = null;      // Source or local identifier (e.g. MBIO056)
    public URI identifier;              // The Identifier (e.g. doi:10.5072/FK2PZ5CV1)
    public ResourceType resourceType;   // The ResourceType (e.g. PhysicalObject)
    public String publisher = "Biocode Commons";

    // HEADER to use with row() method
    public static final String HEADER = "URI\tresourceTypeIdentifier\tsourceID\twebAddress\tpublisher";

    /**
     * General constructor for BCIDs
     * @param webAddress
     * @param sourceID
     * @param resourceTypeIdentifier passed in as an integer
     */
    public bcid(URI webAddress, String sourceID, int resourceTypeIdentifier)  {
        this.webAddress = webAddress;
        this.sourceID = sourceID;
        ResourceTypes types = new ResourceTypes();
        this.resourceType = types.get(resourceTypeIdentifier);
    }

    /**
     * Express this identifier in triple format
     *
     * @return
     */
    public String triplify() {
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
    public String row() {
        return identifier + "\t" + resourceType.string + "\t" + sourceID + "\t" + webAddress + "\t"+ publisher;
    }

    /**
     * Record all notions of now() for BCIDs in a consistent manner
     * @return
     */
    public String now() {
        SimpleDateFormat formatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ssZ");
        formatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatUTC.format(new Date());
    }

}

