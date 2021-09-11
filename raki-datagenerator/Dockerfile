FROM java

ADD target/raki-org.dice_group.raki.hobbit.datagenerator-1.0-SNAPSHOT.jar /raki/org.dice_group.raki.hobbit.datagenerator.jar
ADD src/main/resources/benchmark.yaml /raki/benchmark.yaml
COPY data/ /raki/data/


WORKDIR /raki

CMD java -cp org.dice_group.raki.hobbit.datagenerator.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.org.dice_group.raki.hobbit.datagenerator.RakiDataGenerator
