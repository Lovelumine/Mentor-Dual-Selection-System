FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -q -Dmaven.test.skip=true dependency:go-offline

COPY src src
COPY scripts scripts

RUN ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-openpyxl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY --from=build /workspace/scripts /app/scripts

EXPOSE 50000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
