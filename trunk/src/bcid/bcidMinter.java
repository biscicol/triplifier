package bcid;

//import com.modp.checkdigits.CheckDihedral;
//import com.modp.checkdigits.CheckLuhnMod10;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import org.apache.commons.codec.binary.Base64;
import rest.SettingsManager;

import java.awt.image.LookupOp;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Work with identifiers table in mysql to mint BCIDs and create EZID.  "Mint" means that the identifier is
 * created by the application and "create" means that the identifier is created outside the application.  Thus,
 * BCID is "minting" and EZID "creates" the identifiers.
 */
public class bcidMinter {

    // Mysql Connection
    Connection conn;
    // The "who" is the EZID erc.who syntax to display for each identifier.  The link to display here tells us about BCIDs
    public String who = "http://biocodecommons.org/identifiers.html";
    // The default "shoulder" for BCIDs.  Looked up in Properties file
    public String shoulder = null;
    // Make all base64 encoding URL safe -- this constructor will remove equals ("=") as padding
    Base64 base64 = new Base64(true);
    // some number to start with
    // This could be 0 in most cases, but for testing and in cases where we already created a test encoded EZID
    // this can be set manually here to bypass existing EZIDS
    Integer startingNumber = 10;
    LuhnModN luhnModN;

    /**
     * Controls minting of identifiers in conjunction with Mysql database.
     * Constructor is used mainly to initialize database connection
     *
     * @throws Exception
     */
    public bcidMinter() throws Exception {

        // Load settings
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String bcidUser = sm.retrieveValue("bcidUser");
        String bcidPassword = sm.retrieveValue("bcidPassword");
        String bcidUrl = sm.retrieveValue("bcidUrl");
        String bcidClass = sm.retrieveValue("bcidClass");
        shoulder = sm.retrieveValue("bcidShoulder");

        try {
            Class.forName(bcidClass);
            conn = DriverManager.getConnection(bcidUrl, bcidUser, bcidPassword);
        } catch (ClassNotFoundException e) {
            throw new Exception();
        } catch (SQLException e) {
            throw new Exception();
        }

        setAutoIncrement();
        luhnModN = new LuhnModN();
    }

