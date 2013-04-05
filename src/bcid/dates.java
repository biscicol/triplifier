package bcid;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
* Convenience class for working with dates related to BCIDs.
 */
public class dates {

     /**
     * Record all notions of now() for BCIDs in a consistent manner
     *
     * @return
     */
    public String now() {
        SimpleDateFormat formatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ssZ");
        formatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatUTC.format(new Date());
    }
}
