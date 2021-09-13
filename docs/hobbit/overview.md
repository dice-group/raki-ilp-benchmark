In this section we will explain the overview of Hobbit. 

## Usage

TODO include pictures


## Parameters

TODO explain parameters

## Workflow 

The following image shows the time flow of the Raki Hobbit Modules
![Hobbit timing flow](../images/raki-hobbit-timing.drawio.png)

The RakiBenchmark and the RakiSystem will be created by the Hobbit System.

After that the RakiBenchmark creates the RakiTaskGenerator, the RakiDatagenerator and the RakiEvaluation.

The init phase of Hobbit ends and the RakiSystem and the RakiEvaluator needs the Ontology they should use.

The RakiDataGenerator will load the Ontology to be used and sends it to the RakiSystem and the RakiEvaluator.
To assure that it has fully send the Ontology, the RakiDataGenerator will then send a command to both modules, indicating it has fully send the ontology.

The RakiSystem and the RakiEvaluator will then load the ontology and each sends a command to 
the RakiTaskGenerator indicating they have loaded the Ontology and are readyto go.

In the same time, the RakiDataGenerator will send the RakiTaskGenerator the LearningProblems to the RakiTaskGenerator.
Each LearningProblem is one task and thus the DataGenerator will send each LearningProblem, and not all at once.
Be aware that the image simplifies this.

As soon as the RakiTaskGenerator got the Ontology loaded commands it will send the 
Learning problem to the system (without a gold standard concept) and the full learning problem to the RakiEvaluator.
Be aware that the image simplifies this by simply sending all learning problems instead of each one by one.

The RakiSystem will then send the concept it generated from the learning problem to the evaluation storage (which will then be send to the evaluator).

After the RakiTaskGeneratir and RakiSystem finished their tasks, the RakiBenchmark controller will send a command
to the RakiEvaluator to start the evaluation. (This is needed, as the RakiEvaluator sometimes ended prematurely)

As soon as the RakiEvaluator finished evaluation, it will send the Result model to the RakiBenchmark Controller.

