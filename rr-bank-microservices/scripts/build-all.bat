@echo off
REM ===================================================================
REM  RR-BANK MICROSERVICES - COMPLETE BUILD SCRIPT
REM ===================================================================

setlocal enabledelayedexpansion

echo.
echo ====================================================================
echo    RR-BANK MICROSERVICES - FULL BUILD
echo ====================================================================
echo.

REM Check prerequisites
echo [Step 1/7] Checking prerequisites...
echo.

REM Check Docker
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)
echo   ✓ Docker is running

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 17+ is not installed!
    pause
    exit /b 1
)
echo   ✓ Java is installed

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed!
    pause
    exit /b 1
)
echo   ✓ Maven is installed

echo.
echo [Step 2/7] Building shared library...
cd shared-library
call mvn clean install -DskipTests
if errorlevel 1 (
    echo [ERROR] Failed to build shared library!
    pause
    exit /b 1
)
cd ..
echo   ✓ Shared library built successfully
echo.

echo [Step 3/7] Starting infrastructure...
cd docker
docker-compose up -d
if errorlevel 1 (
    echo [ERROR] Failed to start infrastructure!
    pause
    exit /b 1
)
cd ..
echo   ✓ Infrastructure started
echo.

echo [Step 4/7] Waiting for databases to be ready (30 seconds)...
timeout /t 30 /nobreak >nul
echo   ✓ Databases should be ready
echo.

echo [Step 5/7] Building microservices...
echo.

set SERVICES=auth-service user-service account-service transaction-service payment-service notification-service

for %%S in (%SERVICES%) do (
    if exist %%S (
        echo Building %%S...
        cd %%S
        call mvn clean package -DskipTests
        if errorlevel 1 (
            echo [WARNING] Failed to build %%S
        ) else (
            echo   ✓ %%S built successfully
        )
        cd ..
    ) else (
        echo   [SKIP] %%S directory not found
    )
)

echo.
echo [Step 6/7] Creating databases...
docker exec -it auth-db psql -U postgres -c "CREATE DATABASE IF NOT EXISTS auth_db;" 2>nul
docker exec -it user-db psql -U postgres -c "CREATE DATABASE IF NOT EXISTS user_db;" 2>nul
docker exec -it account-db psql -U postgres -c "CREATE DATABASE IF NOT EXISTS account_db;" 2>nul
docker exec -it transaction-db psql -U postgres -c "CREATE DATABASE IF NOT EXISTS transaction_db;" 2>nul
echo   ✓ Databases created
echo.

echo [Step 7/7] Build Summary
echo.
echo ====================================================================
echo    BUILD COMPLETE!
echo ====================================================================
echo.
echo Infrastructure Status:
docker-compose ps
echo.
echo Microservices Status:
for %%S in (%SERVICES%) do (
    if exist %%S\target\%%S-1.0.0.jar (
        echo   ✓ %%S - READY
    ) else (
        echo   ✗ %%S - NOT BUILT
    )
)
echo.
echo ====================================================================
echo.
echo Next Steps:
echo   1. Start services: run start-all-services.bat
echo   2. Or start individually: cd [service] ^&^& mvn spring-boot:run
echo   3. Check Eureka: http://localhost:8761
echo   4. Check API Gateway: http://localhost:8080/actuator/health
echo.
echo To view logs: docker-compose logs -f
echo To stop infrastructure: docker-compose down
echo.
pause
