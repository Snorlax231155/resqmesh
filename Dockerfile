FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the packaged jar file into our docker image
COPY target/resqmesh-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Set the startup command to execute the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
