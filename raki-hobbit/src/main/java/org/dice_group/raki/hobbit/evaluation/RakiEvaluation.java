package org.dice_group.raki.hobbit.evaluation;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.dice_group.raki.core.evaluation.Evaluator;
import org.dice_group.raki.core.evaluation.ResultContainer;
import org.dice_group.raki.core.evaluation.f1measure.F1Result;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.ilp.LearningProblemFactory;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.hobbit.vocab.HOBBIT;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * This is The Evaluation Module for the RAKI ILP benchmkark in Hobbit
 *
 * It receives the ontology to use, and evaluates the Learning Problem against the Concept (in manchester syntax)
 * from the system for that Learning Problem.
 *
 * It generates the macro and micro F1 Measures as well as some basic statistics over the concept lengths and the result times
 * for each task.
 */
public class RakiEvaluation extends AbstractEvaluationModule {

    protected static Logger LOGGER = LoggerFactory.getLogger(RakiEvaluation.class);

    //Stuff for the file ontology file retrieval
    private String queueName="DG_2_EVAL_MODULE_QUEUE_NAME";
    private SimpleFileReceiver receiver = null;

    private Boolean useConcepts = false;
    protected OWLOntology ontology;
    private Evaluator evaluator;

    //Mutex to use to tell evaluation to start (after Task Generator and Systems are finished, there are some weird problems otherwise)
    private final Semaphore evalStartMutex = new Semaphore(0);

    // Results
    protected List<Double> conceptLengths =new ArrayList<>();
    protected List<Long> resultTimes =new ArrayList<>();
    protected int errorCount=0;
    protected long noOfConcepts=0;


    /**
     * This will first wait until the Ontology was fully send to release the receiver.
     * This is needed to assure that the evaluation won't ignore the ontology.
     *
     * After that it waits until the Controller will send the Eval start command.
     * The evaluation module will then work as usual and receives the tasks and system responses.
     *
     * @param command
     * @param data
     */
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

        //Get the queue name to receive the ontology through
        if (System.getenv().containsKey(CONSTANTS.ONTOLOGY_QUEUE_NAME)) {
                queueName = System.getenv().get(CONSTANTS.ONTOLOGY_QUEUE_NAME);
        }

        //If gold standard concepts should be used instead of examples.
        if (System.getenv().containsKey(CONSTANTS.USE_CONCEPTS)) {
            useConcepts = Boolean.parseBoolean(System.getenv().get(CONSTANTS.USE_CONCEPTS));
        }

        LOGGER.info("Eval module queue name {}, useConcepts {}", queueName, useConcepts);
        LOGGER.info("receiving ontology "+ Instant.now());

