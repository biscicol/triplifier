package JenaTools;

import com.hp.hpl.jena.rdf.model.*;
import org.kohsuke.graphviz.Attribute;
import org.kohsuke.graphviz.Node;
import org.kohsuke.graphviz.Edge;
import org.kohsuke.graphviz.Graph;
import org.kohsuke.graphviz.Shape;
import org.kohsuke.graphviz.Style;
import org.kohsuke.graphviz.StyleAttr;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Barraquand Remi
 */
public class rdf2dot {

    static Map<RDFNode, Node> nodes = new HashMap<RDFNode, Node>();
    static Map<Property, Edge> edges = new HashMap<Property, Edge>();

    /**
     * current model behing workout **
     */
    static Model m;

    static int annoCount = 0;

    /**
     * Build a dot representation of RDF model.
     *
     * @param model
     * @return
     * @throws IOException
     */
    public static String parse(Model model) throws IOException {
        Graph graph = new Graph();
        StringOutputStream os = new StringOutputStream();

        m = model;
        nodes.clear();
        edges.clear();

        StmtIterator itr = m.listStatements();
        while (itr.hasNext()) {
            Statement stm = itr.nextStatement();
            Resource subject = stm.getSubject();
            Property property = stm.getPredicate();
            RDFNode object = stm.getObject();

            // do not print stylish statement
            if (!property.getNameSpace().equals(GSS.getURI())) {
                printStatement(graph, subject, property, object);
            }
        }


        graph.writeTo(os);
        return os.toString();
    }

    static private void printStatement(Graph g, Resource subject, Property property, RDFNode object) {
        Node subject_node = nodes.get(subject);
        Node object_node = nodes.get(object);

        // if object node does not exist already, create it
        if (subject_node == null) {
            if (subject.isAnon()) {
                subject_node = new Node().attr("label", shortenLabel(subject.getId().getLabelString())).style(buildStyleFromResourceOrLiteral(subject));
            } else {
                subject_node = new Node().attr("label", shortenLabel(subject.getURI())).style(buildStyleFromResourceOrLiteral(subject));
            }

            nodes.put(subject, subject_node);
            g.node(subject_node);
        }

        // if object node does not exist already, create it
        if (object_node == null) {
            if (object.isResource()) {
                Resource obj = (Resource) object;
                if (obj.isAnon()) {
                    object_node = new Node().attr("label", shortenLabel(obj.getId().getLabelString())).style(buildStyleFromResourceOrLiteral(obj));
                } else {
                    object_node = new Node().attr("label", shortenLabel(obj.getURI())).style(buildStyleFromResourceOrLiteral(obj));
                }
            } else {
                Literal obj = (Literal) object;
                object_node = new Node().attr("label", shortenLabel(obj.getString())).style(buildStyleFromResourceOrLiteral(obj));
            }

            nodes.put(object, object_node);
            g.node(object_node);
        }

        Edge edge = new Edge(subject_node, object_node).attr("label", shortenLabel(property.getLocalName()));
        g.edgeWith(buildStyleFromProperty(property));
        g.edge(edge);
    }

    static private String shortenLabel(String label) {
        if (label.startsWith("http://")) {
            int lastSlash = label.lastIndexOf("/") + 1;
            String remaining = label.substring(lastSlash);
            if (remaining.indexOf("%2F") > 0) {
                lastSlash = remaining.lastIndexOf("%2F") + 3;
                remaining = remaining.substring(lastSlash);
            }
            label = "http://.../" + remaining;
        }

        // temp
        else if (label.startsWith("[")) {
            return "expr-" + annoCount++;
        }

        return label;
    }

    static private Style buildStyleFromResourceOrLiteral(RDFNode n) {
        Style s = new Style();
        Resource style = JenaTools.getSubjectWithProperty(m, GSS.STYLE, n);

        if (style == null) {
            return s;
        }

        // STROKE
        if (style.hasProperty(GSS.STROKE)) {
            s.attr(Attribute.COLOR, getColor(style.getProperty(GSS.STROKE).getLiteral().getString()));
        }

        // STROKEWIDTH
        if (style.hasProperty(GSS.STROKEWIDTH)) {
            s.attr(Attribute.STYLE, StyleAttr.valueOf(style.getProperty(GSS.STROKEWIDTH).getLiteral().getString().toUpperCase()));
        }

        // SHAPE
        if (style.hasProperty(GSS.SHAPE)) {
            s.attr(Attribute.SHAPE, Shape.valueOf(style.getProperty(GSS.SHAPE).getLiteral().getString().toUpperCase()));
        }

        // SIZE
        if (style.hasProperty(GSS.FIXEDSIZE)) {
            s.attr(Attribute.FIXEDSIZE, style.getProperty(GSS.FIXEDSIZE).getLiteral().getBoolean());
            s.attr(Attribute.WIDTH, style.getProperty(GSS.WIDTH).getLiteral().getFloat());
            s.attr(Attribute.HEIGHT, style.getProperty(GSS.HEIGHT).getLiteral().getFloat());
        }

        // FILL
        if (style.hasProperty(GSS.FILL)) {
            s.attr(Attribute.COLOR, getColor(style.getProperty(GSS.FILL).getLiteral().getString()));
            s.attr(Attribute.STYLE, StyleAttr.FILLED);
        }

        // VISIBILITY
        if (style.hasProperty(GSS.VISIBILITY) && style.getProperty(GSS.VISIBILITY).getString().equals("Hidden")) {
            s.attr(Attribute.STYLE, StyleAttr.INVIS);
        }

        return s;
    }

    public static Style buildStyleFromProperty(Property p) {
        Style s = new Style();

        return s;
    }

    public static Color getColor(String colorName) {
        try {
            if (colorName.startsWith("#")) {
                return Color.decode(colorName);
            } else {
                // Find the field and value of colorName
                Field field = Class.forName("java.awt.Color").getField(colorName);
                return (Color) field.get(null);
            }
        } catch (Exception e) {
            return null;
        }
    }
}