package simplifier.plugins;

import rest.Connection;
import rest.Dataseturi;
import rest.Entity;
import rest.VocabularyItem;

import java.util.ArrayList;

/**
 * OCR simplifier designed to ingest CSV files as created by Bryan Heidorn's group
 */
public class ocrSimplifier extends simplifier {

    protected String filename;

    public ocrSimplifier(Connection connection, String lFilename) {
        super(connection);
        this.filename = lFilename;
        initializeTerms();
    }

    protected void initializeTerms() {
        // InformationContentEntity
        ArrayList iceProperties = new ArrayList();
        iceProperties.add(new columnMap("catalogNumber", "dwc:catalogNumber"));
        iceProperties.add(new columnMap("recordedBy", "dwc:recordedBy"));
        iceProperties.add(new columnMap("recordNumber", "dwc:recordNumber"));
        iceProperties.add(new columnMap("verbatimEventDate", "dwc:verbatimEventDate"));
        iceProperties.add(new columnMap("verbatimScientificName", "aocr:verbatimScientificName"));
        iceProperties.add(new columnMap("verbatimInstitution", "aocr:verbatimInstitution"));
        iceProperties.add(new columnMap("datasetName", "dwc:datasetName"));
        iceProperties.add(new columnMap("verbatimLocality", "dwc:verbatimLocality"));
        iceProperties.add(new columnMap("country", "dwc:country"));
        iceProperties.add(new columnMap("stateProvince", "dwc:stateProvince"));
        iceProperties.add(new columnMap("county", "dwc:county"));
        iceProperties.add(new columnMap("verbatimLatitude", "dwc:verbatimLatitude"));
        iceProperties.add(new columnMap("verbatimLongitude", "dwc:verbatimLongitude"));
        iceProperties.add(new columnMap("eventDate", "dwc:eventDate"));
        iceProperties.add(new columnMap("scientificName", "dwc:scientificName"));
        iceProperties.add(new columnMap("decimalLatitude", "dwc:decimalLatitude"));
        iceProperties.add(new columnMap("decimalLongitude", "dwc:decimalLongitude"));
        iceProperties.add(new columnMap("fieldNotes", "dwc:fieldNotes"));
        iceProperties.add(new columnMap("sex", "dwc:sex"));
        iceProperties.add(new columnMap("identifiedBy", "dwc:identifiedBy"));
        iceProperties.add(new columnMap("verbatimDateIdentified", "aocr:verbatimDateIdentified"));
        iceProperties.add(new columnMap("dateIdentified", "dwc:dateIdentified"));

        Entity ice = setEntity(
                "urn:x-biscicol:" + filename+":",
                new VocabularyItem("informationContentEntity", "http://purl.obolibrary.org/obo/IAO_0000030"),
                "table1",
                "regionType",
                null,
                iceProperties
        );

        dataseturi = new Dataseturi();
        dataseturi.name = filename;

    }
}
