We will describe the Core API in this section


## Learning Problem 

### Description 
A learning problem is an object containing of a set of positive uris, negative uris and an optional concept.

The positive uris as well as the negative uris are named individuals of the ABox of an OWL Ontology. 
The concept is a class expression representation of this problem.
It is build by concepts of the TBox of the Ontology.

The learning problem is described using JSON. 

An Example:

```json
{
  "positives" : [
    "http://ontology.com/individuals/Human1",
    "http://ontology.com/individuals/Human3",
    "http://ontology.com/individuals/Human7"
  ],
  "negatives": [
    "http://ontology.com/individuals/Bear1",
    "http://ontology.com/individuals/Dragon1",
    "http://ontology.com/individuals/Eagle1"
  ],
  "concept" : "ontology:Human"
}
```

#### Why is the gold standard concept optional

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

### API 

#### Creating a Learning Problem

A learning problem can be created using the `LearningProblemFactory`.

We will define `positives` and `negatives` each as a `Collection<String>` for simplicity.
These collections contain the positive and negative uris.

##### Creating from Lists.

```java
LearningProblem problem = LearningProblemFactory.create(positives, negatives);
```

##### Creating from JSON String

```java
String jsonString = "{  \"positives\": [...], \"negatives\": [...]  }"
LearningProblem problem = LearningProblemFactory.parse(jsonString);
```

##### Creating multiple Learning Problems from JSON String

```java
String jsonString = "[ {  \"positives\": [...], \"negatives\": [...]  }, {  \"positives\": [...], \"negatives\": [...]  }]"
Set<LearningProblem> problems = LearningProblemFactory.readMany(jsonString);
```

##### Read learning Problems from file

```java
Set<LearningProblem> problems = LearningProblemFactory.readMany(new File("learningProblems.json"));
```


## Concepts - Manchester Parser 

### Description 

A Concept is a Class Expression derived from the TBox of an OWL Ontology. 
F.e. 

```owl
Human or Dragon
```
describes the Concept of all humans and dragons.

The Format used in RAKI ILP is the Manchester Syntax. 

### API

The API has a `ManchesterSyntaxParser`. 
This parser allows to parse a Manchester Syntax String to an `OWLClassExpression` 
and rendering an `OWLClassExpression` to a Manchester Syntax String.


It needs a main ontology, and an optional OWL base ontology (containing the basics of OWL, such as `owl:Thing`). 
Furhter on it 

```java
ManchesterSyntaxParser parser = new ManchesterSyntaxParser(mainOntology, baseOntology);

OWLClassExpression expr = parser.parse("ontology:Human or ontology:Dragon");

# concept = 'Human or Dragon'
String concept = parser.render(expr);
```


## Benchmark Configuration

### Description

The Benchmark Configuration consists of three elements

* The benchmark name
* The file path containing the Ontology
* The file path containing the learning problems

Configurations can be read from a YAML file or created simply by using the `Configuration` constructor

The YAML file has to look like this

```yaml
datasets:
  - name: "MyBenchmarkName1"
    learningProblem: "/path/to/learningProblems1.json"
    dataset: "/path/to/ontology1.owl"
  - name: "MyBenchmarkName2"
    learningProblem: "/path/to/learningProblems2.json"
    dataset: "/path/to/ontology2.owl"
```

The Learning problem has to be a json file. 
An example can be seen above.

### API

Creating from file

```java
//load all Configurations
Configurations confs = Configurations.load(new File("/path/to/config.yml"));

//retrieve the ones with your benchmark name
Configuration myBenchmark = confs.getConfiguration("MyBenchmarkName1");
```

Using the Configuration to retrieve the `OWLOntology` and the Set of `LearningProblems`

```java
OWLOntology ontology = configuration.readOntology();

Set<LearningProblem> problems = configuration.readLearningProblems();
```

## Metrics 

Currently, the core can calculate two metrics

* F1-Measure, Precision and Recall
* The Concept Length of a Concept

### F1-Measure

The F1 measure will be calculated using the `F1MeasureCalculator`.

