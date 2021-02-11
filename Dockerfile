FROM openjdk:11-jre
ADD target/namtar-*-SNAPSHOT.jar namtar.jar

RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser

WORKDIR /home/appuser

RUN chown -R appuser:appuser /home/appuser
USER appuser

RUN mkdir tmp

EXPOSE 8080
CMD java $JAVA_OPTIONS -jar namtar.jar
