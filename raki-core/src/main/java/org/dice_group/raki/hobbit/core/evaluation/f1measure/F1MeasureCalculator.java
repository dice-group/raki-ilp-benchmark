package org.dice_group.raki.hobbit.core.evaluation.f1measure;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the F1 Measure of an object and stores the values to calculate the Micro and Macro F1 measure at any point.
 */
public class F1MeasureCalculator {

    private final List<F1Result> f1ResultList = new ArrayList<>();

    /**
     * Clears all stored results
     */
    public void clear(){
        f1ResultList.clear();
    }

    public List<F1Result> getF1ResultList() {
        return f1ResultList;
    }

    /**
     * Calculates the F1 Measure from the count of true positives, false positives and false negatives.
     *
     * The calculated f1 measure will be added internally to calculate the Macro and Micro F1 measure later on
     *
     * If all three values are 0 the F1 measure will be defaulted to 1.0
     *
     * @param truePositives the count of true positives
     * @param falsePositives the count of false positives
     * @param falseNegatives the count of false negatives.
     * @return The calculated F1Measure
     */
    public F1Result addF1Measure(int truePositives, int falsePositives, int falseNegatives){
        F1Result result = calculateF1Measure(truePositives, falsePositives, falseNegatives);
        f1ResultList.add(result);
        return result;
    }

    /**
     * Calculates the Micro F1-Measure.
     *
     * This will be done by adding all true positive, false positive and false negatives counts for all stored {@link F1Result}s
     * and calculate the F1-Measure from these.
     *
     * @return The Micro F1-Measure
     */
    public F1Result calculateMicroF1Measure(){
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        for (F1Result f1Result : f1ResultList) {
            truePositives += f1Result.getTruePositives();
            falsePositives += f1Result.getFalsePositives();
            falseNegatives += f1Result.getFalseNegatives();
        }
        return calculateF1Measure(truePositives, falsePositives, falseNegatives);
    }

    /**
     * Calculates the Macro F1-Measure (average over all f1 measures)
     *
     * @return the Macro F1-Measure
     */
    public F1Result calculateMacroF1Measure(){
        double f1measure = 0.0;
        double precision = 0.0;
        double recall = 0.0;
        int tp=0;
        int fp=0;
        int fn=0;
        for(F1Result f1result : f1ResultList){
            f1measure  += f1result.getF1measure();
            precision  += f1result.getPrecision();
            recall  += f1result.getRecall();
            tp += f1result.getTruePositives();
            fp += f1result.getFalsePositives();
            fn += f1result.getFalseNegatives();
        }
        f1measure /= f1ResultList.size();
        precision /= f1ResultList.size();
        recall /= f1ResultList.size();
        return new F1Result(f1measure, precision, recall, tp, fp, fn);
    }

    private F1Result calculateF1Measure(int truePositives, int falsePositives, int falseNegatives){
        double f1measure, precision, recall;

        if (truePositives == 0) {
            if ((falsePositives== 0) && (falseNegatives == 0)) {
                // If there haven't been something to find and nothing has been
                // found --> everything is great
                precision = 1.0;
                recall = 1.0;
                f1measure = 1.0;
            } else {
                // The annotator found no correct ones, but made some mistake
                // --> that is bad
                precision = 0.0;
                recall = 0.0;
                f1measure = 0.0;
            }
        } else {
            precision = (double) truePositives / (double) (truePositives + falsePositives);
            recall = (double) truePositives / (double) (truePositives + falseNegatives);
            f1measure = (2 * precision * recall) / (precision + recall);
        }

        return new F1Result(f1measure, precision, recall, truePositives, falsePositives, falseNegatives);
    }
}
