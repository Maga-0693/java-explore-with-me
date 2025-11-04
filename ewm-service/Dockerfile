FROM openjdk:21-jdk

WORKDIR /app

COPY ewm-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8081"]