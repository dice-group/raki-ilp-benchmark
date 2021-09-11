FROM java

ADD target/raki-taskgenerator-1.0-SNAPSHOT.jar /raki/taskgenerator.jar

WORKDIR /raki

CMD java -cp taskgenerator.jar org.hobbit.core.run.ComponentStarter  org.dice_group.raki.hobbit.taskgenerator.RakiTaskGenerator
