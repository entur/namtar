FROM openjdk:11-jre

RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser

WORKDIR /home/appuser

RUN chown -R appuser:appuser /home/appuser
USER appuser

RUN mkdir tmp

ADD target/namtar-*-SNAPSHOT.jar namtar.jar

EXPOSE 8080
CMD java $JAVA_OPTIONS -jar namtar.jar
