package org.dice_group.raki.core.evaluation;

import org.dice_group.raki.core.evaluation.f1measure.F1Result;

import java.util.Objects;

/**
 * Simple Wrapper class for the {@link F1Result} and the concept length
 */
public class ResultContainer {

    private final F1Result f1Result;
    private final int conceptLength;
    private final String concept;

    public ResultContainer(String concept, F1Result f1Result, int conceptLength){
        this.f1Result = f1Result;
        this.conceptLength = conceptLength;
        this.concept = concept;
    }

    public int getConceptLength() {
        return conceptLength;
    }

    public F1Result getF1Result() {
        return f1Result;
    }

    public String getConcept() {
        return concept;
    }

    @Override
    public String toString() {
        return "ResultContainer{" +
                "f1Result=" + f1Result +
                ", conceptLength=" + conceptLength +
                ", concept='" + concept + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultContainer that = (ResultContainer) o;
        return conceptLength == that.conceptLength && Objects.equals(f1Result, that.f1Result) && Objects.equals(concept, that.concept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(f1Result, conceptLength, concept);
    }
}
