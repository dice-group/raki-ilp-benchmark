FROM openjdk:11

ADD target/raki-hobbit-1.0.0-SNAPSHOT.jar /raki/org.dice_group.raki.hobbit.datagenerator.jar
ADD src/main/resources/benchmark.yaml /raki/benchmark.yaml
COPY data/ /raki/data/

WORKDIR /raki

CMD java -cp org.dice_group.raki.hobbit.datagenerator.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.datagenerator.RakiDataGenerator
