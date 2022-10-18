package org.dice_group.raki.hobbit.system;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * THe abstract raki system adapter
 */
public abstract class AbstractRakiSystemAdapter extends AbstractSystemAdapter {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractRakiSystemAdapter.class);


    protected int delta = 60_000;
    protected double deltaRatio=1.01;
    protected int timeOutms=60_000;
    public abstract String createConcept(String posNegExample) throws Exception;
    public abstract void loadOntology(File ontology) throws Exception;
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

    public void releaseMutexes(){

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
            timeOutms = iterator.next().asLiteral().getInt();
        }

        iterator = systemParamModel
                .listObjectsOfProperty(systemParamModel.getProperty(CONSTANTS.RAKI2_PREFIX + "delta"));
        if(iterator.hasNext()){
            delta = iterator.next().asLiteral().getInt();
        }
        LOGGER.info("Timeout set to {} ms, delta set to {} ms", timeOutms, delta);
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

    public int getSystemTimeoutms() {
        return timeOutms;
    }

    public int getAdapterTimeoutms() {
        return Math.max(1, Math.max(timeOutms + delta, (int) (timeOutms * deltaRatio)));
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        LOGGER.info("Retrieved task with id {}. ", taskId);
        String posNegExamples = RabbitMQUtils.readString(data);
        //empty string will be considered as an error
        String concept = "";
        AtomicReference<String> atomicConcept = new AtomicReference<>("");
        try {
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                try {
                    String conceptTmp = createConcept(posNegExamples);
                    LOGGER.info("recevied {}", conceptTmp);
                    atomicConcept.set(conceptTmp);
                } catch (Exception e) {
                    LOGGER.error("Problems retrieving concepts", e);
                    atomicConcept.set("");
                }
            });
            service.shutdown();
            try {
                int adapterTimeoutms = getAdapterTimeoutms();
                boolean terminated = service.awaitTermination(adapterTimeoutms, TimeUnit.MILLISECONDS);
                if (terminated) {
                    LOGGER.info("Concept creation finished in task {}.", taskId);
                } else {
                    LOGGER.error("Concept creation in task {} didn't finish in {}s.", taskId, adapterTimeoutms / 1000);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
            releaseMutexes();
            concept = atomicConcept.get();
            System.out.println("Concept: "+ concept);
                    //concept = createConcept(posNegExamples);
        } catch (Exception e) {
            LOGGER.error("Concept couldn't be created. ", e);
        }

        try {
            if(concept==null){
                concept="";
            }
            LOGGER.info("Sending concept {} now", concept);
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(concept));
            LOGGER.info("sended concept");
        } catch (IOException e) {
            //Log the error.
            LOGGER.error("Problem sending results to eval storage. ", e);
        }
    }
}
