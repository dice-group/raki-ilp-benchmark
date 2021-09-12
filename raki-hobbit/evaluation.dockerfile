FROM java

ADD target/raki-hobbit-1.0.0-SNAPSHOT.jar /raki/evalModule.jar
ADD owl.ttl /raki/owl.ttl


WORKDIR /raki

CMD java -cp evalModule.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.evaluation.RakiEvaluation
