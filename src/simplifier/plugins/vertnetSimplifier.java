package simplifier.plugins;

import commander.Connection;
import commander.VocabularyItem;
import commander.*;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

/**
 * A crude set of properties to simplify DwCA for testing against, VN, Morphbank, etc.
 * For now, we're just focusing on the mainTable.
 * Ultimately this will need to be re-coded with more robust logic to handle DwCA as constructed by the simplifier
 * in the Javascript code that Brian Stucky wrote.
 */
public class vertnetSimplifier extends simplifier {

    String project_code;
    public void initializeTerms() throws Exception {

        // Specimens
        ArrayList specimenProperties = new ArrayList();
        specimenProperties.add(new columnMap("institutionCode", "dwc:institutionCode"));
        specimenProperties.add(new columnMap("collectionCode", "dwc:collectionCode"));
        specimenProperties.add(new columnMap("catalogNumber", "dwc:catalogNumber"));
        specimenProperties.add(new columnMap("datasetID", "dwc:datasetID"));
        specimenProperties.add(new columnMap("institutionID", "dwc:institutionID"));
        specimenProperties.add(new columnMap("collectionID", "dwc:collectionID"));
        specimenProperties.add(new columnMap("occurrenceID", "dwc:occurrenceID"));
        specimenProperties.add(new columnMap("associatedSequences", "dwc:associatedSequences"));
        specimenProperties.add(new columnMap("associatedOccurrences", "dwc:associatedOccurrences"));
        specimenProperties.add(new columnMap("otherCatalogNumbers", "dwc:otherCatalogNumbers"));
        specimenProperties.add(new columnMap("recordedBy", "dwc:recordedBy"));


        Entity occurrence = setEntity(
                getBCIDPrefix("Specimen"),
                new VocabularyItem("mainTable", "dwc:Occurrence"),
                "maintable",
                "occurrenceID",
                null,
                specimenProperties);

        // Taxon
        ArrayList taxonProperties = new ArrayList();
        taxonProperties.add(new columnMap("taxonID", "dwc:taxonID"));
        taxonProperties.add(new columnMap("scientificName", "dwc:scientificName"));
        Entity taxon = setEntity(
                getBCIDPrefix("Taxon"),
                new VocabularyItem("taxon", "dwc:Taxon"),
                "taxon",
                "id",
                null,
                taxonProperties);

        // Event
        ArrayList eventProperties = new ArrayList();
        eventProperties.add(new columnMap("eventID", "dwc:eventID"));
        eventProperties.add(new columnMap("eventDate", "dwc:eventDate"));
        Entity event = setEntity(
                getBCIDPrefix("Event"),
                new VocabularyItem("event", "dwc:Event"),
                "event",
                "id",
                null,
                eventProperties);

        // Identification
        ArrayList identificationProperties = new ArrayList();
        identificationProperties.add(new columnMap("identifiedBy", "dwc:identifiedBy"));
        identificationProperties.add(new columnMap("identificationID", "dwc:identificationID"));
        Entity identification = setEntity(
                getBCIDPrefix("Identification"),
                new VocabularyItem("identification", "dwc:Identification"),
                "identification",
                "id",
                null,
                identificationProperties);

        // Location
        ArrayList locationProperties = new ArrayList();
        locationProperties.add(new columnMap("locationID", "dwc:locationID"));
        locationProperties.add(new columnMap("decimalLatitude", "dwc:decimalLatitude"));
        locationProperties.add(new columnMap("decimalLongitude", "dwc:decimalLongitude"));
        locationProperties.add(new columnMap("verbatimCoordinates", "dwc:verbatimCoordinates"));
        Entity location = setEntity(
                getBCIDPrefix("Location"),
                new VocabularyItem("location", "http://purl.org/dc/terms/Location"),
                "location",
                "id",
                null,
                locationProperties);

        setJoin("eventID","maintable","id","event");
        setJoin("locationID","maintable","id","location");
        setJoin("taxonID","maintable","id","taxon");
        setJoin("identificationID","maintable","id","identification");

        setRelation(identification, "<bsc:depends_on>", occurrence);
        setRelation(taxon, "<bsc:depends_on>", identification);
        setRelation(event, "<bsc:related_to>", location);
        setRelation(occurrence, "<bsc:depends_on>", event);
        setRelation(occurrence, "<bsc:depends_on>", location);
        setRelation(occurrence, "<bsc:related_to>", taxon);
    }

    /**
     * Get the BCID Prefix
     * @param type
     * @return
     * @throws IOException
     */
    private String getBCIDPrefix(String type) throws IOException {
         String projectServiceString = "http://biscicol.org/id/projectService/" + project_code + "/" + type;
        // Set a 10 second timeout on this connection
        return Jsoup.connect(projectServiceString).timeout(10000).get().body().html();
    }

    public vertnetSimplifier(Connection connection, boolean addPrefix, String project_code) throws Exception {
        super(connection, addPrefix);
        this.project_code = project_code;

        initializeTerms();
    }

}
