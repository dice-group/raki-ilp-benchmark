# raki-ipl-benchmark
RAKI IPL Bencharmk integration for HOBBIT

## How to add a Dataset

Fork this Repository.

```
cd raki-datagenerator/data/
mkdir YOUR_BENCHMARK_NAME
cd YOUR_BENCHMARK_NAME/
```

Add your Ontology `ontology.owl` into `raki-datagenerator/data/YOUR_BENCHMARK_NAME/` and create a file called `lp.json` where you put the learning problems in the following format

```json
[
  {
    "positives": ["http://example.com/positive1", "http://example.com/positive2" ...],
    "negatives": ["http://example.com/negative1", "http://example.com/negative2" ...]
  },
  ...
]
```

Now edit `raki-datagenerator/src/main/resources/benchmark.yaml`

and add the following at the end
```yaml
  - name: "http://w3id.org/raki/hobbit/vocab#YOUR-BENCHMARK-NAME"
    dataset: "/raki/data/YOUR-BENCHMARK-NAME/ontology.owl"
    learningProblem: "/raki/data/YOUR-BENCHMARK-NAME/lp.json"
```

Create a Pull Request. Wait until PR is approved and closed. 

## Use Ontolearn 

To use the Ontolearn adapter you need to create embeddings in https://github.com/dice-group/DAIKIRI-Embedding using ConEx on your dataset. 
and add the embeddings to Ontolearn in `Ontolearn/embeddings/ConEx_YOUR_DATASET_NAME/ConEx_entity_embeddings.csv` and `Ontolearn/pre_trained_agents/YOUR_DATASET_NAME/DrillHeuristic_averaging/DrillHeuristic_averaging.pth`

Add in `raki-system-adapter/src/main/resources/drill-mapping.properties` 

```properties
http\://dl-learner.org/mutagenesis=ConEx_YOUR_DATASET_NAME/ConEx_entity_embeddings.csv, YOUR_DATASET_NAME/DrillHeuristic_averaging/DrillHeuristic_averaging.pth
```

## Use a local HOBBIT deploymeny

See https://hobbit-project.github.io/quick_guide.html how to deploy HOBBIT locally and make sure to set `DOCKER_AUTOPULL: 0` in the config as described in https://hobbit-project.github.io/parameters_env.html  

```yaml
services:
  platform-controller:
    image: hobbitproject/hobbit-platform-controller:latest
    networks:
      - hobbit
      - hobbit-core
    environment:
      ...
      DOCKER_AUTOPULL: 0
```


Set build.sh to an executable

`chmod +x build.sh`

If you've done your changes:
`./build.sh` 

It will automatically build the dockers for the data generator and the Ontolearn system. 
