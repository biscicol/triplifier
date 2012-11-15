package bcid;

import java.net.URISyntaxException;

/**
 * Identifier instance defines the methods that all Identifier classes must implement
 */
interface Identifier {

    /**
     * mint used to actually create the identifier after we construct the identifier
     * @param prefix
     * @throws java.net.URISyntaxException
     */
    public void mint(String prefix) throws URISyntaxException;



}
