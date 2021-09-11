package org.dice_group.raki.hobbit.core.config;

import org.dice_group.raki.hobbit.core.ilp.LearningProblem;
import org.dice_group.raki.hobbit.core.ilp.LearningProblemFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Simple Configuration for the ILP Benchmark
 * contains the benchmark name, the file location of the OWL ontology and the file location of the JSON formatted learning problems file
 */
public class Configuration {

    private String name;
    private String dataset;
    private String learningProblem;

    /**
     * Reads the Learning  Problems from the learningProblem file set in the Configuration
     * @return The set of Learning Problems or null
     * @throws IOException if the file cannot be read.
     */
    public Set<LearningProblem> readLearningProblems() throws IOException {
        return LearningProblemFactory.readMany(new File(learningProblem));
    }

    /**
     * Reads the OWL Ontology from the dataset file set in the configuration
     *
     * @return The OWL Ontology located at the `dataset` attribute in this configuration
     */
    public OWLOntology readOntology() throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(dataset)), loaderConfig);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getLearningProblem() {
        return learningProblem;
    }

    public void setLearningProblem(String learningProblem) {
        this.learningProblem = learningProblem;
    }
}
