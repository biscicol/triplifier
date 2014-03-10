package reader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Attempts to "fix" Darwin Core archive data by inferring the DwC concepts that
 * are actually present in the data, adding any missing ID columns, and
 * normalizing the data so that each concept is in its own table with instance-
 * level data redundancy removed.  The only exceptions are Identification
 * instances, which are not normalized.  Instead, every Occurrence instance gets
 * its own Identification instance.
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
    
    /**
     * Attempts to "fix" Darwin Core archive data.  This method expects to get
     * an active connection to a SQLite database that contains the data for a
     * single-table DwC archive.  All column names in the archive table are 
     * examined to infer which DwC concepts are present in the data.  Then,
     * fixArchive() identifies all unique concept "instance" values, generates
     * (local) ID numbers for each instance, and assigns these IDs to a new
     * concept ID column in the database table.  The end result is pseudo-
     * normalized data with each concept in its own table.  Note that only
     * single-table archives are currently supported by fixArchive().
     * 
     * @param dbconn A connection to a SQLite database for a DwC archive.
     * @throws SQLException 
     */
    public void fixArchive(Connection dbconn) throws SQLException {
        Statement stmt = dbconn.createStatement();
        // A list for tracking which terms are present in the source data.
        ArrayList<String> includedterms = new ArrayList<String>();
        // A list for keeping track of which columns we should delete from the
        // original table after normalizing the data.
        ArrayList<String> deletecolumns = new ArrayList<String>();
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
        boolean hasIDcolumn, IDcolpopulated;
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
            
            // Check if an ID column already exists for this concept.
            hasIDcolumn = colnames.contains(conceptID);
            IDcolpopulated = false;
            if (hasIDcolumn) {
                // If so, see if there are actually any identifiers present.
                query = "SELECT \"" + conceptID + "\" FROM \"" + tablename + "\""
                        + " WHERE \"" + conceptID + "\" != '' LIMIT 1";
                rs = stmt.executeQuery(query);
                IDcolpopulated = rs.next();
            }
            
            // If we found terms for the current conceptID and there is not
            // already a populated ID column for it, process this concept.
            if (!includedterms.isEmpty() && !(hasIDcolumn && IDcolpopulated)) {
               // System.out.println("Fixing missing or empty \"" + conceptID + "\" column.");
                
                stmt.execute("BEGIN TRANSACTION");
                
                // Add an ID column to the table for the current conceptID,
                // if needed.
                if (!hasIDcolumn) {
                    query = "ALTER TABLE \"" + tablename + "\" ADD COLUMN '"
                            + conceptID + "'";
                    colnames.add(conceptID);
                    stmt.executeUpdate(query);
                }
                
                // We need to handle Identification differently than the other
                // concepts.  Each Occurrence gets its own Identification
                // instance, so we don't normalize the Identification data.
                // For all other concepts, we create a new, normalized table.
                if (conceptID.equals("identificationID")) {
                    createIdentificationIDs(stmt, tablename, includedterms);
                } else {
                    createConceptTable(stmt, conceptID, tablename, includedterms, deletecolumns);
                }
                
                stmt.execute("COMMIT");
            }
        }
        
        // Check if the occurrenceID column is already populated.  If not, add
        // it to the list of columns to delete so that we can re-create it as
        // an autoincrement column.
        boolean occurrenceIDpopltd = true;
        if (colnames.contains("occurrenceID")) {
            // See if the occurrenceID column for this table is populated.
            query = "SELECT \"occurrenceID\" FROM \"" + tablename + "\""
                    + " WHERE \"occurrenceID\" != '' LIMIT 1";
            rs = stmt.executeQuery(query);
            occurrenceIDpopltd = rs.next();
            
            // If occurrenceID is not populated, add it to the list of columns
            // to delete.
            if (!occurrenceIDpopltd)
                deletecolumns.add("occurrenceID");
        }
        
        // Delete the columns that are no longer needed.
        // First, get a list of all of the columns we are keeping.
        ArrayList<String> keepcolumns = new ArrayList<String>();
        for (String colname : colnames) {
            if (!deletecolumns.contains(colname))
                keepcolumns.add(colname);
        }
        // Build a formatted list of the column names to use in subsequent
        // queries.
        String savedcollist = "";
        int cnt = 0;
        for (String colname : keepcolumns) {
            if (cnt > 0) {
                savedcollist += ", ";
            }
            savedcollist += "\"" + colname + "\"";
            cnt++;
        }
        String newcollist = savedcollist;
        if (!occurrenceIDpopltd) {
            // If occurrenceID was not populated, create a new auto-increment
            // column for it.
            newcollist = "\"occurrenceID\" INTEGER PRIMARY KEY, " + newcollist;
        }
        
        // Run the queries.
        //System.out.println("Removing unneeded columns from the main table.");
        stmt.execute("BEGIN TRANSACTION");
        
        query = "CREATE TABLE \"" + tablename + "_tmp\"(" + newcollist + ")";
        stmt.executeUpdate(query);
        
        query = "INSERT INTO \"" + tablename + "_tmp\" (" + savedcollist +
                ") SELECT " + savedcollist + " FROM \"" + tablename + "\"";
        stmt.executeUpdate(query);
        
        query = "DROP TABLE \"" + tablename + "\"";
        stmt.executeUpdate(query);
        
        query = "ALTER TABLE \"" + tablename + "_tmp\" RENAME TO " +
                "\"" + "maintable" + "\"";
        //System.out.println(query);
        stmt.executeUpdate(query);
        
        stmt.execute("COMMIT");
        
        stmt.close();
    }
    
    /**
     * Creates a normalized table for the attribute data for a DwC concept.
     * 
     * @param stmt The Statement object to use for running the SQL statements.
     * @param conceptID The name of the conceptID for the DwC concept.
     * @param tablename The name of the DB table for the original data.
     * @param includedterms The DwC terms for this concept that are in the data.
     * @param deletecolumns A list of columns to delete from the original table.
     * @throws SQLException 
     */
    private void createConceptTable(Statement stmt, String conceptID,
            String tablename, ArrayList<String> includedterms,
            ArrayList<String> deletecolumns) throws SQLException {
        // The name of the newly-created table.
        String newtablename;

        // Create a new table to select all distinct values into.
        //System.out.println("USING TEMPORARY TABLE!!!!!!");
        //System.out.println("NOT USING TEMPORARY TABLE!!!!!!");
        newtablename = conceptID.replace("ID", "");
        String query = "CREATE TABLE '" + newtablename + "' "
                + "(id INTEGER PRIMARY KEY";
        for (String term : includedterms) {
            query += ", '" + term + "'";
            // Keep track of the columns we are relocating so that they
            // can be deleted from the main table later.
            deletecolumns.add(term);
        }
        query += ")";
        //System.out.println(query);
        stmt.executeUpdate(query);

        // Populate the new table with all distinct combinations
        // of the concept term values and generate integer IDs for each
        // distinct "instance."
        int cnt = 0;
        String collist = "";
        for (String term : includedterms) {
            if (cnt > 0) {
                collist += ", ";
            }
            collist += "\"" + term + "\"";
            cnt++;
        }
        query = "INSERT INTO \"" + newtablename + "\" (" + collist
                + ") SELECT DISTINCT " + collist
                + " FROM \"" + tablename + "\"";
        //System.out.println(query);
        stmt.executeUpdate(query);

        // Check if there is an empty row in the new table (this will
        // happen if one or more of the original data records contained
        // all blank values for the concept attributes).  If so, delete
        // it from the table to ensure that an empty instance is not
        // created during the translation to RDF.
        query = "DELETE FROM \"" + newtablename + "\" WHERE ";
        cnt = 0;
        for (String term : includedterms) {
            if (cnt > 0) {
                query += " AND ";
            }
            query += "\"" + term + "\"=''";
            cnt++;
        }
        //System.out.println(query);
        stmt.executeUpdate(query);

        // Finally, copy the ID numbers to the appropriate ID column of
        // the matching rows in the source data table.
        String subquery = "SELECT id FROM \"" + newtablename + "\" WHERE ";
        cnt = 0;
        for (String term : includedterms) {
            if (cnt > 0) {
                subquery += " AND ";
            }
            subquery += "\"" + newtablename + "\".\"" + term + "\"="
                    + "\"" + tablename + "\".\"" + term + "\"";
            cnt++;
        }
        query = "UPDATE \"" + tablename + "\" SET \""
                + conceptID + "\" = (" + subquery + ")";
        //System.out.println(query);
        stmt.executeUpdate(query);
    }
    
    /**
     * Creates Identification instance IDs for each Occurrence, provided
     * identification attributes data actually exist for the Occurrence.
     * 
     * @param stmt The Statement object to use for running the SQL statements.
     * @param tablename The name of the DB table for the original data.
     * @param includedterms The DwC terms for this concept that are in the data.
     */
    private void createIdentificationIDs(Statement stmt, String tablename,
            ArrayList<String> includedterms) throws SQLException {
        // Use the internal ROWID as the unique identification instance
        // identifier for each DwC occurrence record.  Only set the identifier
        // for rows that actually have identification data.
        // First, build a logical statement to check if at least one of the
        // included identification attributes has a value.
        String valcheck, query;
        valcheck = "";
        int cnt = 0;
        for (String term : includedterms) {
            if (cnt > 0) {
                valcheck += " OR ";
            }
            valcheck += "\"" + term + "\"!=''";
            cnt++;
        }
        
        // Construct and run the main update statement.  Note that ROWID is
        // explicitly cast to a text value.  Without this, D2RQ chokes on the
        // identificationID values and does not handle them properly.  This
        // makes no sense at all and seems to be a bug in D2RQ.  Casting to a
        // text value was the easiest way I found to work around it.
        query = "UPDATE \"" + tablename +
                "\" SET \"identificationID\" = CAST(ROWID AS TEXT) WHERE " + valcheck;
        //System.out.println(query);
        stmt.executeUpdate(query);
        
        // The next UPDATE statement is another hack to deal with a D2RQ bug.
        // Without explicitly setting the values of empty (NULL) cells to empty
        // strings (''), D2RQ completely crashes when generating the RDF.
        query = "UPDATE \"" + tablename +
                "\" SET \"identificationID\" = '' WHERE \"identificationID\" IS NULL";
        stmt.executeUpdate(query);
    }
}
