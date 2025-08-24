# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper and project files into the container
COPY . .
RUN sed -i 's/\r$//' mvnw
RUN chmod +x ./mvnw
RUN chmod +x ./
#RUN ./mvnw clean package -DskipTests
RUN ./mvnw dependency:go-offline

# Expose the port the app runs on
EXPOSE 9090

# Run the application
#CMD ["java", "-jar", "target/API-0.0.1-SNAPSHOT.jar"]
CMD [ "./mvnw", "spring-boot:run" ]