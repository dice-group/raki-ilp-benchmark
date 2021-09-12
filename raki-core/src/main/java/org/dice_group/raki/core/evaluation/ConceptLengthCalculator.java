package org.dice_group.raki.core.evaluation;

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;

/**
 * Calculates the Concept Length l, by increment l for every encountered sign.
 *
 * E.g. Pizza AND not Burger will resolve in a concept length of 4.
 *
 * The code can be accessed as follows
 * <pre>
 *     {@code
 *
 *          //create calculator
 *          ConceptLengthCalculator calculator = new ConceptLengthCalculator();
 *
 *          //Set your Concept/Class Expression here
 *          OWLClassExpression expr = ...
 *
 *          //Now we calculate the length of the OWLClassExpression object
 *          calculator.render(concept);
 *          int length = calculator.getConceptLength()
 *     }
 * </pre>
 */
public class ConceptLengthCalculator  extends DLSyntaxObjectRenderer {

    private int conceptLength=0;

    public int getConceptLength(){
        return conceptLength;
    }

    public void visit(OWLClass ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(OWLObjectIntersectionOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public  void visit(OWLObjectUnionOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(OWLObjectComplementOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        conceptLength+=2;
        super.visit(ce);
    }

    public  void visit(OWLObjectAllValuesFrom ce) {
        conceptLength+=2;
        super.visit(ce);
    }
}
