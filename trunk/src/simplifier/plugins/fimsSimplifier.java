package simplifier.plugins;

import rest.*;
import rest.VocabularyItem;
import java.util.ArrayList;

/**
 * Simplify the FIMS installation, as loaded from a spreadsheet
 */
public class fimsSimplifier extends simplifier {

    public fimsSimplifier(Connection connection) {
        super(connection);
        initializeTerms();
    }

    protected void initializeTerms() {
        // IdentificationProcess
        ArrayList identificationProperties = new ArrayList();
        identificationProperties.add(new columnMap("IdentifiedBy", "dwc:identifiedBy"));
        Entity identification = setEntity(
                "ark:/21547/tempIdentification_",
                new VocabularyItem("identificationProcess", "http://purl.obolibrary.org/obo/bco_0000042"),
                "Specimens",
                "Specimen_Num_Collector",
                null,
                identificationProperties
        );

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

    }
}
