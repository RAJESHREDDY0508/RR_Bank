@echo off
echo ====================================
echo   RR-BANK FRONTEND APPLICATIONS
echo   Phase 5: Starting Both Apps
echo ====================================
echo.

echo Checking if Node.js is installed...
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed!
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

echo Node.js version:
node --version
echo npm version:
npm --version
echo.

echo ====================================
echo   Step 1: Customer App Setup
echo ====================================
cd "%~dp0customer-app"

if not exist "node_modules\" (
    echo Installing Customer App dependencies...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to install Customer App dependencies
        pause
        exit /b 1
    )
) else (
    echo Customer App dependencies already installed
)

echo.
echo ====================================
echo   Step 2: Admin Console Setup
echo ====================================
cd "%~dp0admin-app"

if not exist "node_modules\" (
    echo Installing Admin Console dependencies...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to install Admin Console dependencies
        pause
        exit /b 1
    )
) else (
    echo Admin Console dependencies already installed
)

echo.
echo ====================================
echo   Step 3: Starting Applications
echo ====================================
echo.
echo Starting Customer App on port 3000...
echo Starting Admin Console on port 3001...
echo.
echo [INFO] Both apps will open in separate terminal windows
echo [INFO] Customer App: http://localhost:3000
echo [INFO] Admin Console: http://localhost:3001
echo.
echo Press Ctrl+C in each window to stop the apps
echo.

REM Start Customer App in new terminal
start "RR-Bank Customer App" cmd /k "cd /d %~dp0customer-app && npm run dev"

REM Wait a moment before starting the second app
timeout /t 3 /nobreak >nul

REM Start Admin Console in new terminal
start "RR-Bank Admin Console" cmd /k "cd /d %~dp0admin-app && npm run dev"

echo.
echo ====================================
echo   Applications Started!
echo ====================================
echo.
echo  Customer App: http://localhost:3000
echo  Admin Console: http://localhost:3001
echo.
echo  Check the separate terminal windows for logs
echo  Press any key to exit this window...
pause >nul