        //Receiving the Ontology now
        receiver= SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);

        //we know, that at this point, the onto was fully send, because we waited until CONSTANTS.COMMAND_ONTO_FULLY_SEND
        String[] receivedFiles = receiver.receiveData("/raki/tempOntologyDirEval/");

        LOGGER.info("received ontology {}", Objects.requireNonNull(new File("/raki/tempOntologyDirEval/").listFiles()).length);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // We might get multiple files, thus we add each one as a standalone ontology.
        // Be aware this is more for future purposes.
        for(File f : (Objects.requireNonNull(new File("/raki/tempOntologyDirEval/").listFiles()))){
            LOGGER.debug("file {}", f.getAbsolutePath());
            LOGGER.debug("Size of recv ont: {}", f.length());
            OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();

            //load the ontology and print out the axioms
            ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(f), loaderConfig);
            LOGGER.info("Axioms in Ontology: {}" , ontology.getAxiomCount());
        }

        //Load the OWL base Ontology. This is sometimes needed, to allow owl:Thing and such.
        //the ontology will be added to the Hobbit container in the dockerfile
        OWLOntology owlOnto = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("/raki/owl.ttl"));

        //Create the core evaluator
        evaluator = new Evaluator(ontology, owlOnto, useConcepts);

        //The eval has at this point loaded the ontology fully and can be executed.
        LOGGER.debug("Sending Eval Loaded command.");
        try {
            sendToCmdQueue(CONSTANTS.COMMAND_EVAL_LOADED);
        } catch (IOException e) {
            LOGGER.error("Error while sending command that eval is loaded", e);
        }
    }


    @Override
    protected void collectResponses() throws Exception {
        // We have to make sure, that the collectResponse method waits until we actually have responses
        // otherwise the evaluation module will just stop immediately. So we wait until we get the signal in the receiveCommands method
        evalStartMutex.acquire();
        super.collectResponses();
        evalStartMutex.release();
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp, long responseReceivedTimestamp) throws Exception {
        //Add the no of concepts
        noOfConcepts++;
        // Add the time it took
        this.resultTimes.add(responseReceivedTimestamp-taskSentTimestamp);
        //Create the Learning Problem from the expected data (this is a json learning problem)
        LearningProblem lp = LearningProblemFactory.parse(RabbitMQUtils.readString(expectedData));
        try {
            if(receivedData.length==0){
                LOGGER.error("Concept Length is 0. Defined as error.");
                errorCount++;
                ResultContainer container = evaluator.evaluate(lp, Sets.newHashSet(), 0, "ERROR");
                container.setResultTimeMs(responseReceivedTimestamp-taskSentTimestamp);
            }
            else {

                //read the concept (in manchester syntax)
                String concept = RabbitMQUtils.readString(receivedData);

                //Be aware, we do not need to save the f1 measures for macro and micro, the evaluator will take care of that.
                ResultContainer container = evaluator.evaluate(lp,concept);
                container.setResultTimeMs(responseReceivedTimestamp-taskSentTimestamp);

                //ad concept lengths
                this.conceptLengths.add(Integer.valueOf(container.getConceptLength()).doubleValue());
            }
        }catch (Exception e){
            LOGGER.error("Found error while evaluating. ", e);
            this.errorCount++;
        }
    }


    /**
     * This function will add all the metrics to one storage.
     * This includes the stats for result times and concept lengths,
     * as well as the Macro and Micro F1 Measures.
     *
     * @return The Result Storage containing all metrics
     */
    private ResultStorage summarize(){
        if(noOfConcepts==0){
            return ResultStorage.createEmpty();
        }
        if(conceptLengths.size()==0){
            conceptLengths.add(0.0);
        }
        Double[] conceptLengthStats = getStats(conceptLengths);

        if(resultTimes.size()==0){
            resultTimes.add(0L);
        }
        Double[] resultTimeStats =  getStats(resultTimes);
        F1Result macro = evaluator.getMacroF1Measure();
        F1Result micro = evaluator.getMicroF1Measure();
        return new ResultStorage(macro.getF1measure(), macro.getPrecision(), macro.getRecall(),
                micro.getF1measure(), micro.getPrecision(), micro.getRecall(), this.errorCount,
                conceptLengthStats[0], conceptLengthStats[1], conceptLengthStats[2], conceptLengthStats[3], conceptLengthStats[4], conceptLengthStats[5],conceptLengthStats[6],
                resultTimeStats[0], resultTimeStats[1], resultTimeStats[2], resultTimeStats[3], resultTimeStats[4], resultTimeStats[5],resultTimeStats[6]);
    }

    /**
     * Creates stats over the results.
     * Including
     * * average
     * * maximum
     * * minimum
     * * first percentile
     * * third percentile
     * * second percentile
     * * Variance
     *
     * In that order.
     *
     * @param results The numbers to create the statistics over
     * @return The above described stats
     */
    private Double[] getStats(List<? extends Number> results){
        double[] values = new double[results.size()];
        for(int i=0;i<values.length;i++){
            values[i]= results.get(i).doubleValue();
        }
        double avg = StatUtils.mean(values);
        double max = StatUtils.max(values);
        double min = StatUtils.min(values);
        double fp = StatUtils.percentile(values, 25);
        double tp = StatUtils.percentile(values, 75);
        double sp = StatUtils.percentile(values, 50);
        double var = StatUtils.variance(values);
        return new Double[]{avg, max, min, fp, tp, sp, var};
    }


    @Override
    protected Model summarizeEvaluation() throws Exception {
        ResultStorage result = summarize();
        evaluator.printTable();
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
