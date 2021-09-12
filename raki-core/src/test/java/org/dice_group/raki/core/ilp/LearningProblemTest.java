package org.dice_group.raki.core.ilp;

import org.apache.commons.compress.utils.Sets;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LearningProblemTest {

    @ParameterizedTest(name="given a json string, create the correct learning problem")
    @MethodSource("createJsonStrings")
    public void stringParse(String jsonString, LearningProblem expected){
        LearningProblem actual = LearningProblemFactory.parse(jsonString);
        assertEquals(expected, actual);
    }

    @Test
    public void fileParse() throws IOException {
        Set<LearningProblem> problems = LearningProblemFactory.readMany(new File("src/test/resources/learningProblems/problems.json"));
        assertEquals(3, problems.size());

        //creates the expected creation
        Set<LearningProblem> expected = Sets.newHashSet(
                LearningProblemFactory.create(Lists.newArrayList("http://example.com/1", "http://example.com/2"),
                    Lists.newArrayList("http://example.com/3", "http://example.com/4")),
                LearningProblemFactory.create(Lists.newArrayList("http://example.com/5", "http://example.com/6"),
                        Lists.newArrayList("http://example.com/7")),
                LearningProblemFactory.create(Lists.newArrayList("http://example.com/8"),
                        Lists.newArrayList("http://example.com/9", "http://example.com/10"),
                        "ABCDEFG")
                );
        assertEquals(expected, problems);
    }

    @ParameterizedTest(name = "given a Learning Problem, and a positive and negative ratio, return the correct amount of positive and negative uris")
    @MethodSource("createLearningProblems")
    public void createCorrectPositiveRatio(LearningProblem problem, double positiveRatio, int expectedPositiveSize,
                                           double negativeRatio, int expectedNegativeSize){
        Random rand = new Random();
        Set<String> uris = problem.getSomePositiveUris(positiveRatio, rand);
        assertEquals(expectedPositiveSize, uris.size());

        uris = problem.getSomeNegativeUris(negativeRatio, rand);
        assertEquals(expectedNegativeSize, uris.size());
    }


    public static Stream<Arguments> createJsonStrings(){
        return Stream.of(
                Arguments.of("{ \"positives\": [\"http://example.com/1\"], \"negatives\": [\"http://example.com/2\", \"http://example.com/3\"] }",
                        LearningProblemFactory.create(
                                Lists.newArrayList("http://example.com/1"),
                                Lists.newArrayList("http://example.com/2", "http://example.com/3")
                        )),
                Arguments.of("{ \"concept\": \"ABCDEFG\", \"positives\": [\"http://example.com/1\"], \"negatives\": [\"http://example.com/2\", \"http://example.com/3\"] }",
                        LearningProblemFactory.create(
                                Lists.newArrayList("http://example.com/1"),
                                Lists.newArrayList("http://example.com/2", "http://example.com/3"),
                                "ABCDEFG"
                        )),
                Arguments.of("{ \"wrongJson\": [\"http://example.com/1\"], \"negatives\": [\"http://example.com/2\", \"http://example.com/3\"] }",
                        null)
        );
    }

    public static Stream<Arguments> createLearningProblems(){
        return Stream.of(
                Arguments.of(LearningProblemFactory.create(
                        Lists.newArrayList("http://example.com/1", "http://example.com/2", "http://example.com/3"),
                        Lists.newArrayList("http://example.com/2", "http://example.com/3", "http://example.com/4")
                ), 0.5, 1, 0.7, 2),
                Arguments.of(LearningProblemFactory.create(
                        Lists.newArrayList("http://example.com/1", "http://example.com/2", "http://example.com/3"),
                        Lists.newArrayList("http://example.com/2", "http://example.com/3", "http://example.com/4")
                ), 1.0, 3, 0.2, 0),
                Arguments.of(LearningProblemFactory.create(
                        Lists.newArrayList("http://example.com/1", "http://example.com/2", "http://example.com/3"),
                        Lists.newArrayList("http://example.com/2", "http://example.com/3", "http://example.com/4")
                ), 1.0, 3, 1.0, 3)
        );
    }
}
