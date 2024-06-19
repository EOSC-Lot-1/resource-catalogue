# vim:set syntax=dockerfile:

FROM maven:3.6.3-openjdk-17 AS maven-build
ARG profile="default"

RUN mkdir /app/
WORKDIR /app

COPY . .
RUN mvn package -B -P ${profile}


FROM eclipse-temurin:17-jre-alpine

WORKDIR /app/

RUN addgroup -g 1000 spring && adduser -u 1000 -H -D -G spring spring
RUN mkdir config logs && chown spring:spring config logs

COPY --from=maven-build --chown=spring:spring /app/resource-catalogue-service/target/resource-catalogue-service-*.jar /app/resource-catalogue-service.jar

EXPOSE 8080

USER 1000
CMD [ "java", "-jar", "/app/resource-catalogue-service.jar", \
  "--spring.main.allow-bean-definition-overriding=true" ]
