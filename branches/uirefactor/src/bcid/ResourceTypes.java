package bcid;

import java.util.ArrayList;

/**
 * ResourceTypes class is a controlled list of available resourceTypes.  This is built into code since these
 * types have rarely changed and in fact are central to so many coding operations and we don't want to rely on
 * instance level configuration control.
 *
 * ResourceTypes encompass the data elements as defined by Dublin Core DCMI
 * at http://dublincore.org/documents/2012/06/14/dcmi-terms/?v=dcmitype#H7
 *
 * See also metadata types as defined by DataCite
 * http://schema.datacite.org/meta/kernel-2.2/doc/DataCite-MetadataKernel_v2.2.pdf  (see page 14, resourceTypeGeneral)
 *
 */
public class ResourceTypes {

    static ArrayList list = new ArrayList();

    public static int COLLECTION = 0;
    public static int DATASET = 1;
    public static int EVENT = 2;
    public static int IMAGE = 3;
    public static int INTERACTIVERESOURCE = 4;
    public static int MOVINGIMAGE = 5;
    public static int PHYSICALOBJECT = 6;
    /*public static int SERVICE = 7;
    public static int SOFTWARE = 8;
    public static int SOUND = 9;
    public static int STILLIMAGE = 10;
    public static int TEXT = 11;
    */

    public ResourceTypes() {
        ResourceType type = null;
        list.add(new ResourceType("Collection", "http://purl.org/dc/dcmitype/Collection", "An aggregation of resources."));
        list.add(new ResourceType("Dataset", "http://purl.org/dc/dcmitype/Dataset", "Data encoded in a defined structure."));
        list.add(new ResourceType("Event", "http://purl.org/dc/dcmitype/Event", "A non-persistent, time-based occurrence."));
        list.add(new ResourceType("Image", "http://purl.org/dc/dcmitype/Image", "A visual representation other than text."));
        list.add(new ResourceType("InteractiveResource", "http://purl.org/dc/dcmitype/InteractiveResource", "A resource requiring interaction from the user to be understood, executed, or experienced."));
        list.add(new ResourceType("MovingImage", "http://purl.org/dc/dcmitype/MovingImage", "A series of visual representations imparting an impression of motion when shown in succession."));
        list.add(new ResourceType("PhysicalObject", "http://purl.org/dc/dcmitype/PhysicalObject", "An inanimate, three-dimensional object or substance."));
    }

    public static ResourceType get(int typeIncrement) {
        return (ResourceType) list.get(typeIncrement);
    }
}
