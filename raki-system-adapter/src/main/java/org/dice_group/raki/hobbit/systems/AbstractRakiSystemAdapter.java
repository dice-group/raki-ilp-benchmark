package org.dice_group.raki.hobbit.systems;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.NodeIterator;
import org.dice_group.raki.hobbit.commons.CONSTANTS;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public abstract class AbstractRakiSystemAdapter extends AbstractSystemAdapter {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractRakiSystemAdapter.class);

    protected long timeOutMs=60000;
    public abstract String createConcept(String posNegExample) throws IOException, Exception;
    public abstract void loadOntology(File ontology) throws IOException, Exception;
    private SimpleFileReceiver receiver = null;

    @Override
    public void receiveCommand(byte command, byte[] data) {
        if(command == CONSTANTS.COMMAND_ONTO_FULLY_SEND_SYSTEM){
            if(receiver!=null){
                receiver.terminate();

            }
            else{
                LOGGER.error("Receiver cannot be terminated at this point. ");
            }
        }
        super.receiveCommand(command, data);
    }

    @Override
    public void init() throws Exception {
        super.init();
        String queueName = generateSessionQueueName("ontologyToSystemQueue");
        LOGGER.info("Queue Name {} "+ Instant.now(), queueName);
        receiver = SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);
        NodeIterator iterator = systemParamModel
                .listObjectsOfProperty(systemParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "timeOutMS"));
        if(iterator.hasNext()){
            timeOutMs = iterator.next().asLiteral().getLong();
        }
        LOGGER.info("Timeout set to {} ms", timeOutMs);
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        // we don't use this one per se as we may expect datasets>1GB
        byte d=0;
        if(data!=null && data.length>0){
            d=data[0];
        }
        LOGGER.info("Trying to receive generated data now.{}", d);

        try {
            LOGGER.info("Created queue.");
            String[] receivedFiles = receiver.receiveData("/raki/tempOntologyDirSystem/");
            LOGGER.info("Received {} Files ", receivedFiles.length);
            //IOUtils.closeQuietly(this.incomingDataQueueFactory.createDefaultRabbitQueue(queueName));
            //FIXME for now assume we only use one Ontology file
            LOGGER.info("Loading Ontology now.");
            loadOntology(new File("/raki/tempOntologyDirSystem/"+receivedFiles[0]));
            LOGGER.info("Ontology loading done.");
            //We have to tell the controller/tg somehow that we are ready
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve Ontology.", e);
        }
        LOGGER.debug("Sending System Loaded command.");
        try {
            sendToCmdQueue(CONSTANTS.COMMAND_SYSTEM_LOADED);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        LOGGER.info("Retrieved task with id {}. ", taskId);
        String posNegExamples = RabbitMQUtils.readString(data);
        //empty string will be considered as an error
        String concept = "";
        try {
            concept = createConcept(posNegExamples);
        } catch (Exception e) {
            LOGGER.error("Concept couldn't be created. ", e);
        }
        try {
            LOGGER.info("Sending concept {} now", concept);
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(concept));
            LOGGER.info("sended concept");
        } catch (IOException e) {
            //Log the error.
            LOGGER.error("Problem sending results to eval storage. ", e);
        }
    }
}
