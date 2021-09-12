package org.dice_group.raki.core.evaluation;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.math3.util.Pair;
import org.dice_group.raki.core.evaluation.f1measure.F1Result;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.ilp.LearningProblemFactory;
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
                //lp, concept, onto, expected, useConcepts
                Arguments.of(
                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01"), Sets.newHashSet("\"http://dl-learner.org/benchmark/dataset/animals#eel1")),
                        "dl:Eagle",
                        ontology,
                        new ResultContainer("dl:Eagle", new F1Result(1.0, 1.0, 1.0, 1, 0, 0), 1),
                        false
                ),
                Arguments.of(
                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01", "http://dl-learner.org/benchmark/dataset/animals#penguin01"), Sets.newHashSet("\"http://dl-learner.org/benchmark/dataset/animals#eel1")),
                        "dl:Eagle",
                        ontology,
                        new ResultContainer("dl:Eagle", new F1Result(1/1.5, 1.0, 0.5, 1, 0, 1), 1),
                        false
                ),
                Arguments.of(
                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01"), Sets.newHashSet("\"http://dl-learner.org/benchmark/dataset/animals#eel1"), "dl:HasMilk"),
                        "dl:HasMilk",
                        ontology,
                        new ResultContainer("dl:HasMilk", new F1Result(1.0, 1.0, 1.0, 5, 0, 0), 1),
                        true
                )
        );
    }

    public static Stream<Arguments> createMultipleProblems() throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("src/test/resources/ontologies/animals.owl"));
        return Stream.of(
                //problemPairs, onto, macro, micro, useConcepts
                Arguments.of(
                        Sets.newHashSet(new Pair<>(
                                LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01", "http://dl-learner.org/benchmark/dataset/animals#dragon01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                "dl:Dragon or dl:Eagle"
                                ),
                                new Pair<>(
                                        LearningProblemFactory.create(Sets.newHashSet( "http://dl-learner.org/benchmark/dataset/animals#dragon01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                        "dl:Dragon"
                                )
                        ),
                        ontology,
                        new F1Result(1.0,1.0,1.0,3,0,0),
                        new F1Result(1.0,1.0,1.0,3,0,0),
                        false
                ),
                Arguments.of(
                        Sets.newHashSet(new Pair<>(
                                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                        "dl:Eel"
                                ),
                                new Pair<>(
                                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#dragon01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                        "dl:Dragon"
                                )
                        ),
                        ontology,
                        new F1Result(0.5,0.5,0.5,1,1,1),
                        new F1Result(0.5,0.5,0.5,1,1,1),
                        false
                ),
                Arguments.of(
                        Sets.newHashSet(new Pair<>(
                                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eagle01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                        "dl:Eel"
                                ),
                                new Pair<>(
                                        LearningProblemFactory.create(Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#dragon01"), Sets.newHashSet("http://dl-learner.org/benchmark/dataset/animals#eel01")),
                                        "dl:Eel"
                                )
                        ),
                        ontology,
                        new F1Result(0.0,0.0,0.0,0,2,2),
                        new F1Result(0.0,0.0,0.0,0,2,2),
                        false
                )


        );
    }

}
