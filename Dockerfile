# Multi-stage build for optimized image
FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Production stage - minimal image
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="appupiratla@gmail.com"
LABEL version="1.0"
LABEL description="SciInov DBMS Production"

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 appuser && adduser -u 1001 -G appuser -s /bin/sh appuser

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start application with optimized JVM options for production
ENTRYPOINT ["java", \
    "-server", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+ParallelRefProcEnabled", \
    "-Dspring.jmx.enabled=false", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=UTC", \
    "-jar", \
    "app.jar"]
