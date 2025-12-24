@echo off
echo ========================================
echo    RR Bank - Quick Start Script
echo ========================================
echo.

echo [1/4] Starting Docker services...
cd Banking-Application
docker-compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to start Docker services
    echo Please make sure Docker Desktop is running
    pause
    exit /b 1
)
echo Docker services started successfully!
echo.

echo Waiting for services to be ready...
timeout /t 10 /nobreak > nul
echo.

echo [2/4] Starting Backend...
start "RR Bank Backend" cmd /k "cd Banking-Application && mvnw spring-boot:run"
echo Backend is starting on http://localhost:8080
echo.

echo Waiting for backend to initialize...
timeout /t 15 /nobreak > nul
echo.

echo [3/4] Installing Frontend dependencies...
cd ..\Frontend
if not exist "node_modules" (
    echo Installing npm packages...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to install npm packages
        pause
        exit /b 1
    )
) else (
    echo Dependencies already installed!
)
echo.

echo [4/4] Starting Frontend...
start "RR Bank Frontend" cmd /k "npm run dev"
echo Frontend is starting on http://localhost:5173
echo.

timeout /t 5 /nobreak > nul

echo ========================================
echo    RR Bank is starting up!
echo ========================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Two new terminal windows have opened:
echo   1. Backend (Spring Boot)
echo   2. Frontend (Vite + React)
echo.
echo Opening browser...
timeout /t 3 /nobreak > nul
start http://localhost:5173
echo.
echo Press any key to close this window...
pause > nul
