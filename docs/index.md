# About

The RAKI ILP Benchmark is a benchmark developed for the RAKI project.

The benchmark aims to evaluate Explainable AI systems, by asking a system the best fitting concept for a Learning Problem.

Thus the system aims to describe the problem as best it can using an OWL ontology.

## What is a Learning Problem in this context?

A learning problem is a set of positive examples and negative examples, whereas one example is an Individual contained in the ABox of the benchmarked OWL ontology.

A learning problem may have also a gold standard concept which describes the learning problem.


## How does it work?

The RAKI ILP benchmark consists of two parts.

* The core part containing the actual API, and 
* The Hobbit part containing the Hobbit workflow

### Core

The Core evaluates a Learning problem and a corresponding concept, by retrieving all
Individuals for this concept from the provided OWL Ontology and compares
the retrieved individuals with the individuals stated in the Learning Problem.

The Learning Problem may not contain all positives and negative individuals.
However, the default is to check only against these individuals. 
That means all Individuals retrieved by the concept not occurring in either the positive nor the negative uris will be ignored.

This can be avoided by using a gold standard concept and setting the `useConcept` flag. 
This allows to retrieve the individuals for the LearningProblem from the Ontology as well. 

For further Information on the API, have a look [here](api/)


### Hobbit

The RAKI ILP Benchmark is integrated into [Hobbit](https://project-hobbit.eu).

For further Information how the ILP benchmark works with Hobbit and a walkthrough on how to use it, have a look [here](hobbit/overview)


## Where can I find the code?

The code is open source at https://github.com/dice-group/raki-ilp-benchmark and you can code with us if you want to :)
Where do I submit a bug or enhancement?

Please use the Github Issue Tracker at https://github.com/dice-group/raki-ilp-benchmark/issues