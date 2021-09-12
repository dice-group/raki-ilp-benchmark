package org.dice_group.raki.hobbit.evaluation;

import com.google.common.collect.Sets;
import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.raki.core.commons.CONSTANTS;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.hobbit.vocab.HOBBIT;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class RakiEvaluation extends AbstractEvaluationModule {

    protected static Logger LOGGER = LoggerFactory.getLogger(RakiEvaluation.class);


    protected OWLOntology ontology;
    protected int errorCount=0;
    protected Double summedRecall=0.0;
    protected Double summedPrecision=0.0;
    protected Double summedF1=0.0;
    protected long tp=0;
    protected long fp=0;
    protected long fn=0;
    protected long noOfConcepts=0;
    private String queueName="DG_2_EVAL_MODULE_QUEUE_NAME";

    protected List<Double> conceptLengths =new ArrayList<Double>();
    protected List<Long> resultTimes =new ArrayList<Long>();
    private SimpleFileReceiver receiver = null;

    private final Semaphore evalStartMutex = new Semaphore(0);
    private OWLReasoner reasoner;
    private OWLOntology owlOnto;
    private Boolean useConcepts = false;


    public void receiveCommand(byte command, byte[] data) {
        if(command == CONSTANTS.COMMAND_ONTO_FULLY_SEND){
            if(receiver!=null){
                receiver.terminate();
            }
        }
        if(command == CONSTANTS.COMMAND_EVAL_START){
            evalStartMutex.release();
        }
        super.receiveCommand(command, data);
    }


    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("Starting eval module");
        if (System.getenv().containsKey(CONSTANTS.ONTOLOGY_QUEUE_NAME)) {
                queueName = System.getenv().get(CONSTANTS.ONTOLOGY_QUEUE_NAME);
        }

        if (System.getenv().containsKey(CONSTANTS.USE_CONCEPTS)) {
            useConcepts = Boolean.parseBoolean(System.getenv().get(CONSTANTS.USE_CONCEPTS));
        }
        LOGGER.info("Eval module queue name {}, useConcepts {}", queueName, useConcepts);
        LOGGER.info("receiving ontology "+ Instant.now());
        receiver= SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);
        //we know, that at this point, the onto was fully send.
        String[] receivedFiles = receiver.receiveData("/raki/tempOntologyDirEval/");
        //IOUtils.closeQuietly(this.incomingDataQueueFactory.createDefaultRabbitQueue(queueName));

        LOGGER.info("received ontology {}", new File("/raki/tempOntologyDirEval/").listFiles().length);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        //load owl ontology
        //ontology = manager.loadOntologyFromOntologyDocument(new File("/raki/owl.ttl"));
        for(File f : (new File("/raki/tempOntologyDirEval/").listFiles())){
            LOGGER.info("file {}", f.getAbsolutePath());
            LOGGER.info("Size of recv ont: {}", f.length());
            OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
            ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(f), loaderConfig);
            //ontology= manager.loadOntologyFromOntologyDocument(f);
            LOGGER.info("Axioms in Ontology: {}" , ontology.getAxiomCount());
        }
        //if(receivedFiles.length>1){
        //OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();

        //ontology = new OWLOntologyMerger(manager).createMergedOntology(manager2, ontology.getOntologyID().getOntologyIRI().get());
        //}

        Configuration conf = new Configuration();
        conf.ignoreUnsupportedDatatypes=true;
        conf.throwInconsistentOntologyException=false;
        reasoner = createReasoner();
        owlOnto = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("/raki/owl.ttl"));

        LOGGER.debug("Sending Eval Loaded command.");
        try {
            sendToCmdQueue(CONSTANTS.COMMAND_EVAL_LOADED);
        } catch (IOException e) {
            LOGGER.error("Error while sending command that eval is loaded", e);
        }
    }

    private OWLReasoner createReasoner() {
        OWLReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        return reasoner;
    }

    @Override
    protected void collectResponses() throws Exception {
        evalStartMutex.acquire();
        super.collectResponses();
        evalStartMutex.release();
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp, long responseReceivedTimestamp) throws Exception {
        //convert data 2 concept
        LOGGER.info("Recv an eval ");
        noOfConcepts++;
        this.resultTimes.add(responseReceivedTimestamp-taskSentTimestamp);
        try {

            Set<OWLNamedIndividual> receivedSet = new HashSet<>();
            if(receivedData.length==0){
                LOGGER.error("Concept Length is 0. Defined as error.");
                errorCount++;
                //this.conceptLengths.add(0.0);
            }
            else {
                try {
                    OWLClassExpression receivedConcept = parseManchesterConcept(RabbitMQUtils.readString(receivedData));
                    LOGGER.info("Received concept is: {}.", receivedConcept);
                    //OWLOntology ont = new OWLOntologyImpl(ontology);
                    this.conceptLengths.add(getConceptLength(receivedConcept));
                    LOGGER.info("retrieving instances now");
                    receivedSet = getInstances(receivedConcept);
                    LOGGER.info("Retrieved {} instances.", receivedSet.size());
                }catch(Exception e){
                    LOGGER.error("Found error while parsing concept. ", e);
                    this.errorCount++;
                }
            }
            //evaluate and add to tp,fp,fn count, as well as f1,recall, precision
            //0=tp, 1=fp, 2=fn
            int[] evalVals = evaluate(expectedData, receivedSet);
            this.tp += evalVals[0];
            this.fp += evalVals[1];
            this.fn += evalVals[2];
            //0=f1, 1=prec, 2=recall
            double[] f1 = calculateF1Measure(tp, fp, fn);
            this.summedF1 += f1[0];
            this.summedPrecision += f1[1];
            this.summedRecall += f1[2];
            LOGGER.info("Got {} {} {} {} {} {}" , tp,fp,fn,f1[0], f1[1], f1[2]);
        }catch (Exception e){
            LOGGER.error("Found error while evaluating. ", e);
            this.errorCount++;
            //this.conceptLengths.add(0.0);
        }
    }

    private Set<OWLNamedIndividual> getInstances(OWLClassExpression receivedConcept)
    {
        return reasoner.getInstances(receivedConcept).getFlattened();
    }


    private int[] evaluate(byte[] expectedData, Set<OWLNamedIndividual> receivedSet) {
        String expectedPosNeg = RabbitMQUtils.readString(expectedData);

        JSONObject posNegJson = new JSONObject(expectedPosNeg);
        if(posNegJson.has("concept") && useConcepts) {
            OWLClassExpression expectedConcept = parseManchesterConcept(posNegJson.getString("concept"));
            LOGGER.info("expected concept {}", expectedConcept);
            Set<OWLNamedIndividual> expectedSet = getInstances(expectedConcept);
            //evaluate and add to tp,fp,fn count, as well as f1,recall, precision
            //0=tp, 1=fp, 2=fn
            LOGGER.info("Evaluation uses individuals retrieved by gold standard concept. ");
            return evaluateSets(expectedSet, receivedSet);
        }
        else {
            Set<String> positiveExamples = getSetFromJSON(posNegJson, "positives");
            Set<String> negativeExamples = getSetFromJSON(posNegJson, "negatives");
            //evaluate and add to tp,fp,fn count, as well as f1,recall, precision
            //0=tp, 1=fp, 2=fn
            LOGGER.info("Evaluation uses positive/negative examples ");
            return evaluatePosNeg(positiveExamples, negativeExamples, receivedSet);
        }
    }

    private Set<String> getSetFromJSON(JSONObject posNegJson, String key) {
        Set<String> ret = new HashSet<String>();
        posNegJson.getJSONArray(key).forEach(uri -> ret.add(uri.toString()));
        return ret;
    }


    private int[] evaluatePosNeg(Set<String> positiveExamples, Set<String> negativeExamples, Set<OWLNamedIndividual> received) {
        int[] evalVals= new int[]{0,0,0};
        int foundPosExa=0;
        LOGGER.info("positives {}", positiveExamples.size());
        for(OWLIndividual individual: received){
            String individualStr = individual.asOWLNamedIndividual().getIRI().toString();
            LOGGER.info("Found {}", individualStr);
            if(positiveExamples.contains(individualStr)){
                evalVals[0]++; //tp ++
                foundPosExa++;
            }
            else if(negativeExamples.contains(individualStr)){
                evalVals[1]++; //fp ++
            }
            LOGGER.info("evals {}", evalVals);
        }
        evalVals[2] = positiveExamples.size()-foundPosExa; //fn (if we found all positive examples, good, if we missed one, this will be accounted here)
        LOGGER.info("tp: {}, fp: {}, fn: {}", evalVals[0], evalVals[1], evalVals[2]);
        return evalVals;
    }

    //0=tp, 1=fp, 2=fn
    private int[] evaluateSets(Set<OWLNamedIndividual> expected, Set<OWLNamedIndividual> received) {
        int[] evalVals= new int[3];


        //0=tp, 1=fp, 2=fn
        //tp = |e and r|
        evalVals[0] = Sets.intersection(expected, received).size();
        //fp = |r and !e|
        evalVals[1] = Sets.difference(received, expected).size();
        //fn = |e and !r|
        evalVals[2] = Sets.difference(expected, received).size();

        return evalVals;
    }

    protected Double getConceptLength(OWLClassExpression concept) {
        ConceptLengthCalculator renderer = new ConceptLengthCalculator();
        renderer.render(concept);
        return 1.0*renderer.conceptLength;
    }

    protected OWLClassExpression parseManchesterConcept(String concept){
        //String concept= RabbitMQUtils.readString(data);
        LOGGER.info("Retrieved concept: {}", concept);
        BidirectionalShortFormProvider provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlOnto), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);

        OWLDataFactory dataFactory = new OWLDataFactoryImpl();

        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        return parser.parse(concept);
    }


    private ResultStorage evaluate(){
        if(noOfConcepts==0){
            return ResultStorage.createEmpty();
        }
        double macroRecall=this.summedRecall/noOfConcepts;
        double macroPrecision=this.summedPrecision/noOfConcepts;
        double macroF1 = this.summedF1/noOfConcepts;

        double[] microResults = calculateF1Measure(this.tp, this.fp, this.fn);
        if(conceptLengths.size()==0){
            conceptLengths.add(0.0);
        }
        double[] conceptLengthValues = new double[conceptLengths.size()];
        for(int i=0;i<conceptLengthValues.length;i++){
            conceptLengthValues[i]= conceptLengths.get(i);
        }
        Double avgConceptLength=StatUtils.mean(conceptLengthValues);
        Double maxCL = StatUtils.max(conceptLengthValues);
        Double minCL = StatUtils.min(conceptLengthValues);
        Double fpCL = StatUtils.percentile(conceptLengthValues, 25);
        Double tpCL = StatUtils.percentile(conceptLengthValues, 75);
        Double spCL = StatUtils.percentile(conceptLengthValues, 50);
        Double varCL = StatUtils.variance(conceptLengthValues);

        if(resultTimes.size()==0){
            resultTimes.add(0l);
        }
        double[] resultTimesValues = new double[resultTimes.size()];
        for(int i=0;i<resultTimesValues.length;i++){
            resultTimesValues[i]= resultTimes.get(i);
        }
        Double avgRT = StatUtils.mean(resultTimesValues);
        Double maxRT = StatUtils.max(resultTimesValues);
        Double minRT = StatUtils.min(resultTimesValues);
        Double fpRT = StatUtils.percentile(resultTimesValues, 25);
        Double tpRT = StatUtils.percentile(resultTimesValues, 75);
        Double spRT = StatUtils.percentile(resultTimesValues, 50);
        Double varRT = StatUtils.variance(resultTimesValues);
        ResultStorage result = new ResultStorage(macroF1,macroPrecision,macroRecall,microResults[0],microResults[1],microResults[2],this.errorCount,
                avgConceptLength, maxCL, minCL, fpCL, tpCL, spCL, varCL,
                avgRT, maxRT, minRT, fpRT, tpRT, spRT, varRT);
        return result;
    }

    protected double[] calculateF1Measure(long tp, long fp, long fn) {
        //0=f1, 1=prec, 2=recall
        double precision, recall, f1measure;
        if (tp == 0) {
            if ((fp== 0) && (fn == 0)) {
                // If there haven't been something to find and nothing has been
                // found --> everything is great
                precision = 1.0;
                recall = 1.0;
                f1measure = 1.0;
            } else {
                // The annotator found no correct ones, but made some mistake
                // --> that is bad
                precision = 0.0;
                recall = 0.0;
                f1measure = 0.0;
            }
        } else {
            precision = (double) tp / (double) (tp + fp);
            recall = (double) tp / (double) (tp + fn);
            f1measure = (2 * precision * recall) / (precision + recall);
        }
        return new double[] {f1measure, precision, recall };
    }

    @Override
    protected Model summarizeEvaluation() throws Exception {
        ResultStorage result = evaluate();
        Model model = createDefaultModel();
        Resource experiment = model.getResource(experimentUri);

        model.add(experiment , RDF.type, HOBBIT.Experiment);
        model.addLiteral(experiment, RAKI.noOfConcepts, this.noOfConcepts);

        model.addLiteral(experiment, RAKI.macroPrecision, result.getMacroPrecision());
        model.addLiteral(experiment, RAKI.macroRecall, result.getMacroRecall());
        model.addLiteral(experiment, RAKI.macroF1, result.getMacroF1Measure());
        model.addLiteral(experiment, RAKI.microPrecision, result.getMicroPrecision());
        model.addLiteral(experiment, RAKI.microRecall, result.getMicroRecall());
        model.addLiteral(experiment, RAKI.microF1, result.getMicroF1Measure());
        model.addLiteral(experiment, RAKI.errorCount, result.getErrorCount());

        model.addLiteral(experiment, RAKI.avgConceptLength, result.getAvgConceptLength());
        model.addLiteral(experiment, RAKI.maxConceptLength, result.getMaxConceptLength());
        model.addLiteral(experiment, RAKI.minConceptLength, result.getMinConceptLength());
        model.addLiteral(experiment, RAKI.varianceConceptLength, result.getVarianceConceptLength());
        model.addLiteral(experiment, RAKI.firstPercentileConceptLength, result.getFirstPercentileConceptLength());
        model.addLiteral(experiment, RAKI.secondPercentileConceptLength, result.getSecondPercentileConceptLength());
        model.addLiteral(experiment, RAKI.thirdPercentileConceptLength, result.getThirdPercentileConceptLength());

        model.addLiteral(experiment, RAKI.avgResultTimes, result.getAvgResultTimes());
        model.addLiteral(experiment, RAKI.maxResultTimes, result.getMaxResultTimes());
        model.addLiteral(experiment, RAKI.minResultTimes, result.getMinResultTimes());
        model.addLiteral(experiment, RAKI.varianceResultTimes, result.getVarianceResultTimes());
        model.addLiteral(experiment, RAKI.firstPercentileResultTimes, result.getFirstPercentileResultTimes());
        model.addLiteral(experiment, RAKI.secondPercentileResultTimes, result.getSecondPercentileResultTimes());
        model.addLiteral(experiment, RAKI.thirdPercentileResultTimes, result.getThirdPercentileResultTimes());


        return model;
    }
}
