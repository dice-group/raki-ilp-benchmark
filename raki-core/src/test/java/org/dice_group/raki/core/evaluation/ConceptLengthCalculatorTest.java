package org.dice_group.raki.core.evaluation;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectComplementOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConceptLengthCalculatorTest {

    @ParameterizedTest(name = "given the expression \"{0}\" calculate it's length as \"{1}\"")
    @MethodSource("createConcepts")
    public void calculateLength(OWLClassExpression expression, int expectedLength){
        ConceptLengthCalculator calculator = new ConceptLengthCalculator();
        calculator.render(expression);
        int actualLength = calculator.getConceptLength();

        assertEquals(expectedLength, actualLength, "Calculated Concept length is not as expected");
    }

    public static Stream<Arguments> createConcepts(){
        return Stream.of(
                //Bird - single class
                Arguments.of(new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")), 1),
                //Not Bird - negation
                Arguments.of(new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")).getObjectComplementOf(), 2),
                // Bird AND cat -> intersection
                Arguments.of(new OWLObjectIntersectionOfImpl(
                        Lists.newArrayList(
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")))
                        ), 3),
                // Bird OR Cat -> union
                Arguments.of(new OWLObjectUnionOfImpl(
                        Lists.newArrayList(
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")))
                        ), 3),
                Arguments.of(new OWLObjectComplementOfImpl(new OWLObjectIntersectionOfImpl(
                        Lists.newArrayList(
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")))
                            )
                        ), 4),
                //Bird And (Cat or Not Dog)
                Arguments.of(new OWLObjectIntersectionOfImpl(
                                Lists.newArrayList(
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                        new OWLObjectUnionOfImpl(
                                                Lists.newArrayList(
                                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")),
                                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Dog")).getObjectComplementOf()
                                                )
                                        )
                                )
                        ), 6
                        )
        );
    }
}
