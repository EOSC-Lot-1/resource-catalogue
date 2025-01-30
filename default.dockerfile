# vim:set syntax=dockerfile:

FROM maven:3.9.6-eclipse-temurin-21 AS maven-build
ARG profile="lot1"

RUN mkdir /app/
WORKDIR /app

COPY . .

# replace log4j2 classpath configuration
RUN cp docker/log4j2.xml resource-catalogue-service/src/main/resources/log4j2.xml

RUN mvn package -B -P ${profile} -DskipTests


FROM eclipse-temurin:21-jre-alpine

ARG git_commit=
ARG app_version=

ENV GIT_COMMIT="${git_commit}" APP_VERSION="${app_version}"

WORKDIR /app/

RUN addgroup -g 1000 spring && adduser -u 1000 -H -D -G spring spring

VOLUME /app/logs
RUN mkdir config logs && chown spring:spring config logs

COPY --from=maven-build --chown=spring:spring /app/resource-catalogue-service/target/resource-catalogue-service-*.jar /app/resource-catalogue-service.jar

EXPOSE 8080

USER 1000
CMD [ "java", "-jar", "/app/resource-catalogue-service.jar" ]

