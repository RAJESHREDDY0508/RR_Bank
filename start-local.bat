@echo off
echo ========================================
echo    RR Bank - Phase 0 Startup Script
echo    Production Hardening Baseline
echo ========================================
echo.

:: Set the project root
set PROJECT_ROOT=%~dp0
cd /d %PROJECT_ROOT%

echo [Step 1/5] Checking Docker Desktop...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker Desktop is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)
echo Docker is running.
echo.

echo [Step 2/5] Starting infrastructure services (PostgreSQL, Redis, Kafka)...
cd banking-service
docker-compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to start Docker services
    pause
    exit /b 1
)
echo.

echo Waiting for services to be healthy...
echo   - PostgreSQL on port 5432
echo   - Redis on port 6379  
echo   - Kafka on port 9092
timeout /t 15 /nobreak > nul

:: Check if PostgreSQL is ready
echo Checking PostgreSQL...
docker exec rrbank-postgres pg_isready -U postgres >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: PostgreSQL may not be ready yet. Waiting additional 10 seconds...
    timeout /t 10 /nobreak > nul
)
echo PostgreSQL: Ready
echo Redis: Ready
echo Kafka: Ready
echo.

echo [Step 3/5] Checking environment variables...
if not defined DB_PASSWORD (
    echo WARNING: DB_PASSWORD not set. Using default for local development.
    set DB_PASSWORD=postgres
)
if not defined JWT_SECRET (
    echo WARNING: JWT_SECRET not set. Using default for local development.
    set JWT_SECRET=your-super-secure-jwt-secret-key-that-should-be-at-least-256-bits-long
)
echo Environment configured.
echo.

echo [Step 4/5] Starting Backend (banking-service)...
start "RR Bank Backend" cmd /k "cd /d %PROJECT_ROOT%banking-service && set DB_PASSWORD=postgres && set JWT_SECRET=your-super-secure-jwt-secret-key-that-should-be-at-least-256-bits-long && mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo Backend starting on http://localhost:8080
echo.

echo Waiting for backend to initialize (30 seconds)...
timeout /t 30 /nobreak > nul

echo [Step 5/5] Starting Frontend...
cd /d %PROJECT_ROOT%
if exist frontend (
    cd frontend
    if not exist "node_modules" (
        echo Installing frontend dependencies...
        call npm install
    )
    start "RR Bank Frontend" cmd /k "npm run dev"
    echo Frontend starting on http://localhost:5173
) else (
    echo NOTE: Frontend directory not found. Skipping frontend startup.
)
echo.

timeout /t 5 /nobreak > nul

echo ========================================
echo    RR Bank Services Started!
echo ========================================
echo.
echo Infrastructure:
echo   PostgreSQL: localhost:5432 (rrbank / postgres)
echo   Redis:      localhost:6379
echo   Kafka:      localhost:9092
echo.
echo Application:
echo   Backend:    http://localhost:8080
echo   Frontend:   http://localhost:5173
echo   Actuator:   http://localhost:8080/actuator/health
echo.
echo API Documentation:
echo   Postman Collection: postman/RR-Bank-API-Collection.postman_collection.json
echo.
echo Press any key to open the application...
pause > nul
start http://localhost:5173
