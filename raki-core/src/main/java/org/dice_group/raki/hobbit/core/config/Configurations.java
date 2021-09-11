package org.dice_group.raki.hobbit.core.config;

import java.io.File;
import java.util.List;

/**
 * Wrapper for the {@link Configuration}s to load from a file
 */
public class Configurations {

    public static Configurations load(File configurationFile){
        //TODO load from YAML marshalling
        return null;
    }

    public List<Configuration> datasets;

    public List<Configuration> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Configuration> datasets) {
        this.datasets = datasets;
    }
}
