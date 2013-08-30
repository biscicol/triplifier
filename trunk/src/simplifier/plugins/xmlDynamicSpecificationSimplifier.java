package simplifier.plugins;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read an XML file to construct a simplifier
 * --- STILL in development, need to figure out Apache Digester
 * Getting this running should simplify the simplifier
 * This may or may not go with triplify codebase... still testing it.
 */
public class xmlDynamicSpecificationSimplifier {
    public xmlDynamicSpecificationSimplifier() throws IOException, SAXException {

        // TODO: read an actual XML file here
        String xmlToParseString = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<FIMS xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" +
                "      xsi:noNamespaceSchemaLocation='bioValidator-0.9.xsd'>\n" +
                "    <Mapping>\n" +
                "        <entities\n" +
                "                bcid=\"ark:/21547/TEMPIdentificationProcess2_\"\n" +
                "                conceptName=\"identificationProcess\"\n" +
                "                conceptURI=\"http://purl.obolibrary.org/obo/bco_0000042\"\n" +
                "                worksheet=\"Specimens\"\n" +
                "                worksheetUniqueKey=\"Specimen_Num_Collector\">\n" +
                "            <property column=\"IdentifiedBy\" uri=\"dwc:identifiedBy\"/>\n" +
                "        </entities>\n" +
                "\n" +
                "        <entities\n" +
                "                bcid=\"ark:/21547/Q2_\"\n" +
                "                conceptName=\"tissue\"\n" +
                "                conceptURI=\"http://purl.obolibrary.org/obo/OBI_0100051\"\n" +
                "                worksheet=\"Specimens\"\n" +
                "                worksheetUniqueKey=\"Specimen_Num_Collector\">\n" +
                "            <property column=\"format_name96\" uri=\"bsc:plate\"/>\n" +
                "            <property column=\"well_number96\" uri=\"bsc:well\"/>\n" +
                "        </entities>\n" +
                "    </Mapping>\n" +
                "</FIMS>";


        /*
        String xmlToParseString =("<?xml version='1.0' ?>\n" +
                "<!DOCTYPE digester-rules PUBLIC\n" +
                "'-//Jakarta Apache //DTD digester-rules XML V1.0//EN'\n" +
                "'digester-rules.dtd'>\n" +
                "<digester-rules>\n" +
                "    <pattern value='books'>\n" +
                "        <object-create-rule classname='java.util.ArrayList' />\n" +
                "        <pattern value='book'>\n" +
                "            <object-create-rule classname='Book' />\n" +
                "            <set-properties-rule />\n" +
                "            <call-method-rule pattern='name' methodname='setName'\n" +
                "                paramcount='1' />\n" +
                "            <call-param-rule pattern='name' paramnumber='0' />\n" +
                "            <set-next-rule methodname='add' />\n" +
                "        </pattern>\n" +
                "    </pattern>\n" +
                "</digester-rules>");
                */
        InputStream xmlToParse = new ByteArrayInputStream(xmlToParseString.getBytes());

        Digester d = new Digester();
        d.addObjectCreate("FIMS/Mapping/entities", entities.class);
        d.addSetProperties("FIMS/Mapping/entities");
        System.out.println(d.parse(xmlToParse));


        // I think need some file to do something with results
        //Rules r = new Rules(ws);
        //

        //d.parse(new StringReader(xmlToParse));

    }

    public static void main(String[] args) {
        try {
            xmlDynamicSpecificationSimplifier s = new xmlDynamicSpecificationSimplifier();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    protected void initDigester(Digester d) {
        d.addObjectCreate("FIMS/Mapping/entities", entities.class);
        d.addSetProperties("FIMS/Mapping/entities");
        d.addSetNext("FIMS/Mapping/Entity", "addEntity");
        d.addCallMethod("Validate/Mapping/Entity/property", "addProperty", 0);
    }

    public class entities {
        private List properties = new ArrayList();

        public void addProperty(String field) {
            System.out.println(field);
            properties.add(field);
        }

        public void addEntity(String field) {
            System.out.println(field);
        }
    }

    public class Book {

        private String isbn = null;

        private String name = null;

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
