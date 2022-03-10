# raki-ilp-benchmark
RAKI ILP Benchmark integration for HOBBIT.

This README will guide you through the steps to add your own datasets, assuming you have the rights on the raki project in HOBBIT. 

# Preparations

1. You need an Account on Hobbit at https://git.project-hobbit.eu and permission on the project
2. You need to use a local HOBBIT deployment if you want to use private datasets
3. You need to Clone this Repository

## 2. Use a local HOBBIT deployment

See https://hobbit-project.github.io/quick_guide.html how to deploy HOBBIT locally and make sure to set `DOCKER_AUTOPULL: 0` in the config as described in https://hobbit-project.github.io/parameters_env.html

```yaml
services:
  platform-controller:
    image: hobbitproject/hobbit-platform-controller:latest
    # ...
    environment:
      # ...
      DOCKER_AUTOPULL: 0
```

## 3. Clone this Repo 

```bash
git clone https://github.com/dice-group/raki-ilp-benchmark 
cd  raki-ilp-benchmark
git checkout raki-private
```

# Add a Dataset

This involves a few straight forward steps

1. You need to create a URL we call benchmark ID for HOBBIT
2. Add the dataset(s) to this repository and set the configurations accordingly, so the system recognizes your dataset
3. Add the benchmark ID to HOBBIT. 

## Create a BENCHMARK ID

Create a benchmark ID like the following 
`http://w3id.org/raki/hobbit/vocab#YOUR-BENCHMARK-NAME` 

we will use `YOUR_BENCHMARK_URI` as a placeholder fo this ID throughout this README.


## How to add a Dataset

Create the directory where you'll put your benchmark dataset in.

```
mkdir raki-hobbit/data/YOUR_BENCHMARK_NAME
```

Add your Ontology `ontology.owl` into `raki-hobbit/data/YOUR_BENCHMARK_NAME/`

Be aware that your Ontology needs an Ontology ID which you can set in the ontology like the following. Let's assume that your ID is `http://example.com/MY-ID`

```xml
<rdf:RDF xmlns="http://example.com/MY-ID"
xml:base="http://example.com/MY-ID"
xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
xmlns:owl="http://www.w3.org/2002/07/owl#"
xmlns:xml="http://www.w3.org/XML/1998/namespace"
xmlns:dl="http://dl-learner.org/benchmark/dataset/animals/"
xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
<owl:Ontology rdf:about="http://example.com/MY-ID"/>
<!-- YOUR ONTOLOGY HERE -->
</rdf:RDF>
```

Now create a file called `lp.json` in `raki-hobbit/data/YOUR_BENCHMARK_NAME/` where you put the learning problems in the following format:

```json
[
  {
    "positives": ["http://example.com/positive1", "http://example.com/positive2" ...],
    "negatives": ["http://example.com/negative1", "http://example.com/negative2" ...]
  },
  ...
]
```

### Add the dataset to the benchmark configuration

Edit `raki-hobbit/src/main/resources/benchmark.yaml` and add the following at the end

```yaml
  - name: "YOUR_BENCHMARK_URI"
    dataset: "/raki/data/YOUR-BENCHMARK-NAME/ontology.owl"
    learningProblem: "/raki/data/YOUR-BENCHMARK-NAME/lp.json"
```

## Add the dataset to HOBBIT

This is the only step which is public. 

go to https://git.project-hobbit.eu/raki/raki-private/raki-benchmark and edit the `benchmark.ttl` 

Add the following to the end

```properties
<YOUR_BENCHMARK_URI> a raki:Datasets;
		rdfs:label "My Benchmark Name"@en;
		rdfs:comment "Description of My Benchmark Name"@en .

```

Be aware: Folks may see the name of the dataset. (Use an obscured one if you don't want them, however it needs to be obscured in the previous steps as well.)

# Use Ontolearn as a system

1. Get pre defined embeddings and trained datasets
```
wget "https://github.com/dice-group/DRILL/blob/main/embeddings.zip?raw=true" -O raki-system-adapter/embeddings.zip
wget "https://github.com/dice-group/DRILL/blob/main/pre_trained_agents.zip?raw=true" -O raki-system-adapter/pre_trained_agents.zip
```

2. Unzip them to add your datasets
```
unzip raki-system-adapter/embeddings.zip -d raki-system-adapter
unzip raki-system-adapter/pre_trained_agents.zip -d raki-system-adapter
```

To use the Ontolearn adapter you need to create embeddings in https://github.com/dice-group/DAIKIRI-Embedding using ConEx on your dataset. 

add the embeddings and pre-trained agents to the corresponding folder in `embeddings/ConEx_YOUR_DATASET_NAME/ConEx_entity_embeddings.csv` and `pre_trained_agents/YOUR_DATASET_NAME/DrillHeuristic_averaging/DrillHeuristic_averaging.pth`

Add in `raki-system-adapter/src/main/resources/drill-mapping.properties` using your previous declared Ontology ID (e.g. `http://example.com/MY-ID`)

```properties
http\://example.com/MY-ID=ConEx_YOUR_DATASET_NAME/ConEx_entity_embeddings.csv, YOUR_DATASET_NAME/DrillHeuristic_averaging/DrillHeuristic_averaging.pth
```

Now we need to zip the embeddings and pre_trained_agents again

```
zip -r raki-system-adapter/pre_trained_agents.zip raki-system-adapter/pre_trained_agents/
zip -r raki-system-adapter/embeddings.zip raki-system-adapter/embeddings/
```

# Build 

If you've done your changes:
`./build.sh`

It will automatically build the Docker images for the private RAKI benchmark.


# The Benchmark 

The benchmark is called `RAKI ILP Benchmark`. 

# Executing Hobbit

As the RAKI-private benchmark should be only accessible by members of the raki-private group 
add to the docker-compose.yml  the following

```yaml
services:
  platform-controller:
    image: hobbitproject/hobbit-platform-controller:latest
    networks:
      - hobbit
      - hobbit-core
    environment:
      ...
      GITLAB_USER: "YOUR_USER_NAME"
      GITLAB_EMAIL: "YOUR_EMAIL"
      GITLAB_TOKEN: "YOUR_TOKEN"
```
 
`YOUR_TOKEN` is a gitlab token you have to create in https://git.project-hobbit.eu  -> Settings -> Access Token -> check at least (api, read_repository, read_registry)

Now start the platform and go to http://localhost:8181 (Keycloack) -> Admin Console and login using the Keycloack admin account (see https://hobbit-project.github.io/quick_guide.html for initial credentials) 

Now add a dummy user (users -> add users) with the same name and email as your token user. 
Click again on `Users` and on `view all users` click on the ID of your newly created dummy user and click on the `Credentials` tab. 
Create a password for your user. 

Now you can login into localhost:8080 using your dummy user and should be able to access the `Raki ILP Benchmark - Priv`.


# Troubleshooting

If something doesn't work please let me know. 
