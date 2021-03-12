package org.dice_group.raki.hobbit.datagenerator;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileSender;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RakiDataGenerator extends AbstractDataGenerator {

    protected static Logger LOGGER = LoggerFactory.getLogger(RakiDataGenerator.class);

    private static final String PROPERTIES_PREFIX = "org.dice_group.raki.benchmark.";

    private String evalQueueName="DG_2_EVAL_MODULE_QUEUE_NAME";
    private String systemOntQueueName="DG_2_SYSTEM_QUEUE_NAME";

    private JSONArray examples;
    private String ontology;
    private Map<String, String[]> datasets = new HashMap<String, String[]>();

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

        YAMLConfiguration conf = readConfiguration(configFile);
        conf.getKeys().forEachRemaining(x ->{
            LOGGER.info("Got config {}", x);

        });

        //What a bunch of garabage. Note to future: DO NOT USE YamlConfiguration
        List<Object> names = conf.getList("datasets.name");
        List<Object> datasetFiles = conf.getList("datasets.dataset");
        List<Object> learningProblems = conf.getList("datasets.learningProblem");
        for(int i=0;i<names.size();i++){
            datasets.put(names.get(i).toString(), new String[]{
                    datasetFiles.get(i).toString(),
                    learningProblems.get(i).toString()
            });
        }

        LOGGER.info("Got following datasets {}", datasets);
        //from file get org.dice_group.raki.benchmark.+benchmarkName.dataset, posNegExamples, hasConcepts
        String datasetFile = datasets.get(benchmarkName)[0];
        String posNegExamples = datasets.get(benchmarkName)[1];

        //load dataset and pos/neg examples or concepts if available
        examples = readPosNegExamples(posNegExamples);
        ontology=datasetFile;
    }

    private JSONArray readPosNegExamples(String posNegExamples) {
        StringBuilder jsonStr = new StringBuilder();

        try(BufferedReader reader = new BufferedReader(new FileReader(new File(posNegExamples)))){
            String line;
            while((line=reader.readLine())!=null){
                jsonStr.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGGER.error("Could not read pos/neg Examples. ", e);
        }
        return new JSONArray(jsonStr.toString());
    }

    private static YAMLConfiguration readConfiguration(String fileName){
        File propertiesFile = new File(fileName);
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(YAMLConfiguration.class)
                        .configure(params.fileBased()
                                .setFile(propertiesFile));
        try
        {
            return (YAMLConfiguration) builder.getConfiguration();
            // config contains all properties read from the file
        }
        catch(ConfigurationException cex)
        {
            LOGGER.error("Could not read dataset/benchmark property file");
            return null;
        }
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
        examples.forEach(posNegExample -> {
            LOGGER.info("Sending {}. task to system. ", count.getAndIncrement());

            byte[] data = RabbitMQUtils.writeString(posNegExample.toString());
            try {
                LOGGER.info("Sending {}", posNegExample.toString());
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
}
