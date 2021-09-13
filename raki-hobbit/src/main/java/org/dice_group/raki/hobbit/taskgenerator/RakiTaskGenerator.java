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

public class RakiTaskGenerator extends AbstractSequencingTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RakiTaskGenerator.class);

    private final Semaphore systemLoadedMutex = new Semaphore(0);
    private final Semaphore evalLoadedMutex = new Semaphore(0);
    private long seed=0;
    private double splitRatio=1.0;
    private int minExamples=1;


    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == CONSTANTS.COMMAND_SYSTEM_LOADED) {
            LOGGER.info("Received signal that system is ready");
            // release the mutex
            systemLoadedMutex.release();
        }
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
        int timeOutMS=60000;
        if (envVariables.containsKey(CONSTANTS.TIMEOUT_MS)) {
            String value = envVariables.get(CONSTANTS.TIMEOUT_MS);
            timeOutMS = Integer.parseInt(value);

        }
        if (envVariables.containsKey(CONSTANTS.SPLIT_RATIO)) {
            String value = envVariables.get(CONSTANTS.SPLIT_RATIO);
            splitRatio = Double.parseDouble(value);
            if(splitRatio<=0){
                LOGGER.warn("Split Ratio was 0 or smaller, This is not allowed. Split Ratio is set to 1.0 per default");
                splitRatio = 1.0;
            }

        }
        if (envVariables.containsKey(CONSTANTS.MIN_EXAMPLES)) {
            String value = envVariables.get(CONSTANTS.MIN_EXAMPLES);
            minExamples = Integer.parseInt(value);
            if(minExamples<=0){
                LOGGER.warn("Minimum examples was smaller 1, This is not allowed. Minimum will be set to 1 per default");
                minExamples = 1;
            }
        }
        if (envVariables.containsKey(CONSTANTS.SEED)) {
            String value = envVariables.get(CONSTANTS.SEED);
            seed = Long.parseLong(value);
        }
        setAckTimeout(timeOutMS);
    }


    @Override
    protected void generateTask(byte[] data) throws Exception {
        LOGGER.info("Got task");
        systemLoadedMutex.acquire();
        systemLoadedMutex.release();
        evalLoadedMutex.acquire();
        evalLoadedMutex.release();
        // Create an ID for the task
        String taskId = getNextTaskId();

        String jsonExampleStr = RabbitMQUtils.readString(data);
        LearningProblem lp = LearningProblemFactory.parse(jsonExampleStr);

        String problemToSystem;
        //check if only a portion of the examples should be used and make sure that the ratio is greater 0.
        if(splitRatio<1 && splitRatio > 0) {
            Set<String> positiveRatioed = lp.getSomePositiveUris(splitRatio, minExamples, new Random(seed));
            lp.getPositiveUris().clear();
            lp.getPositiveUris().addAll(positiveRatioed);
        }
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
