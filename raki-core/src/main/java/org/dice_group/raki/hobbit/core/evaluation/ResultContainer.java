package org.dice_group.raki.hobbit.core.evaluation;

import org.dice_group.raki.hobbit.core.evaluation.f1measure.F1Result;

/**
 * Simple Wrapper class for the {@link org.dice_group.raki.hobbit.core.evaluation.f1measure.F1Result} and the concept length
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
}
