#!/bin/sh
set -eu

mvn clean install -Dmaven.test.skip=true
mvn -f raki-system-adapter clean package
mvn clean package -P shaded


sudo docker build -f raki-hobbit/controller.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakibenchmark raki-hobbit
sudo docker build -f raki-hobbit/datagenerator.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakidatagenerator raki-hobbit
sudo docker build -f raki-hobbit/taskgenerator.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakitaskgenerator raki-hobbit
sudo docker build -f raki-hobbit/evaluation.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakievaluationmodule raki-hobbit
sudo docker build -f raki-system-adapter/ontolearn-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/ontolearn raki-system-adapter
sudo docker build -f raki-system-adapter/dllearner-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/dllearner raki-system-adapter