    /**
     * Set the starting AutoIncrement
     */
    private void setAutoIncrement() {
        String alterString = "ALTER TABLE identifiers AUTO_INCREMENT = ?";
        PreparedStatement alterStatement = null;
        try {
            alterStatement = conn.prepareStatement(alterString);
            alterStatement.setInt(1, startingNumber);
            alterStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mint a group of numbers and return identifer names as Names
     * Can be used to add name portion to Ark Identifiers
     *
     * @return
     */
    public void mintList(ArrayList bcidList) throws Exception {

        // Turn off autocommits just for this method
        conn.setAutoCommit(false);
        PreparedStatement insertStatement = null;

        try {
            // Use auto increment in database to assign the actual identifier.. this is threadsafe this way
            // Also, use auto date assignment feature for when this was applied.
            String insertString = "INSERT INTO identifiers " +
                    "(doi, webaddress, localid, what) " +
                    "values (?,?,?,?)";
            insertStatement = conn.prepareStatement(insertString);

            Iterator ids = bcidList.iterator();
            int count = 0;
            while (ids.hasNext()) {
                bcid id = (bcid) ids.next();
                insertStatement.setString(1, id.doi.toString());
                insertStatement.setString(2, id.webAddress.toString());
                insertStatement.setString(3, id.sourceID);
                insertStatement.setString(4, id.resourceType.string);
                insertStatement.addBatch();
                // Execute a commit at every 1000 rows
                if (count + 1 % 1000 == 0) {
                    insertStatement.executeBatch();
                    conn.commit();
                }
                count++;
            }
            // Execute remainder as batch
            insertStatement.executeBatch();
            conn.commit();
        } finally {
            insertStatement.close();
            conn.setAutoCommit(true);
        }
    }

    /**
     * Return the next available start number from the mysql database as a BigInteger
     * Note that this method is probably not needed with the Mysql Auto_Increment
     *
     * @return
     * @throws Exception
     */
    private BigInteger start() throws Exception {
        BigInteger big = null;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select max(id) as maxid from identifiers");

            if (rs.next()) {

                try {
                    big = new BigInteger(rs.getString("maxid"));
                } catch (NullPointerException e) {
                    // In case this is the very first, returning NULL means there are no integers here
                    // start with some number, set by class
                    big = new BigInteger(startingNumber.toString());
                }

            } else {
                throw new Exception();
            }

            // Add 1 to the start value
            return big.add(new BigInteger("1"));

        } catch (SQLException e) {
            throw new Exception("Unable to find start");
        }

    }

    /**
     * Use Base64 encoding to turn BigIntegers numbers into Strings
     * We do this both to obfuscate integers used as identifiers and also to save some space.
     * Base64 is not necessarily the best for compressing numbers but it is well-known
     * The encoding presented here adds a check digit at the end of the string
     *
     * @param big
     * @return
     */
    public String encode(BigInteger big) {
        String strVal = new String(base64.encode(big.toByteArray()));
        strVal = strVal.replace("\r\n", "");
        return luhnModN.encode(strVal);
    }

    /**
     * Base64 decode identifiers into integer representations.  See explanation for encoding
     * for details.
     *
     * @param string
     * @return
     */
    public BigInteger decode(String string) throws Exception {
        // Validate the data
        if (!luhnModN.verify(string)) {
            throw new Exception("String you are attempting to decode does not validate against check digit!");
        }
        // Now check the Actual String, minus check Character
        String actualString = luhnModN.getData(string);

        // Now return the integer that was encoded here.
        return new BigInteger(base64.decode(actualString));
    }

    /**
     * Close the SQL connection
     *
     * @throws SQLException
     */
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Go through identifier table and create any ezid fields that have yet to be created.
     * This method is meant to be called via a cronjob on the backend.
     * TODO: throw a special exception on this method so we can follow up why EZIDs are not being made if that is the case
     *
     * @param ezid
     * @throws URISyntaxException
     */
    public void createEZIDs(EZIDService ezid) throws URISyntaxException {
        // Grab a row where ezid is false
        Statement stmt = null;
        ResultSet rs = null;
        ArrayList<String> idSuccessList = new ArrayList();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT id,webaddress,localid,what FROM identifiers WHERE ezid != true LIMIT 1000");
            // Attempt to create an EZID for this row
            while (rs.next()) {
                URI identifier = null;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("_profile", "erc");
                map.put("_target", rs.getString("webaddress"));
                map.put("erc.what", rs.getString("what"));
                map.put("erc.who", who);
                // when here is very confusing
                //map.put("erc.when", new dates().now());
                String idString = rs.getString("id");
                identifier = new URI(ezid.createIdentifier(getIdentifier(new BigInteger(idString)), map));

                if (identifier != null) {
                    idSuccessList.add(idString);
                    System.out.println("  " + identifier.toString());
                } else {
                    // Send email, or notify somehow in logs that this threw an error
                    System.out.println("Something happened in creating the EZID identifier, it appears to be null");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (EZIDException e) {
            e.printStackTrace();
            throw new URISyntaxException("trouble minting identifier with EZID service", null);
        } finally {
            try {
                updateEZIDTracker(idSuccessList);
            } catch (SQLException e) {
                System.out.println("It appears we have created " + idSuccessList.size() + " EZIDs but not able to update the identifiers table");
                e.printStackTrace();
            }

        }

    }

    /**
     * Update the EZID tracking (boolean) field
     *
     * @param idSuccessList
     * @throws SQLException
     */
    private void updateEZIDTracker(ArrayList idSuccessList) throws SQLException {
        // Turn off autocommits at beginning
        conn.setAutoCommit(false);
        PreparedStatement updateStatement = null;

        try {

            String updateString = "UPDATE identifiers SET ezid=true WHERE id=?";
            updateStatement = conn.prepareStatement(updateString);

            Iterator ids = idSuccessList.iterator();
            int count = 0;
            while (ids.hasNext()) {

                String id = (String) ids.next();
                updateStatement.setString(1, id);
                updateStatement.executeUpdate();
                updateStatement.addBatch();
                // Execute every 1000 rows
                if (count + 1 % 1000 == 0) {
                    updateStatement.executeBatch();
                    conn.commit();
                }
                count++;
            }
            updateStatement.executeBatch();
            conn.commit();

        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Given an BigInteger, return an identifier in its fully qualified form.
     *
     * @param big
     * @return
     */
    public String getIdentifier(BigInteger big) {
        return shoulder + encode(big);
    }

    /**
     * Use main function for testing
     *
     * @param args
     */
    public static void main(String args[]) {
        bcidMinter minter = null;
        try {
            minter = new bcidMinter();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Test check digits directly
        System.out.println("\nTrying out the LunhModN algorithm");
        LuhnModN luhnModN = new LuhnModN();
        String startString = "O5rKAg";
        String checkedString = luhnModN.encode(startString);
        System.out.println(" startString: " + startString + " -> withCheckDigit: " + checkedString);
        if (luhnModN.verify("O5rKAgE")) {
            System.out.println(" " + startString + " validates!");
        } else {
            System.out.println(" " + startString + " does not validate");
        }

        // A simple check to decode a value -- this one throws an exception
        System.out.println("\nTesting the check Digit");
        try {
            System.out.println(minter.decode("05rKAgE"));
        } catch (Exception e) {
            System.out.println(" Exception Thrown!: 05rKAgE not the same as O5rKAgE");
        }

        // Loop some possible integer values
        System.out.println("\nLoop some possible integer values");
        try {
            Integer i = 1000000000;
            while (i < 1000000005) {
                String encodedValue = minter.encode(new BigInteger(i.toString()));
                String decodedValue = minter.decode(encodedValue).toString();
                System.out.println(" start: " + i + " -> encoded: " + encodedValue + " -> decoded/validated: " + decodedValue);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
