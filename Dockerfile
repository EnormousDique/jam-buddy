FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY profile-app.jar app.jar

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]