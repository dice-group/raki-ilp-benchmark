mvn clean install -Dmaven.test.skip=true

cd raki-benchmark && docker build -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakibenchmark . && cd ..
cd raki-datagenerator &&  docker build -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakidatagenerator . && cd ../
cd raki-taskgenerator  && docker build -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakitaskgenerator . && cd ../
cd raki-evaluation && docker build -t git.project-hobbit.eu:4567/raki/raki-private/raki-benchmark/rakievaluationmodule . && cd ../
#cd raki-system-adapter && docker build -f ontolearn-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/ontolearn . && cd ../
cd raki-system-adapter && docker build -f dllearner-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-private/raki-systems/dllearner . && cd ../


