# Create Dockerfile for Spring Boot Application

# Step 1
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# set the work catalog
WORKDIR /app

# Copy the Maven configuration file
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# download dependencies（use Docker catche）
RUN mvn dependency:go-offline -B

# copy system core code
COPY src ./src

# establish app
RUN mvn clean package -DskipTests

# step 2 : implement
FROM eclipse-temurin:17-jre-alpine

# download tools
RUN apk add --no-cache curl bash

# build app users
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set the working directory
WORKDIR /app

# Copy Wait Script
COPY docker/scripts/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Copy the JAR file from the build stage
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Change file owner
RUN chown -R appuser:appgroup /app

# Switch to application user
USER appuser

# Exposed ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Startup Command
ENTRYPOINT ["/wait-for-it.sh", "mysql:3306", "--timeout=60", "--strict", "--", \
            "java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]