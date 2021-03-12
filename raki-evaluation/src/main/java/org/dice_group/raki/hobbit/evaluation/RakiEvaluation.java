package org.dice_group.raki.hobbit.evaluation;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.hobbit.vocab.HOBBIT;
import org.json.JSONObject;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.parser.DLSyntaxParser;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.impl.DefaultNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import org.dice_group.raki.hobbit.commons.CONSTANTS;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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



    public void receiveCommand(byte command, byte[] data) {
        if(command == CONSTANTS.COMMAND_ONTO_FULLY_SEND){
            if(receiver!=null){
                receiver.terminate();
            }
        }
        super.receiveCommand(command, data);
    }


    @Override
    protected void collectResponses() throws Exception {
        String[] receivedFiles = receiver.receiveData("/raki/tempOntologyDirEval/");
        //IOUtils.closeQuietly(this.incomingDataQueueFactory.createDefaultRabbitQueue(queueName));

        LOGGER.info("received ontology {}", new File("/raki/tempOntologyDirEval/").listFiles().length);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        for(File f : (new File("/raki/tempOntologyDirEval/").listFiles())){
            LOGGER.info("file {}", f.getAbsolutePath());
            LOGGER.info("Size of recv ont: {}", f.length());
            ontology= manager.loadOntologyFromOntologyDocument(f);
            LOGGER.info("Axioms in Ontology: {}" , ontology.getAxiomCount());
        }
        if(receivedFiles.length>1){
            ontology = new OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create("http://raki.merged.ontology/"));
        }
        super.collectResponses();
    }

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("Starting eval module");
        if (System.getenv().containsKey(CONSTANTS.ONTOLOGY_QUEUE_NAME)) {
                queueName = System.getenv().get(CONSTANTS.ONTOLOGY_QUEUE_NAME);
        }
        LOGGER.info("Eval module queue name {}", queueName);
        LOGGER.info("receiving ontology "+ Instant.now());
        receiver= SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);


    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp, long responseReceivedTimestamp) throws Exception {
        //convert data 2 concept
        LOGGER.info("Recv an eval ");
        try {
            noOfConcepts++;
            Reasoner hermit = new Reasoner(ontology);
            NodeSet<OWLNamedIndividual> receivedSet = new OWLNamedIndividualNodeSet();
            if(receivedData.length==0){
                LOGGER.error("Concept Length is 0. Defined as error.");
                errorCount++;
                this.conceptLengths.add(0.0);
            }
            else {
                try {
                    OWLClassExpression receivedConcept = parseManchesterConcept(receivedData);
                    LOGGER.info("Received concept is: {}.", receivedConcept);
                    //OWLOntology ont = new OWLOntologyImpl(ontology);
                    this.conceptLengths.add(getConceptLength(receivedConcept));

                    receivedSet = hermit.getInstances(receivedConcept);
                }catch(Exception e){
                    LOGGER.error("Found error while parsing concept. ", e);
                    this.errorCount++;
                }
            }
            //evaluate and add to tp,fp,fn count, as well as f1,recall, precision
            //0=tp, 1=fp, 2=fn
            int[] evalVals = evaluate(expectedData, receivedSet, hermit);
            this.tp += evalVals[0];
            this.fp += evalVals[1];
            this.fn += evalVals[2];
            //0=f1, 1=prec, 2=recall
            double[] f1 = calculateF1Measure(tp, fp, fn);
            this.summedF1 += f1[0];
            this.summedPrecision += f1[1];
            this.summedRecall += f1[2];
            this.resultTimes.add(responseReceivedTimestamp-taskSentTimestamp);
            LOGGER.info("Got {} {} {} {} {} {}" , tp,fp,fn,f1[0], f1[1], f1[2]);
        }catch (Exception e){
            LOGGER.error("Found error while evaluating. ", e);
            this.errorCount++;
        }
    }


    private int[] evaluate(byte[] expectedData, NodeSet<OWLNamedIndividual> receivedSet, Reasoner hermit){
        String expectedPosNeg = RabbitMQUtils.readString(expectedData);

        JSONObject posNegJson = new JSONObject(expectedPosNeg);
        if(posNegJson.has("concept")) {
            OWLClassExpression expectedConcept = parseManchesterConcept(expectedData);
            LOGGER.info("expected concept {}", expectedConcept);
            NodeSet<OWLNamedIndividual> expectedSet = hermit.getInstances(expectedConcept);
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


    private int[] evaluatePosNeg(Set<String> positiveExamples, Set<String> negativeExamples, NodeSet<OWLNamedIndividual> receivedSet) {
        int[] evalVals= new int[]{0,0,0};
        Set<OWLNamedIndividual> received = receivedSet.getFlattened();
        int foundPosExa=0;
        for(OWLNamedIndividual individual : received){
            String individualStr = individual.toString();
            if(positiveExamples.contains(individualStr)){
                evalVals[0]++; //tp ++
                foundPosExa++;
            }
            else if(negativeExamples.contains(individualStr)){
                evalVals[1]++; //fp ++
            }
        }
        evalVals[2] = positiveExamples.size()-foundPosExa; //fn (if we found all positive examples, good, if we missed one, this will be accounted here)
        return evalVals;
    }

    //0=tp, 1=fp, 2=fn
    private int[] evaluateSets(NodeSet<OWLNamedIndividual> expectedSet, NodeSet<OWLNamedIndividual> receivedSet) {
        int[] evalVals= new int[3];
        Set<OWLNamedIndividual> expected = expectedSet.getFlattened();
        Set<OWLNamedIndividual> received = receivedSet.getFlattened();

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

    protected OWLClassExpression parseManchesterConcept(byte[] data){
        String concept= RabbitMQUtils.readString(data);
        LOGGER.info("Retrieved concept: {}", concept);
        BidirectionalShortFormProvider provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);
        OWLDataFactory dataFactory = new OWLDataFactoryImpl();

        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        return parser.parse(concept);
    }

    private OWLClassExpression parseConcept(byte[] data){
        String concept= RabbitMQUtils.readString(data);
        DLSyntaxParser parser2 = new DLSyntaxParser(concept);
        return parser2.parseClassDescription();
    }

    private ResultStorage evaluate(){
        double macroRecall=this.summedRecall/noOfConcepts;
        double macroPrecision=this.summedPrecision/noOfConcepts;
        double macroF1 = this.summedF1/noOfConcepts;

        double[] microResults = calculateF1Measure(this.tp, this.fp, this.fn);
        double[] conceptLengthValues = new double[conceptLengths.size()];
        for(int i=0;i<conceptLengthValues.length;i++){
            conceptLengthValues[i]= conceptLengths.get(i);
        }
        double avgConceptLength=StatUtils.mean(conceptLengthValues);
        double maxCL = StatUtils.max(conceptLengthValues);
        double minCL = StatUtils.min(conceptLengthValues);
        double fpCL = StatUtils.percentile(conceptLengthValues, 0.25);
        double tpCL = StatUtils.percentile(conceptLengthValues, 0.75);
        double spCL = StatUtils.percentile(conceptLengthValues, 0.5);
        double varCL = StatUtils.variance(conceptLengthValues);

        double[] resultTimesValues = new double[resultTimes.size()];
        for(int i=0;i<resultTimesValues.length;i++){
            resultTimesValues[i]= resultTimes.get(i);
        }
        double avgRT=StatUtils.mean(resultTimesValues);
        double maxRT = StatUtils.max(resultTimesValues);
        double minRT = StatUtils.min(resultTimesValues);
        double fpRT = StatUtils.percentile(resultTimesValues, 0.25);
        double tpRT = StatUtils.percentile(resultTimesValues, 0.75);
        double spRT = StatUtils.percentile(resultTimesValues, 0.5);
        double varRT = StatUtils.variance(resultTimesValues);
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
