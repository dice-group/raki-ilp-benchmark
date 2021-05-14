package org.dice_group.raki.hobbit.taskgenerator;

import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractSequencingTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class RakiTaskGenerator extends AbstractSequencingTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RakiTaskGenerator.class);


    private Semaphore systemLoadedMutex = new Semaphore(0);
    private Semaphore evalLoadedMutex = new Semaphore(0);


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
        Integer timeOutMS=60000;
        if (envVariables.containsKey(CONSTANTS.TIMEOUT_MS)) {
            String value = envVariables.get(CONSTANTS.TIMEOUT_MS);
            timeOutMS = Integer.parseInt(value);

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
        JSONObject posNegExample = new JSONObject(jsonExampleStr);
        posNegExample.remove("concept");

        byte[] taskData = RabbitMQUtils.writeString(posNegExample.toString());

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
