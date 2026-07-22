# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

LABEL maintainer="VerifID Team"

WORKDIR /build

# 1. Copy POM first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# 2. Copy source and build (skip tests — tests run in CI)
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime (minimal JRE image)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for healthcheck and tini for PID 1 signal handling
RUN apk add --no-cache curl tini

WORKDIR /app

# Create non-root user for security
RUN addgroup -S verifid && adduser -S verifid -G verifid

# Copy only the application JAR — no secrets, no source
COPY --from=builder /build/target/*.jar app.jar

# Ownership
RUN chown verifid:verifid app.jar

USER verifid

# JVM tuning: container-aware heap, Server mode
ENV JAVA_OPTS="-server -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xms256m"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "java $JAVA_OPTS -jar app.jar"]
