# ============================================================
# RR-Bank Banking Service - Production Dockerfile
# Multi-stage build for minimal image size
# ============================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="RR-Bank Team"
LABEL version="1.0.0"
LABEL description="RR-Bank Banking Service"

# Create non-root user for security
RUN addgroup -S rrbank && adduser -S rrbank -G rrbank

WORKDIR /app

# Copy jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R rrbank:rrbank /app

# Switch to non-root user
USER rrbank

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
