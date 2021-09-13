# Add A System

In this section we described how you can add your RAKI ILP system to Hobbit

We assume that you directly extend the `raki-system-adapter` module.
Otherwise you need to add the following dependency to your system

```xml
<dependency>
        <groupId>org.dice_group.raki</groupId>
        <artifactId>raki-hobbit</artifactId>
        <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Create your System

There are two ways, either

* the system has a Java API, or
* the system has an HTTP endpoint

### Using the Java API

This way  you need to implement two methods

```java
public class MySystem extends AbstractRakiSystemAdapter {

    
    @Override
    public String createConcept(String posNegExample) throws IOException, Exception {
        LearningProblem problem = LearningProblem.create(posNegExample);
        //TODO your concept learning
        
        //return your concept in manchester syntax
        return concept;
    }

    @Override
    public void loadOntology(File ontologyFile) throws IOException, Exception {
        //TODO load the ontology here.
    }
}
```

### Using HTTP

If you're using an HTTP concept learner you need to implement mainly one function, the function to start the system.

```java
public class MySystem extends AbstractHTTPSystemAdapter {

    private static final String baseUri ="http://localhost:9080";

    @Override
    protected String convertToManchester(String concept) throws OWLOntologyCreationException, IOException {
        //TODO this can be used if your system doesn't provide manchester syntax directly
        
        return concept;
    }

    public MySystem() {
        super(baseUri);
    }

    @Override
    public void startSystem(String ontologyFile) throws Exception {
        //TODO start and load your system using the ontology file 
        // Be aware that the AbstractHTTPSystemAdapter waits until the system provides the status ready
        // and you don't have to implement that
    }
}
```

Additionally, The HTTP endpoint needs to implement the following two paths

* `GET /status` 
* `POST /concept_learning`

#### Status

the status endpoint needs return if the Concept Learner is ready to be queried.
It should return an HTTPCode 200 and the following json message if that is the case

```json
{
  "status": "ready"
}
```

#### Concept_learning

The concept_learner will need to accept a Learning Problem in JSON format as a POST request and
should return the concept for this problem. 

Ideally the concept will be directly in manchester syntax, 
if that is not the case however it is possible to convert the concept to manchster syntax inside the System wrapper itself by overriding the following method

```java
  @Override
    protected String convertToManchester(String concept) throws OWLOntologyCreationException, IOException {
        //TODO this can be used if your system doesn't provide manchester syntax directly
        
        return concept;
    }
```

## Add System to Hobbit

Now that we create that system we can add it to Hobbit

### Create a system.ttl

Create a repository in https://git.project-hobbit.eu

with the following `system.ttl`

```ttl
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix hobbit: <http://w3id.org/hobbit/vocab#> .
@prefix raki: <http://w3id.org/raki/hobbit/vocab#> .


raki:MySystem a  hobbit:SystemInstance;
	rdfs:label	"MySystem"@en;
	rdfs:comment	"The MySystem system"@en;
	hobbit:imageName "git.project-hobbit.eu:4567/raki/mySystem";
	hobbit:implementsAPI raki:Private-API, raki:API .
	
```

### Create the Docker container

Now we can create a Dockerfile, build and push that docker container to the Hobbit registry

The Dockerfile should look like

```dockerfile
FROM java

ADD target/raki-system-adapter-1.0.0-SNAPSHOT.jar /raki/systems.jar

WORKDIR /raki

CMD java -cp systems.jar org.hobbit.core.run.ComponentStarter org.dice_group.raki.hobbit.systems.test.TestSystem
```

However, as long as the system is executed the same way as the last line you can do whatever before

Now let's build the docker container

```bash
docker build -f Dockerfile -t IMAGE_NAME .
```

Be aware that the Image name is predetermined by your repository.

if your repository is located at `https://git.project-hobbit.eu/raki/mysystem` 
your Image name will be `git.project-hobbit.eu:4567/raki/mysystem`

If you're using a local deployment of Hobbit that's it (make sure that you set DOCKER_AUTOPULL to 0 in the Hobbit platform)

If you want to use the system on the https://master.project-hobbit.eu push the repository

```bash
docker push IMAGE_NAME
```

If that doesn't work make sure that you used `docker login` to login to the Docker Hobbit registry