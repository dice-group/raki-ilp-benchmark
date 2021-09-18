package org.dice_group.raki.core.evaluation;

import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.commons.math3.util.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.dice_group.raki.core.concepts.ManchesterSyntaxParser;
import org.dice_group.raki.core.evaluation.f1measure.F1MeasureCalculator;
import org.dice_group.raki.core.evaluation.f1measure.F1Result;
import org.dice_group.raki.core.ilp.LearningProblem;
import org.dice_group.raki.core.utils.TablePrinter;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Evaluator will calculate the F1Measures and Concept Lengths for
 * the provided learning problems and their corresponding solution, represented as a Concept ({@link org.semanticweb.owlapi.model.OWLClassExpression})
 */
public class Evaluator {

    private final F1MeasureCalculator f1MeasureCalculator = new F1MeasureCalculator();

    private final List<ResultContainer> resultContainers = new ArrayList<>();
    private final OWLOntology ontology;
    private final boolean useConcepts;
    private final OWLReasoner reasoner;
    private final ManchesterSyntaxParser manchesterParser;

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
        this.useConcepts = useConcepts;
        this.reasoner = createReasoner();
        this.manchesterParser = new ManchesterSyntaxParser(ontology, owlBaseOntology);
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
     * ## Example
     *
     * ```java
     *         Evaluator evaluator = new Evaluator(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology(), useConcepts);
     *         ResultContainer actual = evaluator.evaluate(problem, concept);
     *
     * ```
     *
     * @param problem the problem which was solved
     * @param answerConcept the concept which is the solution for the problem
     * @return The result container, containing the {@link F1Result} and the conceptLength
     */
    public ResultContainer evaluate(LearningProblem problem, String answerConcept){
        OWLClassExpression expr = manchesterParser.parse(answerConcept);

        //calculate length.
        ConceptLengthCalculator calculator = new ConceptLengthCalculator();
        calculator.render(expr);
        int conceptLength = calculator.getConceptLength();

        //retrieve OWLNamedIndividuals for answerConcept
        Collection<String> answers = retrieveIndividuals(expr);
        return evaluate(problem, answers, conceptLength, answerConcept);
    }

    /**
     * Helper function. Shouldn't be used directly.
     *
     * use [evaluate(LearningProblem, String)]
     *
     * @param problem
     * @param answers
     * @param conceptLength
     * @param answerConcept
     * @return
     */
    public ResultContainer evaluate(LearningProblem problem, Collection<String> answers, int conceptLength, String answerConcept) {
        //retrieve positive uris from either problem or if useConcept is set to all positives
        Collection<String> positiveUris = retrievePositiveUris(problem);

        //retrieve negative uris from either problem or if useConcept is set to empty list
        Collection<String> negativeUris = retrieveNegativeUris(problem);

        // count tp, fp, fn
        int truePositives, falsePositives, falseNegatives;
        int[] vals = evaluate(positiveUris, negativeUris, answers);
        truePositives = vals[0];
        falsePositives = vals[1];
        falseNegatives = vals[2];

        //calculate f1 Result
        F1Result f1Result = f1MeasureCalculator.addF1Measure(truePositives, falsePositives, falseNegatives);

        ResultContainer container = new ResultContainer(
                answerConcept,
                f1Result,
                conceptLength);
        this.resultContainers.add(container);
        return container;
    }

    /**
     * Retrieves all Individuals fitting to the provided concept listed in the provided ontology.
     *
     * @param concept the concept to retrieve the Individuals for
     * @return the set of Individuals fitting to this concept in String representation (is IRI)
     */
    private Collection<String> retrieveIndividuals(OWLClassExpression concept){
        //retrieve all individuals and map the IRIs to a set of strings.
        return reasoner.getInstances(concept, false).getFlattened()
                .stream()
                .map(
                        expr -> expr.getIRI().toString()).collect(Collectors.toSet()
                );
    }

    /**
     * Retrieves negative URIs from the learning problem.
     *
     * Either return the set of negative uris listed in the provided problem or if useConcept is set, will just return null.
     *
     * Be aware that it is defined if the negative uris are null, simply all non positives are considered negative.
     *
     * @param problem the learning problem to retrieve the negative uris from
     * @return the set of negative uris listed in the problem or null if useConcept is true
     */
    private Collection<String> retrieveNegativeUris(LearningProblem problem){
        if(useConcepts){
            // we defined null to be used if all non positives are negative (and not just examples)
            return null;
        }
        return problem.getNegativeUris();
    }

    /**
     * Retrieves the positive uris either from the learning problem itself or by
     * retrieving all instances fitting to the concept in the learning problem if useConcept is true
     * and the learning problem contains a concept
     *
     * @param problem the learning problem to retrieve the positve uris or the concept from
     * @return The set of positive uris.
     */
    private Collection<String> retrievePositiveUris(LearningProblem problem){
        //check if concept should be used to retrieve positives, rather than the listed examples.
        //also check if the problem has a concept listed, otherwise use the listed examples.
        if(useConcepts && problem.hasConcept()){
            //retrieve all positive concepts instead of only the examples in the problem
            OWLClassExpression expr = manchesterParser.parse(problem.getConcept());
            return retrieveIndividuals(expr);
        }
        return problem.getPositiveUris();
    }

    /**
     * Evaluates the positive uris and the negative uris against the answer uris.
     * And returns the true positive, false positive and false negative counts.
     *
     * If the negative uris are null, it will assume that all uris not in positiveUris are negative uris.
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
            //if negative Uris is null, we assume that all non-positive individuals are negative
            else if(negativeUris == null || negativeUris.contains(answer)){
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
     * ## Example
     *
     * ```java
     *         Evaluator evaluator = new Evaluator(ontology, OWLManager.createConcurrentOWLOntologyManager().createOntology(), useConcepts);
     *         evaluator.evaluate(problemPairs);
     *         F1Result actualMacro = evaluator.getMacroF1Measure();
     *         F1Result actualMicro = evaluator.getMicroF1Measure();
     * ```
     *
     *
     * @param answers the learning problem - concept pairs to evaluate
     */
    public void evaluate(Set<Pair<LearningProblem, String>> answers){
        //empty everything
        resultContainers.clear();
        f1MeasureCalculator.clear();

        //for each Pair evaluate
        for (Pair<LearningProblem, String> answer : answers) {
            evaluate(answer.getFirst(), answer.getSecond());
            //store conceptLength
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
                            resultContainer.getConcept().replace("\n", " "),
                            resultContainer.getF1Result().getF1measure(),
                            resultContainer.getF1Result().getPrecision(),
                            resultContainer.getF1Result().getRecall(),
                            resultContainer.getConceptLength(),
                            resultContainer.getResultTimeMs()
                    )
            );
        }
        // Add macro and micro F1 to table
        F1Result macroF1 = f1MeasureCalculator.calculateMacroF1Measure();
        F1Result microF1 = f1MeasureCalculator.calculateMicroF1Measure();
        table.add(Lists.newArrayList("MACRO", macroF1.getF1measure(), macroF1.getPrecision(), macroF1.getRecall(), 0, 0));
        table.add(Lists.newArrayList("MICRO", microF1.getF1measure(), microF1.getPrecision(), microF1.getRecall(), 0, 0));

        // print the whole table.
        TablePrinter.print(table, Lists.newArrayList("concept", "f1measure", "precision", "recall", "concept-length", "result-times-ms"), "%50s\t%5f\t%5f\t%5f\t%5d\t%5d");

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
