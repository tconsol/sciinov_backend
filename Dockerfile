# ==========================================
# SciInov DBMS - Multi-stage Production Build
# ==========================================

# Stage 1: Build (using Maven)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src src

# Cache dependencies layer
RUN mvn clean dependency:resolve -q

# Build application
RUN mvn clean package -DskipTests -q

# ==========================================
# Stage 2: Runtime (using JRE only)
FROM eclipse-temurin:21-jre-alpine

# Add user management tools (shadow package)
RUN apk add --no-cache shadow bash curl

# Create non-root user for security
RUN addgroup -g 1001 appuser && \
    adduser -u 1001 -G appuser -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder --chown=appuser:appuser /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Switch to non-root user
USER appuser

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
