package org.dice_group.raki.hobbit.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Wrapper for the {@link Configuration}s to load from a file
 */
public class Configurations {


    /**
     * Creates the Configurations' wrapper from the provided YAML file .
     *
     * The file has to have the key "datasets" which in itself is an Array of {@link Configuration} objects
     *
     * An example:
     * <pre>
     *     {@code
     *      datasets:
     *          - name: "MyBenchmarkName"
     *            dataset: "/path/to/dataset.owl"
     *            learningProblem: "/path/to/learningProblem.json"
     *          - name: "MyBenchmarkName2"
     *            dataset: "/path/to/dataset2.owl"
     *            learningProblem: "/path/to/learningProblem2.json"
     *     }
     *
     * </pre>
     *
     * @param configurationFile The file containing all configurations
     * @return a list of {@link Configuration}s
     * @throws IOException If the file cannot be read, the YAML cannot be mapped or parsed.
     */
    public static Configurations load(File configurationFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(configurationFile, Configurations.class);
    }

    public List<Configuration> datasets;

    public List<Configuration> getAsList() {
        return datasets;
    }

    public void setDatasets(List<Configuration> datasets) {
        this.datasets = datasets;
    }
}
