# About

The RAKI ILP Benchmark is a benchmark developed for the RAKI project.

The benchmark aims to evaluate Explainable AI systems, by asking a system the best fitting concept for a Learning Problem.

Thus the system aims to describe the problem as best it can using an OWL ontology.

## What is a Learning Problem in this context?

A learning problem is a set of positive examples and negative examples, whereas one example is an Individual contained in the ABox of the benchmarked OWL ontology.

A learning problem may have also a gold standard concept which describes the learning problem.


Be aware: a Learning Problem does not have to contain a Concept. 

Imagine the following Ontology 

```
Class Human
Class Dragon

Disjoint: Humand, Dragon

Human1 is Human
Dragon1 is Dragon
Human2 is Human
```

Our Learning problem is:

```
positives: ["Human1"]
negatives: ["Human2", "Dragon1"]
```

There is no concept within the TBox of the ontology representing the Learning Problem fully.

## How does it work?

### Core


### Hobbit

The RAKI ILP Benchmark is integrated into [Hobbit](https://project-hobbit.eu).

For further Information how the ILP benchmark works with Hobbit and a walkthrough on how to use it, have a look [here](hobbit/overview)


## Where can I find the code?

The code is open source at https://github.com/dice-group/raki-ilp-benchmark and you can code with us if you want to :)
Where do I submit a bug or enhancement?

Please use the Github Issue Tracker at https://github.com/dice-group/raki-ilp-benchmark/issues