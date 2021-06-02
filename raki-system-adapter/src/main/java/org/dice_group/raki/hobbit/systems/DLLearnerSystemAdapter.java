package org.dice_group.raki.hobbit.systems;

import com.clarkparsia.owlapi.explanation.io.manchester.ManchesterSyntaxObjectRenderer;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
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
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.ShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DLLearnerSystemAdapter extends AbstractRakiSystemAdapter{
    private CELOE celoeAlg;
    private ClosedWorldReasoner rc;
    private RhoDRDown op;
    private OWLOntology ontology;
    private ShortFormProvider provider;
    private Semaphore serialMutex = new Semaphore(0);


    //TODO add some variables DLLearner may use in init


    public static void main(String[] args) throws Exception {
        DLLearnerSystemAdapter adapter = new DLLearnerSystemAdapter();
        adapter.timeOutMs=1000l;
        adapter.loadOntology(new File("raki-datagenerator/data/carcinogenesis/ontology.owl"));

        JSONObject posNegJson = new JSONObject("{ \"benchmark\":"+FileUtils.readFileToString(new File("raki-datagenerator/data/carcinogenesis/lp.json"))+"}");
        posNegJson.getJSONArray("benchmark").forEach(lp ->{
            try {
                adapter.receiveGeneratedTask("1", lp.toString().getBytes(StandardCharsets.UTF_8));
                //System.out.println(adapter.createConcept(lp.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing now");
        super.close();
    }

    @Override
    public String createConcept(String posNegExample) throws Exception {
        JSONObject posNegJson = new JSONObject(posNegExample);
        Set<OWLIndividual> posExamples = getExamples(posNegJson.getJSONArray("positives"));
        Set<OWLIndividual> negExamples = getExamples(posNegJson.getJSONArray("negatives"));
        serialMutex.acquire();
        PosNegLP lp = new PosNegLPStandard(rc);
        lp.setNegativeExamples(negExamples);
        lp.setPositiveExamples(posExamples);
        lp.init();
        AtomicReference<String> atomicConcept = new AtomicReference<>("");
        atomicConcept.set(celeo(lp));

        //String ret =  celeo(lp);
        serialMutex.release();
        return atomicConcept.get();
    }

    @Override
    public void releaseMutexes(){
        serialMutex.release();
    }

    private String celeo(PosNegLP lp) throws ComponentInitException {


        this.celoeAlg = new CELOE(lp, rc);
        celoeAlg.setMaxExecutionTimeInSeconds(Math.max(1, timeOutMs/1000));
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
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(ontologyFile), loaderConfig);


        //ontology = new OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("http://raki.merged.ontology/"));
        LOGGER.info("Initialize KS");
        AbstractKnowledgeSource ks = new OWLAPIOntology(ontology);
        ks.init();

        LOGGER.info("Initialize reasoner");
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
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
