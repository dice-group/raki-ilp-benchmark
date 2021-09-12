cd raki-core && mvn clean install -Dmaven.test.skip=true
cd raki-hobbit && mvn clean install -Dmaven.test.skip=true

cd raki-hobbit && docker build -f controller.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakibenchmark . && cd ..
cd raki-hobbit && docker build -f datagenerator.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakidatagenerator . && cd ../
cd raki-hobbit && docker build -f taskgenerator.dockerfile -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakitaskgenerator . && cd ../
cd raki-hobbit && docker build -t evaluation.dockerfile git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakievaluationmodule . && cd ../
cd raki-system-adapter && docker build -f ontolearn-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/ontolearn . && cd ../
cd raki-system-adapter && docker build -f dllearner-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/dllearner . && cd ../


