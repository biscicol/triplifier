package simplifier.plugins;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a brute-force method for reading Genbank files and writing triples.  It gets most of its content
 * from the source qualifiers and uses the following ontology for terms: https://github.com/tfuji/INSDC/blob/master/insdc.ttl
 * writing output to TTL
 */
public class genbankSimplifier {

    BufferedReader bufferedReader;
    FileOutputStream fileOutputStream;

    /**
     * Constructor here takes an inputFile and an outputFile
     * @param inputFile
     * @param outputFile
     * @throws IOException
     */
    public genbankSimplifier(File inputFile, File outputFile) throws IOException {
        // Create bufferedReader for reading input
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        // if outputFile doesnt exists, then create it
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        // Create fileOutputStream for writing output
        fileOutputStream = new FileOutputStream(outputFile);

        // Setup variables here
        String line;
        GBrecord gBrecord = new GBrecord();
        Boolean source = false, gene = false;

        // Loop file
        while ((line = bufferedReader.readLine()) != null) {
            // Source Qualifier Section
            if (source) {
                if (qualifierValue("specimen_voucher", line)) {
                    gBrecord.gBsource.specimen_voucher = qualifierKey(line);
                } else if (qualifierValue("mol_type", line)) {
                    gBrecord.gBsource.mol_type = qualifierKey(line);
                } else if (qualifierValue("lat_lon", line)) {
                    gBrecord.gBsource.lat_lon = qualifierKey(line);
                } else if (qualifierValue("organism", line)) {
                    gBrecord.gBsource.organism = qualifierKey(line);
                } else if (qualifierValue("collected_by", line)) {
                    gBrecord.gBsource.collected_by = qualifierKey(line);
                } else if (qualifierValue("collection_date", line)) {
                    gBrecord.gBsource.collection_date = qualifierKey(line);
                } else if (qualifierValue("identified_by", line)) {
                    gBrecord.gBsource.identified_by = qualifierKey(line);
                } else if (qualifierValue("country", line)) {
                    gBrecord.gBsource.country = qualifierKey(line);
                } else if (qualifierValue("db_xref", line)) {
                    gBrecord.gBsource.db_xref.add(qualifierKey(line));
                }

                // Gene Qualifier Section
            } else if (gene) {
                if (qualifierValue("gene", line)) {
                    gBrecord.gBgene.gene = qualifierKey(line);
                }
            }

            if (line.startsWith("LOCUS")) {
                gBrecord.locus = line.substring(12, 24).trim();
            } else if (line.trim().startsWith("source")) {
                source = true;
                gene = false;
            } else if (line.trim().startsWith("gene")) {
                gene = true;
                source = false;
            } else if (line.equals("//")) {
                gene = false;
                source = false;
                gBrecord.print(fileOutputStream);
                gBrecord = new GBrecord();
            }
        }

        bufferedReader.close();
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * Search for the qualifier Key name given a line
     *
     * @param line
     * @return
     * @throws IOException
     */
    private String qualifierKey(String line) throws IOException {
        line = getNextQualifierLine(line);
        return line.split("\\=")[1].replaceAll("\"", "");
    }

    /**
     * Loop qualifier lines until we reach the end.
     * We use the fact that qualifier lines must end in a quote
     *
     * @param line
     * @return
     * @throws IOException
     */
    private String getNextQualifierLine(String line) throws IOException {
        String nextLine = "";
        if (!line.endsWith("\"")) {
            nextLine = bufferedReader.readLine().trim();
            getNextQualifierLine((nextLine));
        }
        return line + nextLine;
    }

    /**
     * Get the qualifier value
     *
     * @param value
     * @param line
     * @return
     */
    private boolean qualifierValue(String value, String line) {
        String qualifier = line.trim().split("=")[0];
        return qualifier.equals("/" + value);
    }

    /**
     * Define the core genbank Record, consisting of subClasses GBsource and GBgene
     */
    public class GBrecord {
        StringBuilder stringBuilder = new StringBuilder();

        String locus = null;
        GBsource gBsource;
        GBgene gBgene;

        GBrecord() {
            gBgene = new GBgene();
            gBsource = new GBsource();
        }

        /**
         * Uses the insdc.owl project terms, available from:
         * https://github.com/tfuji/INSDC/blob/master/insdc.ttl
         * <p/>
         * NOTE: I added the following property: http://insdc.org/owl/source
         *
         * @return
         */
        public void print(FileOutputStream fileOutputStream) throws IOException {
            // Print header information
            stringBuilder.append("<http://www.ncbi.nlm.nih.gov/nuccore/" + locus + "> " +
                    " <http://www.w3.org/TR/rdf-schema/type> " +
                    "<iao:informationContentEntity>;\n");

            // Print source qualifiers
            // to add: source.note, definition
            propertyPrinter("http://insdc.org/owl/specimen_voucher", gBsource.specimen_voucher);
            propertyPrinter("http://insdc.org/owl/mol_type", gBsource.mol_type);
            propertyPrinter("http://insdc.org/owl/lat_lon", gBsource.lat_lon);
            propertyPrinter("http://insdc.org/owl/organism", gBsource.organism);
            propertyPrinter("http://insdc.org/owl/collected_by", gBsource.collected_by);
            propertyPrinter("http://insdc.org/owl/collection_date", gBsource.collection_date);
            propertyPrinter("http://insdc.org/owl/identified_by", gBsource.identified_by);
            propertyPrinter("http://insdc.org/owl/country", gBsource.country);
            propertyPrinter("http://insdc.org/owl/db_xref", gBsource.db_xref);

            // Print gene qualifiers
            propertyPrinter("http://insdc.org/owl/gene", gBgene.gene);

            stringBuilder.append(".\n");

            fileOutputStream.write(stringBuilder.toString().getBytes());
        }

        /**
         * A propertyPrinter using a simple string value
         *
         * @param property
         * @param value
         */
        private void propertyPrinter(String property, String value) {
            if (value != null) {
                stringBuilder.append("  <" + property + "> \"" + value + "\";\n");
            }
        }

        /**
         * A propertyPrinter using a List
         *
         * @param property
         * @param value
         */
        private void propertyPrinter(String property, List value) {
            Iterator<String> iterator = value.iterator();
            while (iterator.hasNext()) {
                stringBuilder.append("  <" + property + "> \"" + iterator.next() + "\";\n");
            }
        }
    }

    /**
     * Define the source qualifier terms
     */
    public class GBsource {
        String specimen_voucher = null;
        String mol_type = null;
        String organism = null;
        String collected_by = null;
        String collection_date = null;
        String identified_by = null;
        String country = null;

        List db_xref = new ArrayList();
        String lat_lon = null;
    }

    /**
     * Define the gene qualifier terms
     */
    public class GBgene {
        String gene = null;
    }

    /**
     * main method for local testing...
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        File dnaFile = new File("/Users/jdeck/Downloads/gbvrt25.seq");
        //File dnaFile = new File("/Users/jdeck/Downloads/test.seq");
        File outputFile = new File("/tmp/outputFile.txt");
        new genbankSimplifier(dnaFile, outputFile);
    }


}
