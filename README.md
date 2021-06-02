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
