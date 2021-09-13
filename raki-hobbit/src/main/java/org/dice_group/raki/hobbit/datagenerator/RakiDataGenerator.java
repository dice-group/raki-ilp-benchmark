package org.dice_group.raki.hobbit.datagenerator;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dice_group.raki.core.config.Configuration;
import org.dice_group.raki.core.config.Configurations;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.ilp.LearningProblemFactory;
import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Data generator reads the Ontology and learning problems for a benchmark configuration.
 * and sends the Ontology to the System adapter and the Evaluation
 * It will send a command to both after finishing sending the Ontology, so the system and evaluation module can start receiving correctly.
 *
 * After that it will send each learning problem to the Task Generator.
 *
 */
public class RakiDataGenerator extends AbstractDataGenerator {

    protected static Logger LOGGER = LoggerFactory.getLogger(RakiDataGenerator.class);

    private String evalQueueName="DG_2_EVAL_MODULE_QUEUE_NAME";
    private String systemOntQueueName="DG_2_SYSTEM_QUEUE_NAME";

    private Set<LearningProblem> learningProblems;
    private String ontology;

    @Override
    public void init() throws Exception {
        super.init();
        if (System.getenv().containsKey(CONSTANTS.ONTOLOGY_QUEUE_NAME)) {
            evalQueueName = System.getenv().get(CONSTANTS.ONTOLOGY_QUEUE_NAME);
        }
        if (System.getenv().containsKey(CONSTANTS.ONTOLOGY_2_SYSTEM_QUEUE_NAME)) {
            systemOntQueueName = System.getenv().get(CONSTANTS.ONTOLOGY_2_SYSTEM_QUEUE_NAME);
        }
        LOGGER.info("Queue names {}, {}", evalQueueName,systemOntQueueName);
        //get Benchmark name -> dataset & corresponding
        String benchmarkName="";
        if (System.getenv().containsKey(CONSTANTS.BENCHMARK_NAME)) {
            benchmarkName = System.getenv().get(CONSTANTS.BENCHMARK_NAME);
        }
        LOGGER.info("Got following benchmark name {}", benchmarkName);

        String configFile ="/raki/benchmark.yaml";
        LOGGER.info("Reading datasets config file {}", configFile);

        //load configuration from config file with the name=benchmarkName
        Configuration config = Configurations.load(new File(configFile)).getConfiguration(benchmarkName);

        //make sure that the config is not null
        if(config == null){
            LOGGER.error("Couldn't find Configuration with name {}", benchmarkName);
            System.exit(1);
        }

        //check if lps exists
        if(!new File(config.getLearningProblem()).exists()){
            LOGGER.error("Couldn't find Learning Problem file at {}", config.getLearningProblem());
            System.exit(1);
        }
        //read all learning problems from the file
        learningProblems = LearningProblemFactory.readMany(new File(config.getLearningProblem()));

        //check if ontology file exists
        if(!new File(config.getDataset()).exists()){
            LOGGER.error("Couldn't find Ontology Dataset file at {}", config.getDataset());
            System.exit(1);
        }
        ontology = config.getDataset();
    }



    @Override
    protected void generateData() throws Exception {
        //First we send the ontology to the system
        LOGGER.info("Sending ontology to system. "+ Instant.now());
        sendOntologyToSystem();
        //let the system know we fully send the ontology
        sendToCmdQueue(CONSTANTS.COMMAND_ONTO_FULLY_SEND_SYSTEM);
        // Now we send the ontology to the evaluation module
        LOGGER.info("Sending ontology to eval. "+ Instant.now());
        sendOntologyToEval();
        //let the eval know we fully send the ontology
        sendToCmdQueue(CONSTANTS.COMMAND_ONTO_FULLY_SEND);

        LOGGER.debug("Sending now tasks to task generator. ");
        //send Learning Problems to task generator
        AtomicInteger count= new AtomicInteger(1);
        learningProblems.forEach(learningProblem -> {
            LOGGER.info("Sending {}. task to system. ", count.getAndIncrement());
            //convert LP to json, We want to add the concepts here if there are any, so the TG can handle them
            byte[] data = RabbitMQUtils.writeString(learningProblem.asJsonString(true));
            try {
                sendDataToTaskGenerator(data);
            } catch (IOException e) {
                LOGGER.error("Couldn't send data to task generator. ", e);
            }
        });

    }

    /**
     * Sends the Ontology to the evaluation module
     *
     * @throws IOException
     */
    private void sendOntologyToEval() throws IOException {
        SimpleFileSender sender = SimpleFileSender.create(this.outgoingDataQueuefactory, this.evalQueueName);

        sendOntologyToQueue(sender, this.evalQueueName);
    }

    /**
     * Sends the Ontology to the system adapter
     *
     * @throws IOException
     */
    private void sendOntologyToSystem() throws IOException {
        //just sending one byte so the system triggers, and we can send to the correct queue <- no idea why we need that
        SimpleFileSender sender = SimpleFileSender.create(this.outgoingDataQueuefactory, this.systemOntQueueName);

        sendDataToSystemAdapter(new byte[]{1});

        sendOntologyToQueue(sender, this.systemOntQueueName);
        //send to the actual queue
    }

    /**
     * this will send the Ontology to the queue over the provided sender
     *
     * @param sender
     * @param queue
     * @throws IOException
     */
    private void sendOntologyToQueue(SimpleFileSender sender, String queue) throws IOException {
        // define a queue name, e.g., read it from the environment

        LOGGER.info("Queue Name: {}", queue);
        // create the sender

        InputStream is = null;
        try {
            // create input stream, e.g., by opening a file
            LOGGER.info("Ontology size: {}", FileUtils.sizeOf(new File(ontology)));
            is = new FileInputStream(ontology);
            // send data
            sender.streamData(is, "ontology");
        } catch (Exception e) {
            // handle exception
            LOGGER.error("Couldn't send ontology to queue {}. ", queue);
        } finally {
            IOUtils.closeQuietly(is);
        }

        // close the sender
        IOUtils.closeQuietly(sender);
    }

    @Override
    public void close() throws IOException {

        super.close();
    }
}
