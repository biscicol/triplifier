package simplifier.plugins;

import dbmap.*;
import dbmap.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.*;
import settings.deepRoots;
import vocabulary.VocabularyItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A crude set of properties to simplify DwCA for testing against, VN, Morphbank, etc.
 */
public class dwcSimplifier extends simplifier {
    deepRoots dRoots;
    String depends_on = "<http://biscicol.org/terms/biscicol.owl#depends_on>";
    String related_to = "<http://biscicol.org/terms/biscicol.owl#related_to>";

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
                getPrefix("dwc:Occurrence"),
                new VocabularyItem("dwc:Occurrence", "dwc:Occurrence"),
                "maintable",
                "occurrenceID",
                null,
                specimenProperties);

        // Taxon
        ArrayList taxonProperties = new ArrayList();
        taxonProperties.add(new columnMap("taxonID", "dwc:taxonID"));
        taxonProperties.add(new columnMap("scientificName", "dwc:scientificName"));
        Object[] taxonTableAndId = getProperTableAndId("taxon");
        Entity taxon = setEntity(
                getPrefix("dwc:Taxon"),
                new VocabularyItem("taxon", "dwc:Taxon"),
                taxonTableAndId[0].toString(),
                taxonTableAndId[1].toString(),
                null,
                taxonProperties);

        // Event
        ArrayList eventProperties = new ArrayList();
        eventProperties.add(new columnMap("eventID", "dwc:eventID"));
        eventProperties.add(new columnMap("eventDate", "dwc:eventDate"));
        Object[] eventTableAndId = getProperTableAndId("event");
        Entity event = setEntity(
                getPrefix("dwc:Event"),
                new VocabularyItem("event", "dwc:Event"),
                eventTableAndId[0].toString(),
                eventTableAndId[1].toString(),
                null,
                eventProperties);

        // Identification
        ArrayList identificationProperties = new ArrayList();
        identificationProperties.add(new columnMap("identifiedBy", "dwc:identifiedBy"));
        identificationProperties.add(new columnMap("identificationID", "dwc:identificationID"));
        Object[] identificationTableAndId = getProperTableAndId("identification");
        Entity identification = setEntity(
                getPrefix("dwc:Identification"),
                new VocabularyItem("identification", "dwc:Identification"),
                identificationTableAndId[0].toString(),
                identificationTableAndId[1].toString(),
                null,
                identificationProperties);

        // Location
        ArrayList locationProperties = new ArrayList();
        locationProperties.add(new columnMap("locationID", "dwc:locationID"));
        locationProperties.add(new columnMap("decimalLatitude", "dwc:decimalLatitude"));
        locationProperties.add(new columnMap("decimalLongitude", "dwc:decimalLongitude"));
        locationProperties.add(new columnMap("verbatimCoordinates", "dwc:verbatimCoordinates"));
        Object[] locationTableAndId = getProperTableAndId("location");
        Entity location = setEntity(
                getPrefix("dcterms:Location"),
                new VocabularyItem("location", "http://purl.org/dc/terms/Location"),
                locationTableAndId[0].toString(),
                locationTableAndId[1].toString(),
                null,
                locationProperties);

        // GeologicalContext
        ArrayList geologicalContextProperties = new ArrayList();
        Object[] geologicalContextTableAndId = getProperTableAndId("geologicalContext");
        Entity geologicalcontext = setEntity(
                getPrefix("dwc:GeologicalContext"),
                new VocabularyItem("geologicalContext", "dwc:GeologicalContext"),
                geologicalContextTableAndId[0].toString(),
                geologicalContextTableAndId[1].toString(),
                null,
                geologicalContextProperties);


        // Only set joins if we need them
        if (!eventTableAndId[0].toString().equals("maintable"))
            setJoin("eventID", "maintable", "id", "event");
        if (!locationTableAndId[0].toString().equals("maintable"))
            setJoin("locationID", "maintable", "id", "location");
        if (!taxonTableAndId[0].toString().equals("maintable"))
            setJoin("taxonID", "maintable", "id", "taxon");
        if (!identificationTableAndId[0].toString().equals("maintable"))
            setJoin("identificationID", "maintable", "id", "identification");
        if (!identificationTableAndId[0].toString().equals("maintable"))
            setJoin("geologicalcontextID", "maintable", "id", "geologicalcontext");

        setRelation(identification, depends_on, occurrence);
        setRelation(identification, depends_on, taxon);
        setRelation(event, related_to, location);
        setRelation(occurrence, depends_on, event);
        setRelation(occurrence, depends_on, location);
        setRelation(occurrence, depends_on, geologicalcontext);
        setRelation(event, related_to, geologicalcontext);
        setRelation(occurrence, related_to, taxon);

        setDataseturi("commandLineSimplifierOutput.txt");
    }

    public dwcSimplifier(Connection connection, boolean addPrefix, String url) throws Exception {
        super(connection, addPrefix, url);
        initializeTerms();
    }

    /**
     * Populate the tablename and ID field by figuring out if this is an ID column in the maintable or if it
     * has already been normalized as a DwC extension
     * @param tableName
     * @return
     */
    protected Object[] getProperTableAndId(String tableName) {
        ArrayList tableAndId = new ArrayList();
        String maintable = "maintable";
        List<de.fuberlin.wiwiss.d2rq.algebra.Attribute> columns = schemaInspector.listColumns(new RelationName(null, maintable));
        Iterator it = columns.iterator();
        while (it.hasNext()) {
            de.fuberlin.wiwiss.d2rq.algebra.Attribute a = (de.fuberlin.wiwiss.d2rq.algebra.Attribute) it.next();
            if (a.attributeName().equals(tableName + "ID")) {
                tableAndId.add(maintable);
                tableAndId.add(tableName + "ID");
                return tableAndId.toArray();
            }

        }
        tableAndId.add(tableName);
        tableAndId.add("id");
        return tableAndId.toArray();
    }
}
