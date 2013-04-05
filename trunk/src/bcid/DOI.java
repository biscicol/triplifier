package bcid;

import edu.ucsb.nceas.ezid.EZIDService;
import rest.SettingsManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Class for working with EZID DOIs (per DataCite Rules)
 * Every BCID that is created must have a DOI.  A DOI is created for every dataset loaded via the
 * triplifier.
 * The ramifications of this is that we will need to find a way to KEEP all triplified data.
 * Partnering with iDigBio or Merritt is a good idea here.
 */
public class DOI implements Identifier {

    // The webAddress of this dataset
    public URI webAddress;
    // Datacite mandatory metadata elementss
    public String creator;
    public String title;
    public String publisher;
    public String publicationyear;
    public String resourcetype;
    // The profile to use-- this is always datacite
    public String profile = "datacite";
    // The default DOI shoulder
    public String shoulder;
    // Where we store the identifier that was created
    private URI identifier;

    public DOI(EZIDService ezid,
               URI webAddress,
               String creator,
               String title,
               String publisher,
               String publicationyear) throws Exception {

        this.webAddress = webAddress;
        this.creator = creator;
        this.title = title;
        this.publisher = publisher;
        this.publicationyear = publicationyear;

        if (webAddress == null) throw new Exception("Must supply some webAddress");
        if (creator == null || creator.trim().equals("")) throw new Exception("Must supply some Creator");
        if (title == null || title.trim().equals("")) throw new Exception("Must supply some Title");
        if (publisher == null || publisher.trim().equals("")) throw new Exception("Must supply some Publisher");
        if (publicationyear == null || publicationyear.trim().equals(""))
            throw new Exception("Must supply some Publication Year");

        // The Resource Type is always the same for us -- DataSet
        ResourceTypes types = new ResourceTypes();
        this.resourcetype = types.get(ResourceTypes.DATASET).string;

        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        this.shoulder = sm.retrieveValue("doiShoulder");

        mintDOI(ezid);
    }

    /**
     * mint the DOI using the EZID system
     *
     * @return
     * @throws java.net.URISyntaxException
     */
    private void mintDOI(EZIDService ezid) throws URISyntaxException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_target", webAddress.toString());
        map.put("_profile", "datacite");
        map.put("datacite.resourcetype", resourcetype);
        map.put("datacite.publisher", publisher);
        map.put("datacite.creator", creator);
        map.put("datacite.title", title);
        map.put("datacite.publicationyear", publicationyear);

        // Mint the identifier, passing the above map values to the ID
        try {
            identifier = new URI(ezid.mintIdentifier(shoulder, map));
        } catch (Exception e) {
            e.printStackTrace();
            throw new URISyntaxException("trouble minting identier", null);
        }
    }

    @Override
    public URI getIdentifier() {
        return identifier;
    }

}