package bcid;

import java.net.URI;

/**
 * This interface provides a standardized interface for working with Identifiers.
 */
public interface Identifier {

    /**
     * Return an identifier
     * @return
     */
    public URI getIdentifier();
}
