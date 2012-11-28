package bcid;

import bcid.testData.*;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import rest.SettingsManager;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;

/**
 * This is a class used for running/testing Identifier creation methods.
 */
public class run {

    /**
     * The testCycle method demonstrates how to use the suite of identifier services
     * and goes through the following steps:
     * 1. Instantiate the Test Dataset used for loading/testing data
     * 2. Create a DOI for a dataset
     * 3. Minting BCIDs for individual data elements
     * 4. Creating EZIDs for individual data elements
     */
    public static void runServices(ArrayList<testDataRow> dataSet, EZIDService ezidAccount) throws Exception {
        // EZID DOI Minting
        System.out.println("Mint DOI:");
        DOI doi = new DOI(ezidAccount, new URI("http://biocodecommons.org/"), "Test Creator", "Test Title", "Test Publisher", "2012");
        System.out.println("  doi = " + doi.getIdentifier());

        // BCID Minting Example
        System.out.println("\nMint a List of BCIDs:");
        ArrayList<bcid> bcidList = new ArrayList();
        for (testDataRow row : dataSet) {
            bcid b = new bcid(doi.getIdentifier(), row.webAddress, row.localID, row.type);
            bcidList.add(b);
        }
        bcidMinter minter = new bcidMinter();
        minter.mintList(bcidList);
        System.out.println("  Succesfully minted " + bcidList.size() + " identifiers");

        // Run script to Create EZIDs (from those living in identifiers table)
        System.out.println("\nCreating EZIDS:");
        minter.createEZIDs(ezidAccount);

        minter.close();
    }

    public static void main(String[] args) {
        // Setup EZID account/login information
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        EZIDService ezidAccount = new EZIDService();
        try {
            ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
        } catch (EZIDException e) {
            e.printStackTrace();
        }
        testDataSet ds = new testDataSet();
        try {
            runServices(ds, ezidAccount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
