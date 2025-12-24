@echo off
REM ===================================================================
REM  START ALL MICROSERVICES
REM ===================================================================

echo.
echo ====================================================================
echo    STARTING ALL RR-BANK MICROSERVICES
echo ====================================================================
echo.

REM Start services in order with delays

echo [1/6] Starting Auth Service (Port 8081)...
start "Auth Service" cmd /k "cd auth-service && mvn spring-boot:run"
timeout /t 20 /nobreak >nul

echo [2/6] Starting User Service (Port 8082)...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [3/6] Starting Account Service (Port 8083)...
start "Account Service" cmd /k "cd account-service && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [4/6] Starting Transaction Service (Port 8084)...
start "Transaction Service" cmd /k "cd transaction-service && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [5/6] Starting Payment Service (Port 8085)...
start "Payment Service" cmd /k "cd payment-service && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [6/6] Starting Notification Service (Port 8087)...
start "Notification Service" cmd /k "cd notification-service && mvn spring-boot:run"

echo.
echo ====================================================================
echo    ALL SERVICES STARTING
echo ====================================================================
echo.
echo Services are starting in separate windows.
echo Please wait 2-3 minutes for all services to register with Eureka.
echo.
echo Check service health:
echo   - Eureka Dashboard: http://localhost:8761
echo   - Auth Service: http://localhost:8081/actuator/health
echo   - User Service: http://localhost:8082/actuator/health
echo   - Account Service: http://localhost:8083/actuator/health
echo   - Transaction Service: http://localhost:8084/actuator/health
echo   - Payment Service: http://localhost:8085/actuator/health
echo   - Notification Service: http://localhost:8087/actuator/health
echo.
echo To stop a service, close its window or press Ctrl+C
echo.
pause
