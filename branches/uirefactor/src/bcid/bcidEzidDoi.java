package bcid;

import edu.ucsb.nceas.ezid.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Creates an BCID identifier, calling the EZID system, and using DOI (via DataCite)
 */
public class bcidEzidDoi extends bcid implements Identifier {

    EZIDService ezid;
    public String creator = null;
    public String title = "Data Element";
    public String publicationyear = null;

    /**
     *
     * @param ezid  (see superclass)
     * @param webAddress  (see superclass)
     * @param sourceID  (see superclass)
     * @param resourceTypeIdentifier  (see superclass)
     * @param creator  Who submitted this record
     */
    public bcidEzidDoi(EZIDService ezid,
                       URI webAddress,
                       String sourceID,
                       int resourceTypeIdentifier,
                       String creator) throws Exception {
        super(webAddress, sourceID, resourceTypeIdentifier);
        this.ezid = ezid;
        this.creator = creator;
        publicationyear = now();

        if (creator == null || creator.trim().equals("")) throw new Exception("Must supply some Creator");
    }

    /**
     * mint EZID in this section
     *
     * @return
     * @throws java.net.URISyntaxException
     */
    public void mint(String shoulder) throws URISyntaxException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_target", webAddress.toString());
        map.put("_profile", "datacite");
        map.put("datacite.resourcetype", resourceType.string);
        map.put("datacite.publisher", publisher);
        map.put("datacite.creator",creator);
        map.put("datacite.title",title);
        map.put("datacite.publicationyear",publicationyear);


        // Mint the identifier, passing the above map values to the ID
        try {
            identifier = new URI(ezid.mintIdentifier(shoulder, map));
        } catch (Exception e) {
            e.printStackTrace();
            throw new URISyntaxException("trouble minting identier", null);
        }
    }
}