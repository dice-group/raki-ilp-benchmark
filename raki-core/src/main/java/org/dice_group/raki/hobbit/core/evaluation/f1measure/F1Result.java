package org.dice_group.raki.hobbit.core.evaluation.f1measure;


import java.util.Objects;

/**
 * A Container for one F1 Measure Result.
 *
 * It contains the F1Measure, Precision, Recall, #True_Positives, #False_Positives, #False_Negatives
 */
public class F1Result {

    private final int truePositives;
    private final int falsePositives;
    private final int falseNegatives;
    private final double f1measure;
    private final double precision;
    private final double recall;

    public F1Result(double f1measure, double precision, double recall, int truePositives, int falsePositives, int falseNegatives){
        this.f1measure = f1measure;
        this.precision =precision;
        this.recall = recall;
        this.truePositives =truePositives;
        this.falsePositives = falsePositives;
        this.falseNegatives = falseNegatives;
    }

    public double getF1measure() {
        return f1measure;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        F1Result f1Result = (F1Result) o;
        return truePositives == f1Result.truePositives && falsePositives == f1Result.falsePositives && falseNegatives == f1Result.falseNegatives && Double.compare(f1Result.f1measure, f1measure) == 0 && Double.compare(f1Result.precision, precision) == 0 && Double.compare(f1Result.recall, recall) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(truePositives, falsePositives, falseNegatives, f1measure, precision, recall);
    }

    @Override
    public String toString() {
        return "F1Result{" +
                "truePositives=" + truePositives +
                ", falsePositives=" + falsePositives +
                ", falseNegatives=" + falseNegatives +
                ", f1measure=" + f1measure +
                ", precision=" + precision +
                ", recall=" + recall +
                '}';
    }
}
