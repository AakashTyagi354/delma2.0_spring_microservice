FROM eclipse-temurin:21-jre
WORKDIR /app
COPY agents/opentelemetry-javaagent.jar opentelemetry-javaagent.jar
COPY categoryservice/target/categoryservice-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p logs
EXPOSE 8015
ENTRYPOINT ["java", \
  "-javaagent:opentelemetry-javaagent.jar", \
  "-jar", "app.jar"]
