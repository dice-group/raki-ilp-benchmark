package org.dice_group.raki.hobbit.core.evaluation;

import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.commons.math3.util.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.dice_group.raki.hobbit.core.concepts.ManchesterSyntaxParser;
import org.dice_group.raki.hobbit.core.evaluation.f1measure.F1MeasureCalculator;
import org.dice_group.raki.hobbit.core.evaluation.f1measure.F1Result;
import org.dice_group.raki.hobbit.core.ilp.LearningProblem;
import org.dice_group.raki.hobbit.core.utils.TablePrinter;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Evaluator will calculate the F1Measures and Concept Lengths for
 * the provided learning problems and their corresponding solution, represented as a Concept ({@link org.semanticweb.owlapi.model.OWLClassExpression})
 */
public class Evaluator {

    private final F1MeasureCalculator f1MeasureCalculator = new F1MeasureCalculator();

    private final List<ResultContainer> resultContainers = new ArrayList<>();
    private final OWLOntology ontology;
    private final OWLOntology owlBaseOntology;
    private final boolean useConcepts;

    /**
     * Creates an Evaluator using the Ontology and the provided OWL Base Ontology.
     *
     * If useConcept is set to true, the evaluator will retrieve all Individuals not from the Examples in the Learning Problem,
     * but using the concept in the Learning Problem.
     * If the LP doesn't contain a Concept, it will simply use the provided examples.
     *
     * @param ontology The ontology to use
     * @param owlBaseOntology The owl base ontology
     * @param useConcepts if concepts should be used to retrieve the gold standard, rather than evaluating against the examples contained in the Learning Problem.
     */
    public Evaluator(OWLOntology ontology, OWLOntology owlBaseOntology, boolean useConcepts){
        this.ontology = ontology;
        this.owlBaseOntology = owlBaseOntology;
        this.useConcepts = useConcepts;
    }

    /**
     * Creates the Openllet Reasoner to use
     *
     * @return The OWL Reasoner to retrieve Individuals with
     */
    private OWLReasoner createReasoner() {
        return OpenlletReasonerFactory.getInstance().createReasoner(ontology);
    }


    /**
     * Calculates the F1 Measures and Concept Length for the provided {@link LearningProblem} and the concept answering this problem.
     *
     * @param problem the problem which was solved
     * @param answerConcept the concept which is the solution for the problem
     * @return The result container, containing the {@link F1Result} and the conceptLength
     */
    public ResultContainer evaluate(LearningProblem problem, String answerConcept){
        OWLClassExpression expr = ManchesterSyntaxParser.parse(answerConcept, ontology, owlBaseOntology);

        //calculate length.
        ConceptLengthCalculator calculator = new ConceptLengthCalculator();
        calculator.render(expr);
        int conceptLength = calculator.getConceptLength();

        //TODO retrieve OWLNamedIndividuals for answerConcept, and get result dependent on the different techniques.

        // count tp, fp, fn
        int truePositives, falsePositives, falseNegatives;
        int[] vals = evaluate(positiveUris, negativeUris, answers);
        truePositives = vals[0];
        falsePositives = vals[1];
        falseNegatives = vals[2];

        //calculate f1 Result
        F1Result f1Result = f1MeasureCalculator.addF1Measure(truePositives, falsePositives, falseNegatives);

        return new ResultContainer(
                answerConcept,
                f1Result,
                conceptLength);
    }

    /**
     * Evaluates the positive uris and the negative uris against the answer uris.
     * And returns the true positive, false positive and false negative counts.
     *
     * If the negative uris are empty, it will assume that all uris not in positiveUris are negative uris.
     *
     * @param positiveUris the positive uris
     * @param negativeUris the negative uris
     * @param answers The list of answers
     * @return an 3 dim integer, [true positives, false positives, false negatives]
     */
    private int[] evaluate(Collection<String> positiveUris, Collection<String> negativeUris, Collection<String> answers){
        int[] evalVals= new int[]{0,0,0};

        //for all answers
        for(String answer: answers){

            //check if the positive uris contains the answer
            if(positiveUris.contains(answer)){
                evalVals[0]++; //tp ++
            }
            //check if negative uris contains answer -> false positive found,
            //if negative Uris is empty, we assume that all other individuals are negative
            else if(negativeUris.contains(answer) || negativeUris.isEmpty()){
                evalVals[1]++; //fp ++
            }
        }
        //we can simply evaluate the false negatives, by subtracting the amount of true positives from the provided positive Uris.
        //fn (if we found all positive examples, good, if we missed one, this will be accounted here)
        evalVals[2] = positiveUris.size()-evalVals[0];
        return evalVals;
    }

    /**
     * Evaluates each Learning Problem - Concept Pair and stores the concept length as well as the F1Measures.
     *
     * Will remove all previously evaluations
     *
     * @param answers the learning problem - concept pairs to evaluate
     */
    public void evaluate(Set<Pair<LearningProblem, String>> answers){
        //empty everything
        resultContainers.clear();
        f1MeasureCalculator.clear();

        //for each Pair evaluate
        for (Pair<LearningProblem, String> answer : answers) {
            ResultContainer container = evaluate(answer.getFirst(), answer.getSecond());
            //store conceptLength
            this.resultContainers.add(container);
        }
    }

    /**
     * Prints a List of concept - F1 measure and concept Length into the standard output
     */
    public void printTable(){
        List<List<Object>> table = new ArrayList<>();
        for (ResultContainer resultContainer : resultContainers) {
            //add CONCEPT, F1, PRECISION, RECALL, CONCEPT_LENGTH
            table.add(
                    Lists.newArrayList(
                            resultContainer.getConcept(),
                            resultContainer.getF1Result().getF1measure(),
                            resultContainer.getF1Result().getPrecision(),
                            resultContainer.getF1Result().getRecall(),
                            resultContainer.getConceptLength()
                    )
            );
        }
        // Add macro and micro F1 to table
        F1Result macroF1 = f1MeasureCalculator.calculateMacroF1Measure();
        F1Result microF1 = f1MeasureCalculator.calculateMicroF1Measure();
        table.add(Lists.newArrayList("MACRO", macroF1.getF1measure(), macroF1.getPrecision(), macroF1.getRecall(), 0));
        table.add(Lists.newArrayList("MICRO", microF1.getF1measure(), microF1.getPrecision(), microF1.getRecall(), 0));

        // print the whole table.
        TablePrinter.print(table, Lists.newArrayList("concept", "f1measure", "precision", "recall", "concept-length"), "%50s\t%5f\t%3d");

    }

    public List<Integer> getConceptLengths() {
        return resultContainers.stream().map(ResultContainer::getConceptLength).collect(Collectors.toList());
    }

    public F1Result getMacroF1Measure(){
        return f1MeasureCalculator.calculateMacroF1Measure();
    }

    public F1Result getMicroF1Measure(){
        return f1MeasureCalculator.calculateMicroF1Measure();
    }

}
