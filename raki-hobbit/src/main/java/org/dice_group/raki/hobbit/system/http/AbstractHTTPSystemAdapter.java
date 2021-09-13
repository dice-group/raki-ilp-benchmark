package org.dice_group.raki.hobbit.system.http;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.lf5.util.StreamUtils;
import org.dice_group.raki.hobbit.system.AbstractRakiSystemAdapter;
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


    protected String baseUri;

    public AbstractHTTPSystemAdapter(String baseUri){
        super();
        this.baseUri = baseUri;

    }


    @Override
    public String createConcept(String posNegExample) throws IOException {
        LOGGER.debug("Creating Concept request");
        String learningUri = baseUri+"/concept_learning";
        HttpPost post = new HttpPost(learningUri);

        CloseableHttpClient httpclient = HttpClients.createDefault();


        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(this.timeOutMs.intValue()+delta);
        requestConfig.setConnectionRequestTimeout(this.timeOutMs.intValue()+delta);
        requestConfig.setSocketTimeout(this.timeOutMs.intValue()+delta);

        post.setConfig(requestConfig.build());
        post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        StringEntity entity = new StringEntity(posNegExample);
        post.setEntity(entity);
        entity.setContentType("application/json");
        String concept = "";
        try{
            LOGGER.info("Sending Concept request");
            HttpResponse response = httpclient.execute(post);
            InputStream is = response.getEntity().getContent();
            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("Status is not 200, but {}", response.getStatusLine().getStatusCode());
                httpclient.close();
                return "";
            }
            try (BufferedInputStream bis = new BufferedInputStream(is)) {
                byte[] data = StreamUtils.getBytes(bis);
                concept = RabbitMQUtils.readString(data);

            }
            try {
                concept = convertToManchester(concept);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }catch(Exception e){
            LOGGER.warn(e.getMessage());
            return "";
        }finally {
            LOGGER.info("Closing connection.");
            httpclient.close();
            LOGGER.info("Closing connection done.");
        }
        LOGGER.info("Concept received successfully.");
        LOGGER.info("Concept is: {}", concept);
        return concept;
    }


    protected String convertToManchester(String concept) throws OWLOntologyCreationException, IOException {
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
        boolean ready=false;
        do {
            try {
                String statusUri = baseUri + "/status";
                HttpGet get = new HttpGet(statusUri);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpResponse response = httpclient.execute(get);
                ready = response.getStatusLine().getStatusCode() == 200;
                if (ready) {
                    InputStream is = response.getEntity().getContent();
                    try (BufferedInputStream bis = new BufferedInputStream(is)) {
                        byte[] data = StreamUtils.getBytes(bis);
                        String jsonStr = RabbitMQUtils.readString(data);
                        System.out.println(jsonStr);
                        JSONObject jsonStatus = new JSONObject(jsonStr);
                        if (jsonStatus.has("status")) {
                            LOGGER.debug("System Status: {}", jsonStatus.toString());
                            ready = "ready".equals(jsonStatus.get("status").toString().toLowerCase());
                        } else {
                            ready = false;
                        }
                    }
                }
                httpclient.close();
                try {
                    this.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }catch(Exception e){
                try {
                    this.wait(100);
                } catch (Exception e1) {
                }
            }
        }while(!ready);
        //System now ready
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    public abstract void startSystem(String ontologyFile) throws OWLOntologyCreationException, Exception;
}
