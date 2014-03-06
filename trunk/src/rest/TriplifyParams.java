package rest;

import dbmap.Mapping;

/**
 * This is a simple wrapper class that combines a Mapping object with a
 * String representing an RDF output format.  This class is used by the
 * getTriples() method in Rest.java to capture an incoming triplify request
 * from the Web browser-based client UI.
 */
public class TriplifyParams
{
    public Mapping mapping;
    public String outputformat;

    /**
     * For construction from JSON.
     */
    TriplifyParams() {
    }
}
