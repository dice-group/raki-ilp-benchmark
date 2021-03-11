package org.dice_group.raki.hobbit.systems.http;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.File;
import java.io.IOException;

//TODO add some variables DRILL may use in init

public class DRILLSystemAdapter extends AbstractHTTPSystemAdapter {

    private static final String ONTOPY_PATH = "/home/me/Code/OntoPy/";
    private static String baseUri ="http://localhost:9080";
    private Configuration mapping;

    public DRILLSystemAdapter() {
        super(baseUri);
        loadFileMapping();
    }

    private Configuration readConfiguration(String fileName){
        File propertiesFile = new File(fileName);
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased()
                                .setFile(propertiesFile));
        try
        {
            return builder.getConfiguration();
            // config contains all properties read from the file
        }
        catch(ConfigurationException cex)
        {
            LOGGER.error("Could not read dataset/benchmark property file");
            return null;
        }
    }


    private void loadFileMapping() {
        mapping = readConfiguration(getClass().getClassLoader().getResource("drill-mapping.properties").getFile());

    }


    @Override
    public void startSystem(String ontologyFile) throws Exception {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(ontologyFile));
        String id = ontology.getOntologyID().toString();
        String[] files = mapping.getStringArray(id);
        String embeddings = files[0];
        String preTrainedData = files[1];
        String[] start = new String[]{ONTOPY_PATH+"venv/bin/python",
                ONTOPY_PATH+"ontolearn/endpoint/simple_drill_endpoint",
                "--path_knowledge_base", ontologyFile,
                "--path_knowledge_base_embeddings", ONTOPY_PATH+"pre_trained_agents/"+embeddings,
                "--pretrained_drill_avg_path", ONTOPY_PATH+"DrillHeuristic_averaging/"+preTrainedData};
        execute(start);
    }

    private void execute(String[] args) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true).inheritIO();
        processBuilder.command(args);
        Process process = processBuilder.start();
    }
}
