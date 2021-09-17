FROM openjdk:11

ADD target/raki-hobbit-1.0.0-SNAPSHOT.jar /raki/controller.jar

WORKDIR /raki

CMD java -cp controller.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.benchmark.RakiBenchmark
