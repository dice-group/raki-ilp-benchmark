package org.dice_group.raki.hobbit.evaluation;

/**
 * A Result Container.
 *
 * Contains
 * * error count
 * * average concept length
 * * max concept length
 * * min concept length
 * * first percentile of the concept length
 * * second percentile of the concept length
 * * third percentile of the concept length
 * * variance of the concept length
 * * average result time
 * * max result time
 * * min result time
 * * first percentile of the result time
 * * second percentile of the result time
 * * third percentile of the result time
 * * variance of the result time
 * * macro Precision
 * * macro Recall
 * * macro F1 measure
 * * micro Precision
 * * micro Recall
 * * micro F1 measure
 *
 */
public class ResultStorage {

    private final int errorCount;
    private final double avgConceptLength;
    private final double maxCL;
    private final double minCL;
    private final double fpCL;
    private final double spCL;
    private final double tpCL;
    private final double varCL;
    private final double avgRT;
    private final double maxRT;
    private final double minRT;
    private final double fpRT;
    private final double spRT;
    private final double tpRT;
    private final double varRT;
    private final double macroPrecision;
    private final double macroRecall;
    private final double macroF1Measure;
    private final double microPrecision;
    private final double microRecall;
    private final double microF1Measure;


    public ResultStorage(double macroF1Measure, double macroPrecision, double macroRecall,
                         double microF1Measure, double microPrecision, double microRecall, int errorCount,
                         double avgConceptLength, double maxCL, double minCL,
                         double fpCL, double tpCL, double spCL, double varCL,
                         double avgRT, double maxRT, double minRT,
                         double fpRT, double tpRT, double spRT, double varRT) {
        this.errorCount=errorCount;
        this.macroF1Measure=macroF1Measure;
        this.macroPrecision=macroPrecision;
        this.macroRecall=macroRecall;
        this.microF1Measure=microF1Measure;
        this.microPrecision=microPrecision;
        this.microRecall=microRecall;

        this.avgConceptLength=avgConceptLength;
        this.maxCL=maxCL;
        this.minCL=minCL;
        this.fpCL=fpCL;
        this.spCL=spCL;
        this.tpCL=tpCL;
        this.varCL=varCL;

        this.avgRT=avgRT;
        this.maxRT=maxRT;
        this.minRT=minRT;
        this.fpRT=fpRT;
        this.spRT=spRT;
        this.tpRT=tpRT;
        this.varRT=varRT;
    }

    /**
     * Creates an empty ResultStorage where each value is set to 0
     *
     * @return an empty ResultStorage
     */
    public static ResultStorage createEmpty() {
        return new ResultStorage(0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0);
    }

    public double getMacroPrecision() {
        return macroPrecision;
    }

    public double getMacroRecall() {
        return macroRecall;
    }

    public double getMacroF1Measure() {
        return macroF1Measure;
    }

    public double getMicroPrecision() {
        return microPrecision;
    }

    public double getMicroRecall() {
        return microRecall;
    }

    public double getMicroF1Measure() {
        return microF1Measure;
    }
    public int getErrorCount(){
        return errorCount;
    }


    public double getAvgConceptLength(){
        return avgConceptLength;
    }

    public double getMaxConceptLength() {
        return maxCL;
    }
    public double getMinConceptLength() {
        return minCL;
    }

    public double getVarianceConceptLength() {
        return varCL;
    }

    public double getFirstPercentileConceptLength() {
        return fpCL;
    }

    public double getSecondPercentileConceptLength() {
        return spCL;
    }

    public double getThirdPercentileConceptLength() {
        return tpCL;
    }


    public double getAvgResultTimes(){
        return avgRT;
    }

    public double getMaxResultTimes() {
        return maxRT;
    }
    public double getMinResultTimes() {
        return minRT;
    }

    public double getVarianceResultTimes() {
        return varRT;
    }

    public double getFirstPercentileResultTimes() {
        return fpRT;
    }

    public double getSecondPercentileResultTimes() {
        return spRT;
    }

    public double getThirdPercentileResultTimes() {
        return tpRT;
    }
}
