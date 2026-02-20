# ==========================================
# SciInov DBMS - Multi-stage Production Build
# ==========================================

# Stage 1: Build (using Maven)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve -q
COPY src src
RUN mvn clean package -DskipTests -q

# ==========================================
# Stage 2: Runtime (using JRE only)
FROM eclipse-temurin:21-jre-alpine

# curl needed for health check
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 appuser && \
    adduser -u 1001 -G appuser -s /bin/sh -D appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appuser /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Health check — Cloud Run calls /actuator/health
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

USER appuser

# Cloud Run sets PORT env var — use it, fall back to 8080
# -Dspring.profiles.active=prod ensures correct config loading
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar --server.port=${PORT:-8080}"]
