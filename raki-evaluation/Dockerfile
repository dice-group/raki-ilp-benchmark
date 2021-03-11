FROM java

ADD target/raki-evaluation-1.0-SNAPSHOT.jar /raki/evalModule.jar

WORKDIR /raki

CMD java -cp evalModule.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.evaluation.RakiEvaluation
