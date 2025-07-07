FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/claude-code-hooks-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]