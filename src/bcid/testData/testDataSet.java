package bcid.testData;

import bcid.ResourceTypes;

import java.net.URISyntaxException;
import java.util.ArrayList;


/**
 * Construct a sample dataset in an ArrayList to use for testing
 */
public class testDataSet extends ArrayList {
    public testDataSet() {
        try {
            this.add(new testDataRow("http://biocode.berkeley.edu/specimens/MBIO56","MBIO56", ResourceTypes.PHYSICALOBJECT));
            //this.add(new testDataRow("http://biocode.berkeley.edu/specimens/MBIO1000","MBIO1000", ResourceTypes.PHYSICALOBJECT));
            //this.add(new testDataRow("http://biocode.berkeley.edu/specimens/MBIO1400","MBIO1400", ResourceTypes.PHYSICALOBJECT));
            this.add(new testDataRow("http://biocode.berkeley.edu/events/66","CM91", ResourceTypes.EVENT));
            //this.add(new testDataRow("http://biocode.berkeley.edu/events/88","CM125-126", ResourceTypes.EVENT));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
