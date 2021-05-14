package org.dice_group.raki.hobbit.evaluation;

public class ResultStorage {

    private int errorCount;
    private double avgConceptLength;
    private double maxCL;
    private double minCL;
    private double fpCL;
    private double spCL;
    private double tpCL;
    private double varCL;
    private double avgRT;
    private double maxRT;
    private double minRT;
    private double fpRT;
    private double spRT;
    private double tpRT;
    private double varRT;
    private double macroPrecision=0.0;
    private double macroRecall=0.0;
    private double macroF1Measure=0.0;
    private double microPrecision=0.0;
    private double microRecall=0.0;
    private double microF1Measure=0.0;


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
