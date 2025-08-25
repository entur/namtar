FROM  eclipse-temurin:11-jre

RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser

RUN chown -R appuser:appuser /home/appuser
USER appuser

WORKDIR /home/appuser

RUN mkdir tmp

ADD --chown=appuser:appuser target/namtar-*-SNAPSHOT.jar namtar.jar

EXPOSE 8080
CMD java $JAVA_OPTIONS -jar namtar.jar
