FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven wrapper and project files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw

# Download dependencies (improves build cache usage)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests -B

# Use a minimal JDK image for running the app
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]