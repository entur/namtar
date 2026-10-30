FROM bellsoft/liberica-openjdk-alpine:21.0.13.0.3-5
RUN apk update && apk upgrade && apk add --no-cache tini
WORKDIR /deployments
COPY target/namtar-*-SNAPSHOT.jar namtar.jar
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser
EXPOSE 8080
CMD [ "/sbin/tini", "--", "java", "-jar", "namtar.jar" ]
