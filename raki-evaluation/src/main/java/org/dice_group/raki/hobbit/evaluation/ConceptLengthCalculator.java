package org.dice_group.raki.hobbit.evaluation;


import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;

public class ConceptLengthCalculator extends DLSyntaxObjectRenderer {

    public int conceptLength=0;

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
