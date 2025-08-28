FROM  eclipse-temurin:11-jre

# The default user "ubuntu" must be removed, otherwise it supercedes the user "appuser" created
# in this Dockerfile
RUN userdel -r ubuntu

WORKDIR /home/appuser
COPY target/namtar-*-SNAPSHOT.jar namtar.jar
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
RUN mkdir -p /home/appuser/tmp && chown -R appuser:appuser /home/appuser
USER appuser
EXPOSE 8080
CMD  [ "java", "-jar", "namtar.jar"]
