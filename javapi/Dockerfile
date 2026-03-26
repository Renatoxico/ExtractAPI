# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

# Runtime stage (smaller image, no build tools)
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

COPY --from=builder /app/target/API-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
