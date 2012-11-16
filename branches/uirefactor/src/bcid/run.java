package bcid;


import bcid.testData.*;
import edu.ucsb.nceas.ezid.EZIDService;
import rest.SettingsManager;

import java.util.ArrayList;

/**
 * The run class demonstrates how to use BiSciCol identifier services.
 */
public class run {



    public static void johnTest(ArrayList<testDataRow> dataSet) throws Exception {

        // UUID creation
        System.out.println("Testing UUID creation");
        for (testDataRow row : dataSet) {
            bcidUuid uuid = new bcidUuid(row.webAddress, row.localID, row.type);
            uuid.mint("http://zoobank.org/specimen/");
            System.out.println(uuid.row());
        }

        // Setup EZID account/login information
        // Currently the user/pass for EZID account set in a properties file, but this should be changed
        // when we start getting users with their own EZID accounts
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        EZIDService ezidAccount = new EZIDService();
        ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));

        // EZID DOI creation
        System.out.println("Testing EZID/DOI creation");
        for (testDataRow row : dataSet) {
            bcidEzidDoi bscEZID = new bcidEzidDoi(ezidAccount, row.webAddress, row.localID, row.type, "Test Account");
            bscEZID.mint("doi:10.5072/FK2");
            System.out.println(bscEZID.row());
        }

        // EZID ARK creation
        System.out.println("Testing EZID/ARK creation");
        for (testDataRow row : dataSet) {
            bcidEzidDoi bscEZID = new bcidEzidDoi(ezidAccount, row.webAddress, row.localID, row.type, "Test Account");
            bscEZID.mint("ark:/99999/fk4");
            System.out.println(bscEZID.row());
        }

    }

    public static void main(String[] args) {
        testDataSet ds = new testDataSet();
        try {
            johnTest(ds);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
