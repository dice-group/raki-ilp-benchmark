
#cd raki-benchmark && docker build -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark/rakibenchmark . && cd ..
cd raki-datagenerator &&  docker build -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark/rakidatagenerator . && cd ../
#cd raki-taskgenerator  && docker build -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark/rakitaskgenerator . && cd ../
#cd raki-evaluation && docker build -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark/rakievaluationmodule . && cd ../
cd raki-system-adapter && docker build -f drill-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark-drill-system/drill-small . && cd ../
#cd raki-system-adapter && docker build -f dllearner-dockerfile.docker -t git.project-hobbit.eu:4567/raki/raki-ipl-benchmark-systems/dllearner . && cd ../


