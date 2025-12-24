@echo off
REM RR-Bank Application Startup Script (Development)
REM This script runs the application with the development profile

echo =========================================
echo 🏦 RR-Bank Application (Development)
echo =========================================
echo.

REM Check if .env file exists
if not exist .env (
    echo ⚠️  Warning: .env file not found
    echo Creating .env from .env.example...
    copy .env.example .env
    echo.
    echo ✅ Created .env file
    echo 📝 Please edit .env and set your values
    echo.
    pause
)

REM Set development profile
set SPRING_PROFILES_ACTIVE=dev

echo.
echo Starting application with:
echo   Profile: dev
echo   Database: H2 (in-memory)
echo   H2 Console: http://localhost:8080/h2-console
echo   Actuator: http://localhost:8080/actuator
echo.
echo =========================================
echo.

REM Run the application
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

REM If Maven wrapper doesn't exist, try system Maven
if errorlevel 1 (
    echo Maven wrapper not found, trying system Maven...
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
)

pause
