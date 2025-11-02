# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom file
COPY pom.xml .

# Download dependencies (leverage Docker cache layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B


# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache ca-certificates

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage
COPY --from=builder /app/target/*.jar app.jar

# Create config directory
RUN mkdir -p /app/config && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Start application
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]