The calculator will calculate the f1-score, precision and recall 
from the `true positives`, `false positives` and `false negatives` provided
and stores the results internally as well to calculate the macro and micro f1 measure later on.


#### Create an F1Result

```java
F1MeasureCalculator calculator = new F1MeasureCalculator();
F1Result result = calculator.addF1Measure(truePositives, falsePositives, falseNegatives);
```

The results can then be queried from the `F1Result` object 

```java
double precision = result.getPrecision();
double recall = result.getRecall();
double f1score = result.getF1measure();
```

#### Create macro and micro F1 scores

```java
F1MeasureCalculator calculator = new F1MeasureCalculator();

// Calculate and add the F1 scores for some truePositives, falsePositives and falseNegatives
calculator.addF1Measure(truePositives, falsePositives, falseNegatives);
calculator.addF1Measure(truePositives, falsePositives, falseNegatives);

//the scores will be stored internally and be used to calculate the Micro and Macro F1Measures
F1Result macroResults = calculator.calculateMacroF1Measure();
F1Result microResults = calculator.calculateMicroF1Measure();
```

To clear the stored values use the `clear` method
```java
calculator.clear();
```

### Concept Length 

The concept length of an `OWLClassExpression` can be calculated by using the
`ConceptLengthCalculator`.

Currently, only the following syntax structures are supported:

* AND
* OR
* SOME
* ALL 
* NOT

```java
ConceptLengthCalculator calculator = new ConceptLengthCalculator();

//Create your concept/class expression
OWLClassExpression expr = ... ;

calculator.render(expr);
int lengthOfExpr = calculator.getConceptLength();
```

## Evaluator

### Description

The `Evaluator` evaluates a set of Learning problems against a set of concepts.

Additionally, the `Evaluator` can evaluate a single Learning Problem against a single concept.
The single evaluation will return a `ResultContainer` containing the F1 Measures and Concept Length.

If either the `Evaluator` was used, by using a set of Learning Problem and Concept pairs or
the single evaluation was executed multiple times, the `Evaluator` can return the Macro and Micro F1 measures
as well as all concept lengths.

It needs a main Ontology, the owl base Ontology (can be empty though) and if the gold standard concept should be used
instead of the positive and negative uris inside the learning problems.

### API

Create the evaluator

```java
boolean useConcepts = false;
Evaluator evaluator = new Evaluator(mainOntology, owlBaseOntology, useConcepts);
```

Execute a single execution

```java
//create your learning problem
LearningProblem problem = ...;

//create the answer concept
String concept = "ontology:Human"

ResultContainer container = evaluator.evaluate(learningProblem, concept);

int conceptLength = container.getConceptLength();
F1Result f1result = container.getF1Result();
```

Execute multiple LearningProblem, Concept pairs.
Each Pair consists of a Learning Problem and the corresponding concept.

```java
//Create your problem Pairs
Set<Pair<LearningProblem, String>> problemPairs = ...;


evaluator.evaluate(problemPairs);

//Now we can retrieve the macro and micro F1 measures, as well as all Concept Lengths as following
F1Result macroF1 = evaluator.getMacroF1Measure();
F1Result microF1 = evaluator.getMicroF1Measure();
List<Integer> conceptLengths = evaluator.getConceptLengths();
```

Further on all available Metrics can be printed as a table to the standard output using:
```java
evaluator.printTable();
```

## Table Printer

The `TablePrinter` is a helper class to print a table in a kinda nice format.

```java
List<String> header = new ArrayList<>();
header.add("Name");
header.add("Description");
header.add("Salary");

List<List<Object>> table = new ArrayList<>();
table.add(Lists.newArrayList("Joe", "Consultant in IT", 5000));
table.add(Lists.newArrayList("Mary", "Engineer in IT", 8000));
table.add(Lists.newArrayList("Lina", "Security Expert in IT", 9500));
TablePrinter.print(table, header, "%10s %20s %5d");
```

will produce

```
---------------------------------------------------------------------------
      Name          Description Salary
---------------------------------------------------------------------------
       Joe     Consultant in IT  5000
      Mary       Engineer in IT  8000
      Lina Security Expert in IT  9500
---------------------------------------------------------------------------
```