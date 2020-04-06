FROM openjdk:11-jre
ADD target/namtar-*-SNAPSHOT.jar namtar.jar
EXPOSE 8080
CMD java $JAVA_OPTIONS -jar /namtar.jar
