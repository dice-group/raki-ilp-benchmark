package org.dice_group.raki.core.concepts;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectComplementOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManchesterSyntaxParserTest {

    @ParameterizedTest(name = "Given \"{0}\" parse correctly to \"{1}\"")
    @MethodSource("createConcepts")
    public void correctParsing(String concept, OWLClassExpression expected, OWLOntology ontology) throws OWLOntologyCreationException {
        ManchesterSyntaxParser parser = new ManchesterSyntaxParser(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology());
        OWLClassExpression actual = parser.parse(concept);
        assertEquals(expected, actual, "Manchester Syntax parsing didn't work");
    }

    @ParameterizedTest(name = "Given \"{1}\" render correctly to \"{0}\"")
    @MethodSource("createConcepts")
    public void correctRendering(String expected, OWLClassExpression concept, OWLOntology ontology) throws OWLOntologyCreationException {
        ManchesterSyntaxParser parser = new ManchesterSyntaxParser(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology());
        String actual = parser.render(concept);
        assertEquals(expected, actual, "Manchester Syntax rendering didn't work");
    }


    public static Stream<Arguments> createConcepts() throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("src/test/resources/ontologies/animals.owl"));
        return Stream.of(
                //Simple class
                Arguments.of("dl:Bird", new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")), ontology),
                //NOT
                Arguments.of("not (dl:Bird)", new OWLObjectComplementOfImpl(new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird"))), ontology),
                //OR
                Arguments.of("dl:Bird or dl:Cat",
                        new OWLObjectUnionOfImpl(
                                Lists.newArrayList(
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")))
                        ), ontology),
                //AND
                Arguments.of("dl:Bird\n and dl:Cat",
                        new OWLObjectIntersectionOfImpl(
                                Lists.newArrayList(
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")))
                        ), ontology),
                // AND and OR
                Arguments.of("dl:Bird\n and (dl:Cat or dl:Dog)",
                        new OWLObjectIntersectionOfImpl(
                                Lists.newArrayList(
                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Bird")),
                                        new OWLObjectUnionOfImpl(
                                                Lists.newArrayList(
                                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Cat")),
                                                        new OWLClassImpl(IRI.create("http://dl-learner.org/benchmark/dataset/animals/Dog"))
                                                )
                                        )
                                )
                        ), ontology)
        );
    }
}
