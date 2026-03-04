# ==========================================
# SciInov DBMS - Memory-Optimized Production Build
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

# Cloud Run Memory Optimization for 512MB limit
# -Xmx400m: Max heap set to 400MB (leaving 112MB for non-heap)
# -Xms200m: Initial heap 200MB (reduces startup memory spike)
# -XX:+UseG1GC: G1GC is optimal for 512MB+ heaps
# -XX:MaxGCPauseMillis=200: Target low pause times
# -XX:InitiatingHeapOccupancyPercent=35: Trigger GC earlier to avoid full collections
# -XX:+DisableExplicitGC: Prevent expensive explicit GC calls
# -XX:+ExitOnOutOfMemoryError: Fail fast instead of hanging
# -XX:CICompilerCount=2: Reduce JIT compilation threads
ENV JAVA_OPTS="-Xmx400m -Xms200m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC -XX:CICompilerCount=2 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar --server.port=${PORT:-8080}"]
