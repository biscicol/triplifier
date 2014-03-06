package JenaTools;

/*
 * JenaTools.java
 *
 * Provides utility function for use with Jena.
 *
 * Copyright (c) 2010 Remi Barraquand <dev at barraquand.com>. All rights reserved.
 *
 * This file is part of JenaTools
 *
 * JenaTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JenaTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JenaTools. If not, see <http://www.gnu.org/licenses/>.
 *
 * Software written by:
 * Barraquand Remi <dev@barraquand.com>
 * Emonet Remi <remi@heeere.com>.
 */

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;

import com.hp.hpl.jena.shared.JenaException;

import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Jena Help class
 * @author Remi Barraquand <dev at barraquand.com>
 */
public class JenaTools {

    /**
     * Load a model from a string as N-TRIPLES.
     * @param content data
     * @return
     */
    public static Model loadModelFromString(String content) {
        return loadModelFromString(content, "N-TRIPLES");
    }

    /**
     * Load model from string.
     * @param content
     * @param lang
     * @return
     */
    public static Model loadModelFromString(String content, String lang) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(content), null, lang);
        return model;
    }

    /**
     * Load model from a list of statements.
     * @param statements
     * @return
     */
    public static Model loadModelFromStatements(Collection<Statement> statements) {
        Model model = ModelFactory.createDefaultModel();
        model.add(statements.toArray(new Statement[0]));
        return model;
    }

    /**
     * Load model from filename.
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public static Model loadModelFromFile(String fileName) throws FileNotFoundException {
        return loadModelFromStream(new FileInputStream(new File(fileName)));
    }

    /**
     * Load model from input stream as N-TIPLES.
     * @param in
     * @return
     */
    public static Model loadModelFromStream(InputStream in) {
        return loadModelFromStream(in, null);
    }

    /**
     * Load model from input stream.
     * @param in
     * @param lang
     * @return
     */
    public static Model loadModelFromStream(InputStream in, String lang) {
        Model model = ModelFactory.createDefaultModel();
        model.read(in, null, lang);
        return model;
    }

    /**
     * Dump model to String as RDF/XML format
     * @param model
     * @return
     */
    public static String dumpModelToString(Model model) {
        return dumpModelToString(model, null);
    }

    /**
     * Dump model to String.
     * @param model
     * @param lang
     * @return
     */
    public static String dumpModelToString(Model model, String lang) {
        StringOutputStream outputStream = new StringOutputStream();
        model.write(outputStream, lang);
        return outputStream.toString();
    }

    public static void dumpRules(Reasoner reasoner) {
        dumpRules(reasoner, new PrintWriter(System.out));
    }

    public static void dumpRules(Reasoner reasoner, PrintWriter out) {
        if (reasoner instanceof GenericRuleReasoner) {
            for (Object rule : ((GenericRuleReasoner) reasoner).getRules()) {
                out.println(rule.toString());
            }
        }
    }

    public static void dumpRulesCount(Reasoner reasoner) {
        dumpRulesCount(reasoner, System.out);
    }

    public static void dumpRulesCount(Reasoner reasoner, PrintStream out) {
        if (reasoner instanceof GenericRuleReasoner) {
            out.println("-- Total Rules Count: " + ((GenericRuleReasoner) reasoner).getRules().size());
        }
    }

    public static Model union(Model... models) {
        if (models.length == 0) {
            return newEmptyModel();
        } else {
            Model model = null;
            for (Model m : models) {
                if (model == null) {
                    model = m;
                } else {
                    model = model.union(m);
                }
            }
            return model;
        }
    }

    public static Model dynamicUnion(Model... models) {
        if (models.length == 0) {
            return newEmptyModel();
        } else {
            Model model = null;
            for (Model m : models) {
                if (model == null) {
                    model = m;
                } else {
                    model = ModelFactory.createUnion(model, m);
                }
            }
            return model;
        }
    }

    /**
     * Processes the union of the given models.
     * @param models
     * @return
     */
    public static InfModel inferenceUnion(InfModel... models) {
        if (models.length == 0) {
            return newEmptyInfModel();
        } else {
            InfModel first = models[0];
            GenericRuleReasoner reasoner = (GenericRuleReasoner) GenericRuleReasonerFactory.theInstance().create(null);
            Model model = first;
            for (InfModel m : models) {
                reasoner.addRules(((GenericRuleReasoner) m.getReasoner()).getRules());
                if (m != first) {
                    model = model.union(m);
                }
            }
            return ModelFactory.createInfModel(reasoner, model);
        }
    }

    public static Model newEmptyModel() {
        return loadModelFromString("");
    }

    public static InfModel newEmptyInfModel() {
        return ModelFactory.createInfModel(loadReasonerFromString(""), loadModelFromString(""));
    }

    public static InfModel inferenceModel(Model model) {
        return ModelFactory.createInfModel(loadReasonerFromString(""), model);
    }

    public static Statement getStatementFromPath(Resource r, String... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("path must be non-empty");
        }

        Model m = r.getModel();
        Statement statement = null;

        for (String p : path) {
            if (statement != null) {
                r = (Resource) statement.getObject();
            }

            StmtIterator it = m.listStatements(r, m.createProperty(p), (RDFNode) null);
            if (!it.hasNext()) {
                throw new IllegalArgumentException("path does not lead to a valid statement");
            }
            statement = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException("path is not unique");
            }
        }
        return statement;
    }

    public static Statement getStatementFromPath(Resource r, Property... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("path must be non-empty");
        }

        Model m = r.getModel();
        Statement statement = null;

        for (Property p : path) {

            if (statement != null) {
                r = (Resource) statement.getObject();
            }

            StmtIterator it = m.listStatements(r, p, (RDFNode) null);
            if (!it.hasNext()) {
                throw new IllegalArgumentException("path does not lead to a valid statement");
            }
            statement = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException("path is not unique");
            }
        }
        return statement;
    }

    public static Literal getLiteralFromResource(Resource r, Property p) {
        return (Literal) r.getProperty(p).getObject();
    }

    public static Resource getSubjectWithProperty(Model m, Property p, RDFNode o) {
        ResIterator rc = m.listResourcesWithProperty(p, o);
        if (rc.hasNext()) {
            return rc.nextResource();
        } else {
            return null;
        }
    }

    public static Resource getUniqueSubjectWithProperty(Model m, Property p, RDFNode o) throws JenaException {
        ResIterator rc = m.listSubjectsWithProperty(p, o);
        Resource r = rc.nextResource();
        if (rc.hasNext() || p == null) {
            throw new JenaException("Subject of " + p.toString() + " with value " + o + " must be unique");
        }

        return r;
    }

    public static Resource getUniqueSubjectWithProperty(Model m, Property p, String o) throws JenaException {
        ResIterator rc = m.listSubjectsWithProperty(p, o);
        Resource r = rc.nextResource();
        if (rc.hasNext() || p == null) {
            throw new JenaException("Subject of " + p.toString() + " with value " + o + " must be unique");
        }

        return r;
    }

    /**
     * Get all resources from a model matching a given query. This returns a
     * collection of resources.
     * Queries are required to follow this pattern in the select clause:
     *
     * <code>SELECT ?s WHERE ...</code>
     *
     * It's important to use name variable ?s. This is the named variable
     * JenaTools will expect.
     *
     * @param model
     * @param query
     * @return
     */
    public static Collection<Resource> queryResourcesFromModel(Model model, String query) {
        Collection<Resource> resources = new ArrayList<Resource>();

        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.next();
                resources.add(soln.getResource("s"));
            }
        } finally {
            qexec.close();
        }

        return resources;
    }

    /************ old stuff **********/
    public static Reasoner loadReasonerFromString(String content) {
        List rules = Rule.parseRules(content);
//      GenericRuleReasoner reasoner = (GenericRuleReasoner) OWLMiniReasonerFactory.theInstance().create(null);
//      GenericRuleReasoner reasoner = (GenericRuleReasoner) OWLMicroReasonerFactory.theInstance().create(null);
        GenericRuleReasoner reasoner = (GenericRuleReasoner) GenericRuleReasonerFactory.theInstance().create(null);
        //reasoner.setMode(GenericRuleReasoner.HYBRID);
        reasoner.addRules(rules);
        //reasoner.setOWLTranslation(true);
        reasoner.setTransitiveClosureCaching(true);
        return reasoner;
    }
}
