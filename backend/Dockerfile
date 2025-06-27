FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app


COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw

RUN ./mvnw -q dependency:go-offline

COPY src ./src

RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"] 