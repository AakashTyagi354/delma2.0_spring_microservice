FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/notificationservice-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8017
ENTRYPOINT ["java","-jar","app.jar"]
