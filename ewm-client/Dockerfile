FROM eclipse-temurin:21-jre

WORKDIR /app

COPY ewm-client/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]