package org.dice_group.raki.core.config;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationTest {

    @Test
    @Order(1)
    public void readCorrectConfigurations() throws IOException {
        Configurations configurations = Configurations.load(new File("src/test/resources/configurations/config.yml"));
        List<Configuration> actual = configurations.getAsList();

        //create expected
        List<Configuration> expected = createExpected();

        assertEquals(expected, actual);
    }

    private List<Configuration> createExpected(){
        Configuration conf1 = new Configuration();
        conf1.setName("benchmark1");
        conf1.setDataset("/path/to/dataset1.owl");
        conf1.setLearningProblem("/path/to/lps1.json");

        Configuration conf2 = new Configuration();
        conf2.setName("benchmark2");
        conf2.setDataset("/path/to/dataset2.owl");
        conf2.setLearningProblem("/path/to/lps2.json");

        Configuration conf3 = new Configuration();
        conf3.setName("benchmark3");
        conf3.setDataset("/path/to/dataset3.owl");
        conf3.setLearningProblem("/path/to/lps3.json");

        return Lists.newArrayList(conf1, conf2, conf3);
    }

    @Test
    @Order(2)
    public void retrieveCorrectConfiguration() throws IOException {
        Configurations configurations = Configurations.load(new File("src/test/resources/configurations/config.yml"));

        Configuration actual = configurations.getConfiguration("benchmark2");

        Configuration expected = new Configuration();
        expected.setName("benchmark2");
        expected.setDataset("/path/to/dataset2.owl");
        expected.setLearningProblem("/path/to/lps2.json");

        assertEquals(expected, actual);
    }

}
