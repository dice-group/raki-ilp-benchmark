package org.dice_group.raki.core.evaluation.f1measure;

import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class F1MeasureCalculatorTest {

    @ParameterizedTest(name = "given the true positives [\"{0}\"], false positives  [\"{1}\"] , false negatives values  [\"{2}\"], calculate the correct f1 measure  [\"{3}\", \"{4}\", \"{5}\"]")
    @MethodSource("createSimpleValues")
    public void calculationTest(int truePositives, int falsePositives, int falseNegatives,
                                double expectedF1, double expectedPrecision, double expectedRecall){
        F1MeasureCalculator calculator = new F1MeasureCalculator();
        F1Result result = calculator.addF1Measure(truePositives, falsePositives, falseNegatives);

        assertEquals(truePositives, result.getTruePositives());
        assertEquals(falsePositives, result.getFalsePositives());
        assertEquals(falseNegatives, result.getFalseNegatives());

        assertEquals(expectedF1, result.getF1measure());
        assertEquals(expectedPrecision, result.getPrecision());
        assertEquals(expectedRecall, result.getRecall());

    }

    @ParameterizedTest(name = "given a set of several true positives, false positives false negatives values, calculate the correct macro f1 measures")
    @MethodSource("createMacroValues")
    public void macroCalculationTest(Set<Integer[]> values,
                                          double expectedMacroF1, double expectedMacroPrecision, double expectedMacroRecall){
        F1MeasureCalculator calculator = new F1MeasureCalculator();
        values.forEach(
                value -> calculator.addF1Measure(value[0], value[1], value[2])
        );
        F1Result result = calculator.calculateMacroF1Measure();

        //get the summation of tp, fp, fn
        int truePositives = values.stream().mapToInt(value -> value[0]).sum();
        int falsePositives = values.stream().mapToInt(value -> value[1]).sum();
        int falseNegatives = values.stream().mapToInt(value -> value[2]).sum();

        assertEquals(truePositives, result.getTruePositives());
        assertEquals(falsePositives, result.getFalsePositives());
        assertEquals(falseNegatives, result.getFalseNegatives());

        assertEquals(expectedMacroF1, result.getF1measure());
        assertEquals(expectedMacroPrecision, result.getPrecision());
        assertEquals(expectedMacroRecall, result.getRecall());
    }

    @ParameterizedTest(name = "given a set of several true positives, false positives false negatives values, calculate the correct micro f1 measures")
    @MethodSource("createMicroValues")
    public void microCalculationTest(Set<Integer[]> values,
                                     double expectedMicroF1, double expectedMicroPrecision, double expectedMicroRecall){
        F1MeasureCalculator calculator = new F1MeasureCalculator();
        values.forEach(
                value -> calculator.addF1Measure(value[0], value[1], value[2])
        );
        F1Result result = calculator.calculateMicroF1Measure();

        //get the summation of tp, fp, fn
        int truePositives = values.stream().mapToInt(value -> value[0]).sum();
        int falsePositives = values.stream().mapToInt(value -> value[1]).sum();
        int falseNegatives = values.stream().mapToInt(value -> value[2]).sum();

        assertEquals(truePositives, result.getTruePositives());
        assertEquals(falsePositives, result.getFalsePositives());
        assertEquals(falseNegatives, result.getFalseNegatives());

        // we need a delta here just because of floating point errors we cannot control.
        assertEquals(expectedMicroF1, result.getF1measure(), 0.001);
        assertEquals(expectedMicroPrecision, result.getPrecision(), 0.001);
        assertEquals(expectedMicroRecall, result.getRecall(), 0.001);
    }


    public static Stream<Arguments> createMicroValues(){
        return Stream.of(
                Arguments.of(Sets.newHashSet(
                                new Integer[]{0, 2, 4},
                                new Integer[]{0, 5, 7}),
                            0.0, 0.0, 0.0
                        ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{10, 0, 0},
                                new Integer[]{0, 5, 7}),
                        2*(10.0/15.0 * 10.0/17.0)/(10.0/15.0 + 10.0/17.0), 10.0/15.0, 10.0/17.0
                    ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{0, 0, 0},
                                new Integer[]{5, 5, 5}),
                        0.5, 0.5, 0.5
                ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{10, 0, 0},
                                new Integer[]{5, 0, 0}),
                        1.0, 1.0, 1.0
                )
        );
    }

    public static Stream<Arguments> createMacroValues(){
        return Stream.of(
                Arguments.of(Sets.newHashSet(
                                new Integer[]{0, 2, 4},
                                new Integer[]{0, 5, 7}),
                        0.0, 0.0, 0.0
                ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{10, 0, 0},
                                new Integer[]{0, 5, 7}),
                        0.5, 0.5, 0.5
                ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{0, 0, 0},
                                new Integer[]{5, 5, 5}),
                        0.75, 0.75, 0.75
                ),
                Arguments.of(Sets.newHashSet(
                                new Integer[]{10, 0, 0},
                                new Integer[]{5, 0, 0}),
                        1.0, 1.0, 1.0
                )
        );
    }

    public static Stream<Arguments> createSimpleValues(){
        return Stream.of(
                Arguments.of(0, 0, 0, 1.0, 1.0, 1.0),
                Arguments.of(10, 0, 0, 1.0, 1.0, 1.0),
                Arguments.of(0, 10, 0, 0.0, 0.0, 0.0),
                Arguments.of(0, 0, 10, 0.0, 0.0, 0.0),
                Arguments.of(5, 5, 5, 0.5, 0.5, 0.5),
                Arguments.of(5, 5, 0, 2*(0.5)/(1.5), 0.5, 1.0),
                Arguments.of(5, 0, 5, 2*(0.5)/(1.5), 1.0, 0.5),
                Arguments.of(0, 5, 5, 0.0, 0.0, 0.0)
                );
    }

}
