package JenaTools;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.util.PrintUtil;

/**
 *
 * @author Barraquand Remi
 */
public class RDFStylizer {

    Reasoner  reasoner;

    public RDFStylizer(Resource configuration) {
        PrintUtil.registerPrefix(GSS.getPrefix(), GSS.getURI());
        reasoner = GenericRuleReasonerFactory.theInstance().create(configuration);
    }



    public InfModel applyStyleToModel(Model m) {
        InfModel infmodel = ModelFactory.createInfModel(reasoner, m);
        infmodel.write(System.out);
        return infmodel;
    }
}


