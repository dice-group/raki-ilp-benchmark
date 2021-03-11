package org.dice_group.raki.hobbit.systems;

import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class AbstractRakiSystemAdapter extends AbstractSystemAdapter {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractRakiSystemAdapter.class);

    protected long timeOutMs=60000;
    public abstract String createConcept(String posNegExample) throws IOException, Exception;
    public abstract void loadOntology(File ontology) throws IOException, Exception;


    @Override
    public void receiveGeneratedData(byte[] data) {
        // we don't use this one per se as we may expect datasets>1GB
        SimpleFileReceiver receiver = null;
        try {
            receiver = SimpleFileReceiver.create(this.incomingDataQueueFactory, generateSessionQueueName("ontologyToSystemQueue"));
            String[] receivedFiles = receiver.receiveData("tempOntologyDir");
            //FIXME for now assume we only use one Ontology file
            LOGGER.info("Loading Ontology now.");
            loadOntology(new File(receivedFiles[0]));
            LOGGER.info("Ontology loading done.");
            //We have to tell the controller/tg somehow that we are ready
            LOGGER.debug("Sending System Loaded command.");
            sendToCmdQueue(CONSTANTS.COMMAND_SYSTEM_LOADED);
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve Ontology.", e);
        }
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        LOGGER.debug("Retrieved task with id {}. ", taskId);
        String posNegExamples = RabbitMQUtils.readString(data);
        LOGGER.debug("Examples: {} ", posNegExamples);
        //empty string will be considered as an error
        String concept = "";
        try {
            concept = createConcept(posNegExamples);
        } catch (Exception e) {
            LOGGER.error("Concept couldn't be created. ", e);
        }
        try {
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(concept));
        } catch (IOException e) {
            //Log the error.
            LOGGER.error("Problem sending results to eval storage. ", e);
        }
    }
}
