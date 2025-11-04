FROM eclipse-temurin:21-jre

WORKDIR /app

COPY stats-server/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=9090"]