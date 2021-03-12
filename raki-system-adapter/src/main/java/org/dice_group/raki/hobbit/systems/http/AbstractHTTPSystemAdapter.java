package org.dice_group.raki.hobbit.systems.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.lf5.util.StreamUtils;
import org.dice_group.raki.hobbit.systems.AbstractRakiSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractHTTPSystemAdapter extends AbstractRakiSystemAdapter {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractHTTPSystemAdapter.class);


    private String baseUri;

    public AbstractHTTPSystemAdapter(String baseUri){
        super();
        this.baseUri = baseUri;
    }


    @Override
    public String createConcept(String posNegExample) throws IOException {
        LOGGER.debug("Creating Concept request");
        String learningUri = baseUri+"/concept_learning";
        HttpPost post = new HttpPost(learningUri);
        StringEntity entity = new StringEntity(posNegExample);
        post.setEntity(entity);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        LOGGER.info("Sending Concept request");
        HttpResponse response = httpclient.execute(post);
        InputStream is = response.getEntity().getContent();
        if(response.getStatusLine().getStatusCode()!=200){
            LOGGER.error("Status is not 200, but {}",response.getStatusLine().getStatusCode());
            return "";
        }
        String concept="";
        try(BufferedInputStream bis = new BufferedInputStream(is)){
            byte[] data = StreamUtils.getBytes(bis);
            concept = RabbitMQUtils.readString(data);

        }
        httpclient.close();
        LOGGER.info("Concept received successfully.");
        LOGGER.debug("Concept is: {}", concept);
        return concept;
    }

    @Override
    public void loadOntology(File ontology) throws Exception {
         LOGGER.info("Starting system now with ontology file {}", ontology.getAbsolutePath());
         startSystem(ontology.getAbsolutePath());
         LOGGER.info("Waiting for system to be ready.");
         waitForSystemReady();
         LOGGER.info("System is now ready to be queried");
    }

    protected void waitForSystemReady() throws IOException {
        boolean notReady=true;
        do {
            String statusUri = baseUri + "/status";
            HttpGet get = new HttpGet(statusUri);
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpResponse response = httpclient.execute(get);
            notReady = response.getStatusLine().getStatusCode()==200;
            if(!notReady){
            InputStream is = response.getEntity().getContent();
            try(BufferedInputStream bis = new BufferedInputStream(is)){
                byte[] data =StreamUtils.getBytes(bis);
                String jsonStr = RabbitMQUtils.readString(data);
                JSONObject jsonStatus = new JSONObject(jsonStr);
                if(jsonStatus.has("status")) {
                    LOGGER.debug("System Status: {}",jsonStatus.toString());
                    notReady="ready".equals(jsonStatus.get("status").toString().toLowerCase());
                }else{
                    notReady=false;
                }
            }
            }
            httpclient.close();
            try {
                this.wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }while(notReady);
        //System now ready
    }

    public abstract void startSystem(String ontologyFile) throws OWLOntologyCreationException, Exception;
}
