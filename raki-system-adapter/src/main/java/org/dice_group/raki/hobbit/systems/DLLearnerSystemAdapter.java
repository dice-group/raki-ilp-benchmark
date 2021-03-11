package org.dice_group.raki.hobbit.systems;

import com.clarkparsia.owlapi.explanation.io.manchester.ManchesterSyntaxObjectRenderer;
import com.google.common.collect.Sets;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractKnowledgeSource;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.dllearner.reasoning.ReasonerImplementation;
import org.dllearner.refinementoperators.RhoDRDown;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DLLearnerSystemAdapter extends AbstractRakiSystemAdapter{
    private CELOE celoeAlg;
    private ClosedWorldReasoner rc;
    private RhoDRDown op;
    private OWLOntology ontology;
    private ShortFormProvider provider;

    //TODO add some variables DLLearner may use in init


    @Override
    public String createConcept(String posNegExample) throws Exception {
        JSONObject posNegJson = new JSONObject(posNegExample);
        Set<OWLIndividual> posExamples = getExamples(posNegJson.getJSONArray("positives"));
        Set<OWLIndividual> negExamples = getExamples(posNegJson.getJSONArray("negatives"));

        PosNegLP lp = new PosNegLPStandard(rc);
        lp.setNegativeExamples(negExamples);
        lp.setPositiveExamples(posExamples);
        lp.init();
        return celeo(lp);
    }

    private String celeo(PosNegLP lp) throws ComponentInitException {
        this.celoeAlg = new CELOE(lp, rc);
        celoeAlg.setMaxExecutionTimeInSeconds(timeOutMs);
        celoeAlg.setOperator(op);
        celoeAlg.setWriteSearchTree(true);
        celoeAlg.setSearchTreeFile("log/search-tree.log");
        celoeAlg.setReplaceSearchTree(true);
        celoeAlg.init();
        celoeAlg.setKeepTrackOfBestScore(true);

        celoeAlg.start();

        OWLClassExpression best = celoeAlg.getCurrentlyBestDescription();

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        return renderer.render(best);
    }

    private Set<OWLIndividual> getExamples(JSONArray examples) {
        Set<OWLIndividual> ret = new HashSet<OWLIndividual>();
        examples.forEach(uri -> {
            ret.add(new OWLNamedIndividualImpl(IRI.create(uri.toString())));
        });
        return ret;
    }

    @Override
    public void loadOntology(File ontologyFile) throws Exception {
        ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);
        AbstractKnowledgeSource ks = new OWLAPIOntology(ontology);
        ks.init();

        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);
        baseReasoner.setReasonerImplementation(ReasonerImplementation.HERMIT);
        baseReasoner.init();
        rc = new ClosedWorldReasoner(ks);
        rc.setReasonerComponent(baseReasoner);
        rc.init();

        op = new RhoDRDown();
        op.setReasoner(rc);
        op.setUseNegation(false);
        op.setUseHasValueConstructor(false);
        op.setUseCardinalityRestrictions(true);
        op.setUseExistsConstructor(true);
        op.setUseAllConstructor(true);
        op.init();
    }
}
