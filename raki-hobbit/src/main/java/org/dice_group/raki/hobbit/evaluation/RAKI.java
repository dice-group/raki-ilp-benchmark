package org.dice_group.raki.hobbit.evaluation;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class RAKI {
    public static final String RAKI2_PREFIX = "http://w3id.org/raki/hobbit/vocab#";
    public static Property macroPrecision=getProperty("macroPrecision");
    public static Property macroRecall=getProperty("macroRecall");
    public static Property macroF1=getProperty("macroF1");
    public static Property microPrecision=getProperty("microPrecision");
    public static Property microRecall=getProperty("microRecall");
    public static Property microF1=getProperty("microF1");
    public static Property errorCount=getProperty("errorCount");
    public static Property avgConceptLength=getProperty("avgConceptLength");
    public static Property maxConceptLength=getProperty("maxConceptLength");
    public static Property minConceptLength=getProperty("minConceptLength");
    public static Property varianceConceptLength=getProperty("varianceConceptLength");
    public static Property firstPercentileConceptLength=getProperty("firstPercentileConceptLength");
    public static Property secondPercentileConceptLength=getProperty("secondPercentileConceptLength");
    public static Property thirdPercentileConceptLength=getProperty("thirdPercentileConceptLength");
    public static Property avgResultTimes=getProperty("avgCResultTimes");
    public static Property maxResultTimes=getProperty("maxResultTimes");
    public static Property minResultTimes=getProperty("minResultTimes");
    public static Property varianceResultTimes=getProperty("varianceResultTimes");
    public static Property firstPercentileResultTimes=getProperty("firstPercentileResultTimes");
    public static Property secondPercentileResultTimes=getProperty("secondPercentileResultTimes");
    public static Property thirdPercentileResultTimes=getProperty("thirdPercentileResultTimes");
    public static Property noOfConcepts=getProperty("noOfConcepts");

    public static Property getProperty(String propertyName){
        return ResourceFactory.createProperty(RAKI2_PREFIX+propertyName);
    }
}
