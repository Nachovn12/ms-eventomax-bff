FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:25
WORKDIR /app
RUN groupadd --system eventomax && useradd --system --gid eventomax --home-dir /app --shell /usr/sbin/nologin eventomax
COPY --from=build --chown=eventomax:eventomax /workspace/target/ms-eventomax-bff-0.0.1-SNAPSHOT.jar /app/app.jar
USER eventomax
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
