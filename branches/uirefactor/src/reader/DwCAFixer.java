package reader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Attempts to "fix" Darwin Core archive data by inferring any missing ID
 * columns.
 */
public class DwCAFixer
{
    // A map for representing which DwC terms match with each conceptID name.
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
        // A string for building SQL queries.
        String query;
        
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
        // in the data source, verify if the appropriate ID column is present,
        // and if not, try to create it.
        for (String conceptID : dwcterms.keySet()) {
            //System.out.println(conceptID);
            
            // Go through all terms for the current conceptID and see which, if
            // any, are present in the source data.
            includedterms.clear();
            for (String term : dwcterms.get(conceptID)) {
                //System.out.println("  " + term);
                if (colnames.contains(term)) {
                    //System.out.println("  " + term);
                    includedterms.add(term);
                }
            }
            
            // Check if we found terms for the current conceptID and if there is
            // not already an ID column for it.
            if (!includedterms.isEmpty() && !colnames.contains(conceptID)) {
                System.out.println("Fixing missing \"" + conceptID + "\" column.");
                
                // Add an ID column to the table for the current conceptID.
                query = "ALTER TABLE \"" + tablename + "\" ADD COLUMN '"
                        + conceptID + "'";
                stmt.executeUpdate(query);
                
                // Create a temporary table to select all distinct values into.
                query = "CREATE TEMPORARY TABLE 'tmp_distinct' "
                        + "(id INTEGER PRIMARY KEY";
                for (String term : includedterms) {
                    query += ", '" + term + "'";
                }
                query += ")";
                //System.out.println(query);
                stmt.executeUpdate(query);
                
                // Populate the temporary table with all distinct combinations
                // of the concept term values and generate integer IDs for each
                // distinct "instance."
                int cnt = 0;
                String collist = "";
                for (String term : includedterms) {
                    if (cnt > 0)
                        collist += ", ";
                    collist += "\"" + term + "\"";
                    cnt++;
                }
                query = "INSERT INTO \"tmp_distinct\" (" + collist +
                        ") SELECT DISTINCT " + collist
                        + " FROM \"" + tablename + "\"";
                //System.out.println(query);
                stmt.executeUpdate(query);
                
                // Finally, copy the ID numbers to the appropriate ID column of
                // the matching rows in the source data table.
                String subquery = "SELECT id FROM \"tmp_distinct\" WHERE ";
                cnt = 0;
                for (String term : includedterms) {
                    if (cnt > 0)
                        subquery += " AND ";
                    subquery += "\"tmp_distinct\".\"" + term + "\"="
                            + "\"" + tablename + "\".\"" + term + "\"";
                    cnt++;
                }
                query = "UPDATE \"" + tablename + "\" SET \""
                        + conceptID + "\" = (" + subquery + ")";
                //System.out.println(query);
                stmt.executeUpdate(query);
                
                // Delete the temporary table.
                stmt.executeUpdate("DROP TABLE \"tmp_distinct\"");
            }
        }
    }
}
