FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/orderservice-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8013
ENTRYPOINT ["java","-jar","app.jar"]
