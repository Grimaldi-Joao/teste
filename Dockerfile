FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S motor && adduser -S motor -G motor && mkdir -p /app/data && chown -R motor:motor /app
COPY --from=build --chown=motor:motor /workspace/target/motor-ia-atendimento-0.0.1-SNAPSHOT.jar app.jar
USER motor
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
