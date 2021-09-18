package org.dice_group.raki.hobbit.systems;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dice_group.raki.core.utils.TablePrinter;
import org.dice_group.raki.hobbit.system.AbstractRakiSystemAdapter;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.AbstractKnowledgeSource;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.dllearner.reasoning.ReasonerImplementation;
import org.dllearner.refinementoperators.RhoDRDown;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * This is the raki ilp system adapter for the DLLearner
 */
public class DLLearnerSystemAdapter extends AbstractRakiSystemAdapter {


    private ClosedWorldReasoner rc;
    private RhoDRDown op;
    private ShortFormProvider provider;
    private ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

    private List<List<List<Object>>> concepts = new ArrayList<>();

    private final Semaphore serialMutex = new Semaphore(0);

    @Override
    public void close() throws IOException {
        //print stuff here
        for (int i =0 ; i<concepts.size(); i++) {
            System.out.println("Learning Problem "+i);
            TablePrinter.print(concepts.get(i), Lists.newArrayList("CONCEPT", "LENGTH", "ACCURACY"),
                    "%80s\t%5d\t%5f");
            System.out.println();
        }
        super.close();
    }

    @Override
    public String createConcept(String posNegExample) throws Exception {
        // read the LP from the string
        JSONObject posNegJson = new JSONObject(posNegExample);
        //get positive and negative examples
        Set<OWLIndividual> posExamples = getExamples(posNegJson.getJSONArray("positives"));
        Set<OWLIndividual> negExamples = getExamples(posNegJson.getJSONArray("negatives"));

        //get the mutex, we will release it later on in the AbstractSystemAdapter
        serialMutex.acquire();
        PosNegLP lp = new PosNegLPStandard(rc);
        lp.setNegativeExamples(negExamples);
        lp.setPositiveExamples(posExamples);
        lp.init();
        return celeo(lp);
    }

    @Override
    public void releaseMutexes(){
        serialMutex.release();
    }

    /**
     * This creates the CELEO Algorithm from DLLearner and returns the Concept in Manchester Syntax.
     * @param lp The learning problem to create the concept for
     * @return The concept rendered in Manchester Syntax
     * @throws ComponentInitException
     */
    private String celeo(PosNegLP lp) throws ComponentInitException {

        //Simply copied that from the example DLLearner
        CELOE celoeAlg = new CELOE(lp, rc);
        celoeAlg.setMaxExecutionTimeInSeconds(Math.max(1, timeOutMs/1000));
        celoeAlg.setOperator(op);
        celoeAlg.setWriteSearchTree(true);
        celoeAlg.setSearchTreeFile("log/search-tree.log");
        celoeAlg.setReplaceSearchTree(true);
        celoeAlg.init();
        celoeAlg.setKeepTrackOfBestScore(true);

        celoeAlg.start();

        //save all info to print at the end
        List<List<Object>> tmp  =new ArrayList<>();
        celoeAlg.getCurrentlyBestEvaluatedDescriptions().forEach( it ->
                tmp.add(Lists.newArrayList(renderer.render(it.getDescription()),
                        it.getDescriptionLength(),
                        it.getAccuracy()))
        );
        concepts.add(tmp);

                // We want the best OWLClassExpression
        OWLClassExpression best = celoeAlg.getCurrentlyBestDescription();
        LOGGER.info("Best expression: {}", best);

        return renderer.render(best);
    }

    /**
     * Get the set of OWLIndividuals in the examples
     *
     * @param examples
     * @return
     */
    private Set<OWLIndividual> getExamples(JSONArray examples) {
        Set<OWLIndividual> ret = new HashSet<OWLIndividual>();
        examples.forEach(uri -> {
            ret.add(new OWLNamedIndividualImpl(IRI.create(uri.toString())));
        });
        return ret;
    }

    @Override
    public void loadOntology(File ontologyFile) throws Exception {

        // initializes the DLLearner stuff

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(ontologyFile), loaderConfig);


        //ontology = new OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("http://raki.merged.ontology/"));
        LOGGER.info("Initialize KS");
        AbstractKnowledgeSource ks = new OWLAPIOntology(ontology);
        ks.init();

        LOGGER.info("Initialize reasoner");
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        renderer.setShortFormProvider(provider);


        OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);
        baseReasoner.setReasonerImplementation(ReasonerImplementation.PELLET);
        baseReasoner.init();
        LOGGER.info("Initialize closed world reasoner");

        rc = new ClosedWorldReasoner(ks);
        rc.setReasonerComponent(baseReasoner);
        rc.init();

        LOGGER.info("Initialize rhodrdown");

        op = new RhoDRDown();
        op.setReasoner(rc);
        op.setUseNegation(false);
        op.setUseHasValueConstructor(false);
        op.setUseCardinalityRestrictions(true);
        op.setUseExistsConstructor(true);
        op.setUseAllConstructor(true);
        op.init();
        serialMutex.release();
        LOGGER.info("Initializing done");

    }
}
