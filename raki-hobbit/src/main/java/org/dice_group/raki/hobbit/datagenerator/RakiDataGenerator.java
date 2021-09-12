package org.dice_group.raki.hobbit.datagenerator;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dice_group.raki.core.config.Configuration;
import org.dice_group.raki.core.config.Configurations;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.ilp.LearningProblemFactory;
import org.dice_group.raki.core.commons.CONSTANTS;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

        //load Properties file
        String configFile ="/raki/benchmark.yaml";
        LOGGER.info("Reading datasets config file {}", configFile);

        Configuration config = Configurations.load(new File(configFile)).getConfiguration(benchmarkName);
        if(config == null){
            LOGGER.error("Couldn't find Configuration with name {}", benchmarkName);
            System.exit(1);
        }
        //check if lps exists
        if(!new File(config.getLearningProblem()).exists()){
            LOGGER.error("Couldn't find Learning Problem file at {}", config.getLearningProblem());
            System.exit(1);
        }
        learningProblems = LearningProblemFactory.readMany(new File(config.getLearningProblem()));
        //check if onto exists
        if(!new File(config.getDataset()).exists()){
            LOGGER.error("Couldn't find Ontology Dataset file at {}", config.getDataset());
            System.exit(1);
        }
        ontology= config.getDataset();
    }



    @Override
    protected void generateData() throws Exception {
        LOGGER.info("Sending ontology to system. "+ Instant.now());
        sendOntologyToSystem();
        sendToCmdQueue(CONSTANTS.COMMAND_ONTO_FULLY_SEND_SYSTEM);
        LOGGER.info("Sending ontology to eval. "+ Instant.now());
        sendOntologyToEval();
        sendToCmdQueue(CONSTANTS.COMMAND_ONTO_FULLY_SEND);

        LOGGER.debug("Sending now tasks to task generator. ");
        //send examples to task generator
        AtomicInteger count= new AtomicInteger(1);
        learningProblems.forEach(learningProblem -> {
            LOGGER.info("Sending {}. task to system. ", count.getAndIncrement());
            byte[] data = RabbitMQUtils.writeString(learningProblem.asJsonString(true));
            try {
                sendDataToTaskGenerator(data);
            } catch (IOException e) {
                LOGGER.error("Couldn't send data to task generator. ", e);
            }
        });

    }

    private void sendOntologyToEval() throws IOException {
        SimpleFileSender sender = SimpleFileSender.create(this.outgoingDataQueuefactory, this.evalQueueName);

        sendOntologyToQueue(sender, this.evalQueueName);
    }

    private void sendOntologyToSystem() throws IOException {
        //just sending one byte so the system triggers and we can send to the correct queue
        SimpleFileSender sender = SimpleFileSender.create(this.outgoingDataQueuefactory, this.systemOntQueueName);

        sendDataToSystemAdapter(new byte[]{1});

        sendOntologyToQueue(sender, this.systemOntQueueName);
        //send to the actual queue
    }

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
