package simplifier.plugins;

import commander.*;
import java.util.*;

/**
 * A crude set of properties to simplify DwCA for testing against, VN, Morphbank, etc.
 * For now, we're just focusing on the mainTable.
 * Ultimately this will need to be re-coded with more robust logic to handle DwCA as constructed by the simplifier
 * in the Javascript code that Brian Stucky wrote.
 */
public class identifierTestsSimplifier extends simplifier {

    public void initializeTerms() {

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
        String prefix = "";
        if (addPrefix) {
             prefix = "urn:x-biscicol:maintable.occurrenceID_";
        }
        setEntity(
                prefix,
                new VocabularyItem("mainTable", "dwc:Occurrence"),
                "maintable",
                "occurrenceID",
                null,
                specimenProperties);

    }

    public identifierTestsSimplifier(Connection connection, boolean  addPrefix) {
        super(connection, addPrefix);
        initializeTerms();
    }

}
