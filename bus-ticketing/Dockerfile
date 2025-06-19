# Use official JDK image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy build artifact
COPY ../target/bus-ticketing.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
