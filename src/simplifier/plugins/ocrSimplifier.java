package simplifier.plugins;

import rest.Connection;
import rest.Dataseturi;
import rest.Entity;
import rest.VocabularyItem;

import java.util.ArrayList;

/**
 * Simplify the FIMS installation, as loaded from a spreadsheet
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

         //setRelation("table1.regionType", "<bsc:depends_on>", "table1.regionType");
        /*
       // Tissue Attributes
       ArrayList tissueProperties = new ArrayList();
       tissueProperties.add(new columnMap("format_name96", "bsc:plate"));
       tissueProperties.add(new columnMap("well_number96", "bsc:well"));
       Entity tissue = setEntity(
               "ark:/21547/tempTissue_",
               new VocabularyItem("tissue", "http://purl.obolibrary.org/obo/OBI_0100051"),
               "Specimens",
               "Specimen_Num_Collector",
               null,
               tissueProperties
       );

       // Specimen Attributes
       ArrayList specimenProperties = new ArrayList();
       specimenProperties.add(new columnMap("preservative", "bsc:preservative"));
       specimenProperties.add(new columnMap("Host", "dwc:host"));
       specimenProperties.add(new columnMap("relaxant", "bsc:relaxent"));
       Entity specimen = setEntity(
               "ark:/21547/tempSpecimen_",
               new VocabularyItem("specimen", "http://purl.obolibrary.org/obo/OBI_0100051"),
               "Specimens",
               "Specimen_Num_Collector",
               null,
               specimenProperties);

       // Taxon Extra Conditions
       ArrayList taxonExtraConditions = new ArrayList();
       ArrayList taxonProperties = new ArrayList();
       taxonExtraConditions.add("SpecificEpithet");
       taxonExtraConditions.add("Phylum");
       // Taxon Attributes
       taxonProperties.add(new columnMap("SpecificEpithet", "dwc:scientificName"));
       taxonProperties.add(new columnMap("Phylum", "dwc:phylum"));
       Entity taxon = setEntity(
               "ark:/21547/tempTaxon_",
               new VocabularyItem("informationContentEntity", "http://purl.obolibrary.org/obo/IAO_0000030"),
               "Specimens",
               "Specimen_Num_Collector",
               taxonExtraConditions,
               taxonProperties
       );

       // EventProcess
       // TODO: insert Event items....

       setRelation(identification, "<bsc:depends_on>", specimen);
       setRelation(taxon, "<bsc:depends_on>", identification);
       setRelation(tissue, "<bsc:derives_from>", specimen);

       // TODO: join Event Table to Specimens table
       // TODO: relation of "Specimen bsc:depends_on Event"
       // TODO: relation of "Event bsc:related_to Location"
        */
    }
}
