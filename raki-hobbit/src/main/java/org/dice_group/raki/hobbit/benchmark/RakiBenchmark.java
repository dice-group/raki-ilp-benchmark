package org.dice_group.raki.hobbit.benchmark;

import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.raki.hobbit.core.commons.CONSTANTS;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RakiBenchmark extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RakiBenchmark.class);

    private static final String EVALUATION_MODULE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakievaluationmodule";
    private static final String TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakitaskgenerator";
    private static final String DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakidatagenerator";

    private long timeOutMS=60000;


    @Override
    public void init() throws Exception {
        super.init();
        Resource newExpResource = benchmarkParamModel.getResource(Constants.NEW_EXPERIMENT_URI);

        NodeIterator iterator = benchmarkParamModel
                .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "benchmarkName"));
        String datasetName = "";
        if(iterator.hasNext()){
            datasetName = iterator.next().asResource().toString();
        }
        iterator = benchmarkParamModel
                .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "timeOutMS"));
        if(iterator.hasNext())
            timeOutMS = iterator.next().asLiteral().getLong();

        boolean useConcepts=false;


        int minExamples=1;
        iterator = benchmarkParamModel
                .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "minExamples"));
        if(iterator.hasNext())
            minExamples = iterator.next().asLiteral().getInt();

        long seed=1;
        iterator = benchmarkParamModel
                .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "seed"));
        if(iterator.hasNext())
            seed = iterator.next().asLiteral().getLong();

        double splitRatio=1.0;
        iterator = benchmarkParamModel
                .listObjectsOfProperty(benchmarkParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "splitRatio"));
        if(iterator.hasNext())
            splitRatio = iterator.next().asLiteral().getDouble();

        //CREATE TASK DATA etc here
        String ontToSystemQueueName = generateSessionQueueName("ontologyToSystemQueue");

        LOGGER.info("Benchmark uses: benchmark={}, timeoutMS={}", datasetName, timeOutMS);
        String ontToEvalQueueName = generateSessionQueueName("ontologyToEvalQueue");

        createEvaluationModule(EVALUATION_MODULE_CONTAINER_IMAGE,
                new String[] { CONSTANTS.ONTOLOGY_QUEUE_NAME+"="+ontToEvalQueueName, CONSTANTS.USE_CONCEPTS+"="+useConcepts });

        createDataGenerators(DATA_GENERATOR_CONTAINER_IMAGE, 1,
                new String[] { CONSTANTS.BENCHMARK_NAME+"="+datasetName, CONSTANTS.ONTOLOGY_QUEUE_NAME+"="+ontToEvalQueueName,
                        CONSTANTS.ONTOLOGY_2_SYSTEM_QUEUE_NAME+"="+ontToSystemQueueName});
        LOGGER.info("Finished creating Data Generator");

        createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, 1, new String[] {
                CONSTANTS.TIMEOUT_MS+"="+timeOutMS, CONSTANTS.SEED+"="+seed, CONSTANTS.MIN_EXAMPLES+"="+minExamples, CONSTANTS.SPLIT_RATIO+"="+splitRatio});
        LOGGER.info("Finished creating Task Generator");

        createEvaluationStorage(DEFAULT_EVAL_STORAGE_IMAGE,
                new String[] { Constants.ACKNOWLEDGEMENT_FLAG_KEY + "=true", DEFAULT_EVAL_STORAGE_PARAMETERS[0] });




        // start the evaluation module
        waitForComponentsToInitialize();
    }



    @Override
    protected void executeBenchmark() throws Exception {
        // give the start signals
        String ontToEvalQueueName = generateSessionQueueName("ontologyToEvalQueue");

        sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);
        sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
        // wait for the data generators to finish their work


        LOGGER.info("Finished creating Modules");
        waitForDataGenToFinish();

        // wait for the task generators to finish their work
        waitForTaskGenToFinish();

        waitForSystemToFinish();

        sendToCmdQueue(CONSTANTS.COMMAND_EVAL_START);
        // wait for the evaluation to finish
        waitForEvalComponentsToFinish();
        // the evaluation module should have sent an RDF model containing the
        // results. We should add the configuration of the benchmark to this
        // model.
        // FIXME add parameters
        // this.resultModel.add(null);
        LOGGER.info("Results: {}", this.resultModel);
        //this.resultModel.add(benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.createProperty("http://w3id.org/hobbit/vocab#involvesSystemInstance")).next().asResource(), RDF.type, HOBBIT.System);
        sendResultModel(this.resultModel);
    }
}