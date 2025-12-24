#!/bin/bash

# RR-Bank Application Startup Script (Development)
# This script runs the application with the development profile

echo "========================================="
echo "🏦 RR-Bank Application (Development)"
echo "========================================="
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  Warning: .env file not found"
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo ""
    echo "✅ Created .env file"
    echo "📝 Please edit .env and set your values"
    echo ""
    read -p "Press Enter to continue with default values or Ctrl+C to exit..."
fi

# Load environment variables from .env
if [ -f .env ]; then
    echo "📥 Loading environment variables from .env..."
    export $(cat .env | grep -v '^#' | xargs)
fi

# Set development profile
export SPRING_PROFILES_ACTIVE=dev

echo ""
echo "Starting application with:"
echo "  Profile: dev"
echo "  Database: H2 (in-memory)"
echo "  H2 Console: http://localhost:8080/h2-console"
echo "  Actuator: http://localhost:8080/actuator"
echo ""
echo "========================================="
echo ""

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# If Maven wrapper doesn't exist, use system Maven
if [ $? -ne 0 ]; then
    echo "Maven wrapper not found, trying system Maven..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
fi
