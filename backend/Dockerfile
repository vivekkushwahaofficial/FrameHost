# Stage 1: Build the Spring Boot application.
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory inside the build container.
WORKDIR /app

# Copy Maven configuration first for better Docker layer caching.
COPY pom.xml .

# Copy application source code.
COPY src ./src

# Compile, run tests, and package the application.
RUN mvn -B -ntp clean package

# Stage 2: Run the packaged application.
FROM eclipse-temurin:21-jre

# Set the runtime working directory.
WORKDIR /app

# Copy the executable JAR from the build stage.
COPY --from=build --chown=10001:10001 \
    /app/target/framehost-backend-0.0.1-SNAPSHOT.jar \
    app.jar

# Run the application as a non-root user.
USER 10001:10001

# Document the application port.
EXPOSE 8080

# Start the Spring Boot application.
ENTRYPOINT ["java", "-jar", "app.jar"]
