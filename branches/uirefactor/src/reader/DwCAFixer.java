package reader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Attempts to "fix" Darwin Core archive data by inserting any missing ID
 * columns.
 */
public class DwCAFixer
{
    private static HashMap<String, String[]> dwcterms = initializeTerms();
    
    /**
     * Initializes the map of DwC terms to class ID names.
     */
    private static HashMap<String, String[]> initializeTerms() {
        HashMap<String, String[]> terms = new HashMap<String, String[]>();
        
        // Build the map of DwC terms to their associated classes.
        terms.put("eventID", new String[] {"samplingProtocol", "samplingEffort",
            "eventDate", "eventTime", "startDayOfYear", "endDayOfYear", "year",
            "month", "day", "verbatimEventDate", "habitat", "fieldNumber",
            "fieldNotes", "eventRemarks"});
        terms.put("locationID", new String[] {"higherGeographyID",
            "higherGeography", "continent", "waterBody", "islandGroup",
            "island", "country", "countryCode", "stateProvince", "county",
            "municipality", "locality", "verbatimLocality", "verbatimElevation",
            "minimumElevationInMeters", "maximumElevationInMeters",
            "verbatimDepth", "minimumDepthInMeters", "maximumDepthInMeters",
            "minimumDistanceAboveSurfaceInMeters",
            "maximumDistanceAboveSurfaceInMeters", "locationAccordingTo",
            "locationRemarks", "verbatimCoordinates", "verbatimLatitude",
            "verbatimLongitude", "verbatimCoordinateSystem", "verbatimSRS",
            "decimalLatitude", "decimalLongitude", "geodeticDatum",
            "coordinateUncertaintyInMeters", "coordinatePrecision",
            "pointRadiusSpatialFit", "footprintWKT", "footprintSRS",
            "footprintSpatialFit", "georeferencedBy", "georeferencedDate",
            "georeferenceProtocol", "georeferenceSources",
            "georeferenceVerificationStatus", "georeferenceRemarks"});
        terms.put("geologicalContextID", new String[] {
            "earliestEonOrLowestEonothem", "latestEonOrHighestEonothem",
            "earliestEraOrLowestErathem", "latestEraOrHighestErathem",
            "earliestPeriodOrLowestSystem", "latestPeriodOrHighestSystem",
            "earliestEpochOrLowestSeries", "latestEpochOrHighestSeries",
            "earliestAgeOrLowestStage", "latestAgeOrHighestStage",
            "lowestBiostratigraphicZone", "highestBiostratigraphicZone",
            "lithostratigraphicTerms", "group", "formation", "member", "bed"});
        terms.put("identificationID", new String[] {"identifiedBy",
            "dateIdentified", "identificationReferences",
            "identificationVerificationStatus", "identificationRemarks",
            "identificationQualifier", "typeStatus"});
        terms.put("taxonID", new String[] {"scientificNameID",
            "acceptedNameUsageID", "parentNameUsageID", "originalNameUsageID",
            "nameAccordingToID", "namePublishedInID", "taxonConceptID",
            "scientificName", "acceptedNameUsage", "parentNameUsage",
            "originalNameUsage", "nameAccordingTo", "namePublishedIn",
            "namePublishedInYear", "higherClassification", "kingdom", "phylum",
            "class", "order", "family", "genus", "subgenus", "specificEpithet",
            "infraspecificEpithet", "taxonRank", "verbatimTaxonRank",
            "scientificNameAuthorship", "vernacularName", "nomenclaturalCode",
            "taxonomicStatus", "nomenclaturalStatus", "taxonRemarks"});
        
        return terms;
    }
    
    public static void fixArchive(Connection dbconn) throws SQLException {
        Statement stmt = dbconn.createStatement();
        // A list for tracking which terms are present in the source data.
        ArrayList<String> includedterms = new ArrayList<String>();
        
        // Get the table name from the database, and verify that there is only
        // one table.
        ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master " +
                "WHERE type='table'");
        int tablecnt = 0;
        String tablename = "";
        while (rs.next()) {
            tablename = rs.getString(1);
            tablecnt++;
        }
        if (tablecnt != 1)
            return;
        //System.out.println(tablename);
        
        // Get the column names and store them in a list.
        ArrayList<String> colnames = new ArrayList<String>();
        rs = stmt.executeQuery("PRAGMA table_info('" + tablename + "')");
        while (rs.next()) {
            colnames.add(rs.getString("name"));
        }
        //for (String colname : colnames)
        //    System.out.println(colname);
        
        // Now work through each DwC concept.  See if the concept is represented
        // in the data source, verify if the appropriate ID term is present,
        // and if not, try to create it.
        for (String concept : dwcterms.keySet()) {
            //System.out.println(concept);
            
            // Go through all terms for the current concept and see which, if
            // any, are present in the source data.
            includedterms.clear();
            for (String term : dwcterms.get(concept)) {
                //System.out.println("  " + term);
                if (colnames.contains(term)) {
                    //System.out.println("  " + term);
                    includedterms.add(term);
                }
            }
            
            // Check if we found the current concept and there is not already an
            // ID column for it.
            if (!includedterms.isEmpty() && !colnames.contains(concept)) {
                System.out.println("archive contains \"" + concept + "\" with no ID column");
            }
        }
    }
}
