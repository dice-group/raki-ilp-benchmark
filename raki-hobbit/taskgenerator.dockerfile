FROM java

ADD target/raki-hobbit-1.0.0-SNAPSHOT.jar /raki/taskgenerator.jar

WORKDIR /raki

CMD java -cp taskgenerator.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.taskgenerator.RakiTaskGenerator
