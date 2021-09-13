package org.dice_group.raki.hobbit.taskgenerator;

import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.ilp.LearningProblemFactory;
import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractSequencingTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * The Task Generator will wait until the system and the evaluation module has loaded the ontology.
 *
 * It will receive a command from each to indicate that the ontology has been loaded.
 *
 * Afterwards the task generator will receive the Task Data (one Learning Problem at a time)
 * and sends that Learning Problem to the System and the evaluation module.
 *
 * if splitRatio is set, the positive uris from the Learning Problem will be split at the given ratio.
 *
 */
public class RakiTaskGenerator extends AbstractSequencingTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RakiTaskGenerator.class);

    //mutexes to wait for before the task generation
    private final Semaphore systemLoadedMutex = new Semaphore(0);
    private final Semaphore evalLoadedMutex = new Semaphore(0);

    // Some parameters to use in the benchmark
    private long seed=0;
    private double splitRatio=1.0;
    private int minExamples=1;


    /**
     * This will additionally check if the System and the Evaluation Module send their command,
     * that they have loaded the Ontology and are ready to go.
     *
     * @param command
     * @param data
     */
    @Override
    public void receiveCommand(byte command, byte[] data) {
        // wait until we know that the system has loaded the ontology
        if (command == CONSTANTS.COMMAND_SYSTEM_LOADED) {
            LOGGER.info("Received signal that system is ready");
            // release the mutex
            systemLoadedMutex.release();
        }
        //wait until we know the evaluation has loaded the ontology
        if (command == CONSTANTS.COMMAND_EVAL_LOADED) {
            LOGGER.info("Received signal that eval is ready");
            // release the mutex
            evalLoadedMutex.release();
        }
        super.receiveCommand(command, data);
    }


    @Override
    public void init() throws Exception {
        super.init();
        Map<String, String> envVariables = System.getenv();

        //Get timeout to use in MS
        int timeOutMS=60000;
        if (envVariables.containsKey(CONSTANTS.TIMEOUT_MS)) {
            String value = envVariables.get(CONSTANTS.TIMEOUT_MS);
            timeOutMS = Integer.parseInt(value);

        }

        //Should the Learning Problem positive examples be split into a smaller subset?
        if (envVariables.containsKey(CONSTANTS.SPLIT_RATIO)) {
            String value = envVariables.get(CONSTANTS.SPLIT_RATIO);
            splitRatio = Double.parseDouble(value);
            if(splitRatio<=0){
                LOGGER.warn("Split Ratio was 0 or smaller, This is not allowed. Split Ratio is set to 1.0 per default");
                splitRatio = 1.0;
            }

        }

        //If splitRatio is set, make sure that at least minExamples are in the set
        if (envVariables.containsKey(CONSTANTS.MIN_EXAMPLES)) {
            String value = envVariables.get(CONSTANTS.MIN_EXAMPLES);
            minExamples = Integer.parseInt(value);
            if(minExamples<=0){
                LOGGER.warn("Minimum examples was smaller 1, This is not allowed. Minimum will be set to 1 per default");
                minExamples = 1;
            }
        }

        //The seed to use
        if (envVariables.containsKey(CONSTANTS.SEED)) {
            String value = envVariables.get(CONSTANTS.SEED);
            seed = Long.parseLong(value);
        }
        setAckTimeout(timeOutMS);
    }


    @Override
    protected void generateTask(byte[] data) throws Exception {
        LOGGER.info("Got task");

        //Wait until the System and eval send their commands that they have loaded the Ontology
        systemLoadedMutex.acquire();
        systemLoadedMutex.release();
        evalLoadedMutex.acquire();
        evalLoadedMutex.release();

        // Create an ID for the task
        String taskId = getNextTaskId();

        //Read the task data as JSON and convert it to a Learning Problem
        String jsonExampleStr = RabbitMQUtils.readString(data);
        LearningProblem lp = LearningProblemFactory.parse(jsonExampleStr);

        //This will contain the Learning Problem as a json representation at the end,
        //This will be send to the system
        String problemToSystem;

        //check if only a portion of the examples should be used and make sure that the ratio is greater 0.
        if(splitRatio<1 && splitRatio > 0) {
            Set<String> positiveRatioed = lp.getSomePositiveUris(splitRatio, minExamples, new Random(seed));
            //clear the positive uris and set them to the ratioed ones
            lp.getPositiveUris().clear();
            lp.getPositiveUris().addAll(positiveRatioed);
        }
        //set to the JSON repr. of the Learning Problem without the gold standard concept
        problemToSystem = lp.asJsonString(false);
        byte[] taskData = RabbitMQUtils.writeString(problemToSystem);

        // Send the task to the system (and store the timestamp)
        long timestamp = System.currentTimeMillis();
        LOGGER.info("Sending task with id {} to system. ", taskId);
        //wait for system to be loaded message
        sendTaskToSystemAdapter(taskId, taskData);

        LOGGER.info("Sending task with id {} to eval storage. ", taskId);
        // Send the expected answer to the evaluation store
        sendTaskToEvalStorage(taskId, timestamp, data);
    }

    @Override
    public void close() throws IOException {
        systemLoadedMutex.release();
        super.close();
    }
}
