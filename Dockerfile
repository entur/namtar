FROM adoptopenjdk/openjdk8
ADD target/namtar-*-SNAPSHOT.jar namtar.jar
EXPOSE 8080
CMD java $JAVA_OPTIONS -jar /namtar.jar
