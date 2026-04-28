FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/userservice-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8111
ENTRYPOINT ["java","-jar","app.jar"]
