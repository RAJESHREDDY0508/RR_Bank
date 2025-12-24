@echo off
REM ========================================
REM RR-BANK MICROSERVICES - COMPLETE SETUP
REM ========================================

echo.
echo ========================================
echo   RR-BANK MICROSERVICES SETUP
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/6] Docker is running... OK
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed!
    echo Please install Java 17 or higher.
    pause
    exit /b 1
)

echo [2/6] Java is installed... OK
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed!
    echo Please install Maven 3.8 or higher.
    pause
    exit /b 1
)

echo [3/6] Maven is installed... OK
echo.

REM Build shared library first
echo [4/6] Building shared library...
cd shared-library
call mvn clean install -DskipTests
if errorlevel 1 (
    echo [ERROR] Failed to build shared library!
    pause
    exit /b 1
)
cd ..
echo Shared library built successfully!
echo.

REM Start infrastructure services
echo [5/6] Starting infrastructure (PostgreSQL, Kafka, Redis)...
cd docker
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start infrastructure!
    pause
    exit /b 1
)
cd ..
echo Infrastructure started successfully!
echo.

REM Wait for services to be ready
echo [6/6] Waiting for services to be ready (30 seconds)...
timeout /t 30 /nobreak

echo.
echo ========================================
echo   SETUP COMPLETE!
echo ========================================
echo.
echo Infrastructure is running:
echo   - PostgreSQL (Auth):        localhost:5433
echo   - PostgreSQL (User):        localhost:5434
echo   - PostgreSQL (Account):     localhost:5435
echo   - PostgreSQL (Transaction): localhost:5436
echo   - Kafka:                    localhost:9092
echo   - Redis:                    localhost:6379
echo.
echo Next steps:
echo   1. Build microservices: run build-all-services.bat
echo   2. Start services individually or use Docker
echo   3. Access API Gateway: http://localhost:8080
echo.
echo To view infrastructure logs: docker-compose logs -f
echo To stop infrastructure: docker-compose down
echo.
pause
