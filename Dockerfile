# Stage 1: Build the app
FROM maven:3.9.16-eclipse-temurin-25 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY --from=builder /build/target/Hotel-Booking-Management-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]