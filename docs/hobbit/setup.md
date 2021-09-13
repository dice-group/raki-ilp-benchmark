RAKI ILP Benchmark integration for HOBBIT.

This setup will guide you through the steps to create the RAKI ILP Benchmark on your local machine using a local Hobbit.

## Preparations

1. You need an Account on Hobbit at https://git.project-hobbit.eu and permission on the project
2. You need to use a local HOBBIT deployment if you want to use private datasets
3. You need to Clone this Repository

### 2. Use a local HOBBIT deployment

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

### 3. Clone this Repo 

```bash
git clone https://github.com/dice-group/raki-ilp-benchmark 
cd  raki-ilp-benchmark
git checkout raki-private
```

## Add a Dataset

Please see [Add a Benchmark to Hobbit](add-benchmark.md)

## Build 

Set build.sh to an executable

`chmod +x build.sh`

If you've done your changes:
`./build.sh`

It will automatically build the Docker images for the private RAKI benchmark.


## Access Raki-private

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

