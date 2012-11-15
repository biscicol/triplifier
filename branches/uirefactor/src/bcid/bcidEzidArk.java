package bcid;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Creates an BCID identifier, calling the EZID system, and using ARKs (resolved by N2T)
 */
public class bcidEzidArk extends bcid implements Identifier {

    EZIDService ezid;

    public String who = null;
    public String what = null;
    public String when = null;

    public bcidEzidArk(EZIDService ezid,
                       URI webAddress,
                       String sourceID,
                       int resourceTypeIdentifier,
                       String who) {
        super(webAddress, sourceID, resourceTypeIdentifier);
        this.ezid = ezid;
        this.who = who;
        when = now();
        what = resourceType.string;
    }

    /**
     * Create EZID in this section
     *
     * @return
     * @throws java.net.URISyntaxException
     */
    public void mint(String shoulder) throws URISyntaxException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_profile", "erc");
        map.put("_target", webAddress.toString());
        map.put("erc.who", who);
        map.put("erc.what", what);
        map.put("erc.who", when);

        // Mint the identifier, passing the above map values to the ID
        try {
            identifier = new URI(ezid.mintIdentifier(shoulder, map));
        } catch (EZIDException e) {
            e.printStackTrace();
            throw new URISyntaxException("trouble minting identier", null);
        }
    }
}
