package bcid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * The bcidUuid can be used as a substitute for EZIDs only when persistence is not a primary issue for identifier
 * maintenance.  This is useful, for instance, during system testing, when one wishes to quickly compare source data
 * in the BiSciCol system, or a provider wishes to adopt their own ongoing resolution services (not advised).
 */
public class bcidUuid extends bcid implements Identifier {

    /**
     *  NOTE: webAddress and resourceTypeIdentifier are relevant when this data will be stored on server.
     * @param webAddress
     * @param sourceID
     * @param resourceTypeIdentifier passed in as an integer
     */
    public bcidUuid(URI webAddress, String sourceID, int resourceTypeIdentifier) throws URISyntaxException {
        super(webAddress, sourceID, resourceTypeIdentifier);
    }

    public void mint(String prefix) throws URISyntaxException {
        identifier = new URI("urn:uuid:" + UUID.randomUUID());
    }
}
