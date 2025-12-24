@echo off
echo ========================================
echo    RR Bank - System Verification
echo    Phase 0 Baseline Check
echo ========================================
echo.

set PASSED=0
set FAILED=0

echo [1/6] Checking Docker services...
docker ps --filter "name=rrbank" --format "table {{.Names}}\t{{.Status}}" 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ Docker containers found
    set /a PASSED+=1
) else (
    echo ✗ Docker containers not running
    set /a FAILED+=1
)
echo.

echo [2/6] Checking PostgreSQL connection...
docker exec rrbank-postgres pg_isready -U postgres >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ PostgreSQL is ready
    set /a PASSED+=1
) else (
    echo ✗ PostgreSQL is not ready
    set /a FAILED+=1
)
echo.

echo [3/6] Checking Redis connection...
docker exec rrbank-redis redis-cli ping >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ Redis is ready
    set /a PASSED+=1
) else (
    echo ✗ Redis is not ready
    set /a FAILED+=1
)
echo.

echo [4/6] Checking Kafka...
docker exec rrbank-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ Kafka is ready
    set /a PASSED+=1
) else (
    echo ✗ Kafka is not ready (may need more time)
    set /a FAILED+=1
)
echo.

echo [5/6] Checking Backend health endpoint...
curl -s -o nul -w "%%{http_code}" http://localhost:8080/actuator/health > temp_status.txt 2>nul
set /p HTTP_STATUS=<temp_status.txt
del temp_status.txt 2>nul
if "%HTTP_STATUS%"=="200" (
    echo ✓ Backend is healthy (HTTP 200)
    set /a PASSED+=1
) else (
    echo ✗ Backend not responding (HTTP %HTTP_STATUS%)
    set /a FAILED+=1
)
echo.

echo [6/6] Checking Auth endpoint...
curl -s -o nul -w "%%{http_code}" http://localhost:8080/api/auth/health > temp_status.txt 2>nul
set /p HTTP_STATUS=<temp_status.txt
del temp_status.txt 2>nul
if "%HTTP_STATUS%"=="200" (
    echo ✓ Auth service is healthy (HTTP 200)
    set /a PASSED+=1
) else (
    echo ✗ Auth service not responding (HTTP %HTTP_STATUS%)
    set /a FAILED+=1
)
echo.

echo ========================================
echo    Verification Results
echo ========================================
echo.
echo Passed: %PASSED%/6
echo Failed: %FAILED%/6
echo.

if %FAILED% EQU 0 (
    echo ✓ ALL CHECKS PASSED - System is operational!
    echo.
    echo You can now:
    echo   1. Import Postman collection from postman/ folder
    echo   2. Run API tests
    echo   3. Proceed to Phase 1 hardening
) else (
    echo ✗ SOME CHECKS FAILED - Please review and fix issues
    echo.
    echo Common fixes:
    echo   - Run 'docker-compose up -d' in banking-service folder
    echo   - Run 'mvnw spring-boot:run' in banking-service folder
    echo   - Wait a few seconds for services to initialize
)
echo.
pause
