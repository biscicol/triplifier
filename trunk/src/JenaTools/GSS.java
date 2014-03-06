package JenaTools;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 *
 * @author Remi Barraquand
 */
public class GSS {
    protected static final String URI = "http://www.w3.org/2001/11/IsaViz/graphstylesheets#";
    protected static final String prefix = "gss";

    public static String getURI() {
        return URI;
    }

    public static String getPrefix() {
        return prefix;
    }

    private static Model m = ModelFactory.createDefaultModel();

    public static final Property STYLE = m.createProperty(URI, "style");

    public static final Property STROKE = m.createProperty(URI, "stroke");
    public static final Property STROKEWIDTH = m.createProperty(URI, "stroke-width");
    public static final Property SHAPE = m.createProperty(URI, "shape");
    public static final Property FILL = m.createProperty(URI, "fill");

    public static final Property FIXEDSIZE = m.createProperty(URI, "fixedsize");
    public static final Property WIDTH = m.createProperty(URI, "width");
    public static final Property HEIGHT = m.createProperty(URI, "height");

    public static final Property FONTFAMILY = m.createProperty(URI,"font-family");
    public static final Property FONTSIZE = m.createProperty(URI,"font-size");
    public static final Property FONTWEIGHT = m.createProperty(URI,"font-weight");
    public static final Property FONTSTYLE = m.createProperty(URI,"font-style");
    public static final Property TEXTALIGN = m.createProperty(URI,"text-align");

    public static final Property VISIBILITY = m.createProperty(URI,"visibility");
    public static final Property DISPLAY = m.createProperty(URI,"display");
}

