FROM bellsoft/liberica-openjdk-debian:25.0.5.0.3-5

RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser

WORKDIR /home/appuser

RUN chown -R appuser:appuser /home/appuser
USER appuser

ADD target/namtar-*-SNAPSHOT.jar namtar.jar

EXPOSE 8080
CMD java $JAVA_OPTIONS -jar /namtar.jar