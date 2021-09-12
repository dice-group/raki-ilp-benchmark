package org.dice_group.raki.core.evaluation;

import org.apache.commons.math3.util.Pair;
import org.dice_group.raki.core.evaluation.f1measure.F1Result;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluatorTest {

    @ParameterizedTest(name = "given a Learning Problem and a concept for an Ontology, generate the correct results")
    @MethodSource("createSingleProblems")
    public void evaluateSingleProblemTest(LearningProblem problem, String concept, OWLOntology ontology, ResultContainer expected, boolean useConcepts) throws OWLOntologyCreationException {
        Evaluator evaluator = new Evaluator(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology(), useConcepts);
        ResultContainer actual = evaluator.evaluate(problem, concept);

        assertEquals(expected, actual);
    }


    @ParameterizedTest(name = "given a set of Learning Problem and concept pairs for an Ontology, generate the correct macro and micro F1 measure results")
    @MethodSource("createMultipleProblems")
    public void evaluateMultipleProblemTest(Set<Pair<LearningProblem, String>> problemPairs, OWLOntology ontology,
                                            F1Result expectedMacro, F1Result expectedMicro, boolean useConcepts) throws OWLOntologyCreationException {
        Evaluator evaluator = new Evaluator(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology(), useConcepts);
        evaluator.evaluate(problemPairs);
        F1Result actualMacro = evaluator.getMacroF1Measure();
        F1Result actualMicro = evaluator.getMicroF1Measure();
        assertEquals(expectedMacro, actualMacro);
        assertEquals(expectedMicro, actualMicro);
    }


    public static Stream<Arguments> createSingleProblems() throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("src/test/resources/ontologies/animals.owl"));
        return Stream.of(
                //TODO add use cases
        );
    }

    public static Stream<Arguments> createMultipleProblems() throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("src/test/resources/ontologies/animals.owl"));
        return Stream.of(
                //TODO add use cases
        );
    }

}
