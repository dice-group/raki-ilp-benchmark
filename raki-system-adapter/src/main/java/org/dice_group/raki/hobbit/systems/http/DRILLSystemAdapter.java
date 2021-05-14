package org.dice_group.raki.hobbit.systems.http;

import com.clarkparsia.owlapi.explanation.io.manchester.ManchesterSyntaxObjectRenderer;
import com.google.common.collect.Sets;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.OWLAPIConfigProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

//TODO add some variables DRILL may use in init

public class DRILLSystemAdapter extends AbstractHTTPSystemAdapter {

    private static final String ONTOPY_PATH = "/OntoPy/";
    private static String baseUri ="http://localhost:9080";
    private final ManchesterOWLSyntaxOWLObjectRendererImpl renderer;
    private Configuration mapping;
    private OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();
    private OWLParser parser = new RDFXMLParser();
    private BidirectionalShortFormProviderAdapter provider;




    @Override
    protected String convertToManchester(String concept) throws OWLOntologyCreationException, IOException {
        OWLOntology onto = manager.createOntology();
        parser.parse(new StreamDocumentSource(new ByteArrayInputStream(concept.getBytes(StandardCharsets.UTF_8))), onto, manager.getOntologyLoaderConfiguration());
        OWLClass  pred0 =new OWLDataFactoryImpl().getOWLClass(IRI.create(onto.getOntologyID().getOntologyIRI().get().toString()+"#Pred_0"));
        OWLEquivalentClassesAxiom axiom = onto.getEquivalentClassesAxioms(pred0).iterator().next();
        OWLClassExpression expr = axiom.getClassExpressionsMinus(pred0).iterator().next();
        manager.removeOntology(onto);
        return renderer.render(expr);
    }

    public DRILLSystemAdapter() {
        super(baseUri);
        LOGGER.info("Loading mapping now.");
        loadFileMapping();
        LOGGER.info("mapping loaded.");
        renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
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
        mapping = readConfiguration("/raki/drill-mapping.properties");

    }


    @Override
    public void startSystem(String ontologyFile) throws Exception {
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        OWLOntology owlOntology = manager.loadOntologyFromOntologyDocument(IRI.create("http://www.w3.org/2002/07/owl"));
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlOntology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));
        renderer.setShortFormProvider(provider);
        String id = ontology.getOntologyID().getOntologyIRI().get().toString();

        LOGGER.info("Using Ontology with ID {}", id);
        String[] files = mapping.getString(id).split(",\\s*");
        String embeddings = files[0];
        String preTrainedData = files[1];
        LOGGER.info("Found embeddings {} and pre trained data {}", embeddings, preTrainedData);
        System.out.println(timeOutMs);
        String[] start = new String[]{"bash", "-c", "/raki/startonto.sh "+ontologyFile+" "+ONTOPY_PATH+"embeddings/"+embeddings+
                " "+ONTOPY_PATH+"pre_trained_agents/"+preTrainedData+" "+Math.max(1, this.timeOutMs/1000) };
        execute(start);
    }

    public void execute(String[] args) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true).inheritIO();
        processBuilder.command(args);
        processBuilder.start();
    }
}
