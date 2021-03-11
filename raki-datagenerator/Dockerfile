FROM java

ADD target/raki-datagenerator-1.0-SNAPSHOT.jar /raki/datagenerator.jar
ADD src/main/resources/benchmark.yaml /raki/benchmark.yaml
COPY data/ /raki/data/


WORKDIR /raki

CMD java -cp datagenerator.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.datagenerator.RakiDataGenerator
