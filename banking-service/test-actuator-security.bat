@echo off
REM Actuator Security Verification Script for Windows
REM Tests that actuator endpoints are properly secured

setlocal enabledelayedexpansion
set BASE_URL=http://localhost:8080

echo =========================================
echo 🔒 Actuator Security Test Suite
echo =========================================
echo.

REM Test 1: Public Health Check (Should Pass)
echo Test 1: Public health check...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/health > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="200" (
    echo ✅ PASS - Public health check accessible
) else (
    echo ❌ FAIL - Expected 200, got %RESPONSE%
)
echo.

REM Test 2: Public Info (Should Pass)
echo Test 2: Public info endpoint...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/info > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="200" (
    echo ✅ PASS - Public info accessible
) else (
    echo ❌ FAIL - Expected 200, got %RESPONSE%
)
echo.

REM Test 3: Environment Variables (Should Be Blocked)
echo Test 3: Environment variables (should be blocked)...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/env > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="403" (
    echo ✅ PASS - Environment variables blocked (got %RESPONSE%)
) else if "%RESPONSE%"=="404" (
    echo ✅ PASS - Environment variables blocked (got %RESPONSE%)
) else (
    echo ❌ FAIL - SECURITY RISK! Environment exposed (got %RESPONSE%)
)
echo.

REM Test 4: Beans Configuration (Should Be Blocked)
echo Test 4: Beans configuration (should be blocked)...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/beans > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="403" (
    echo ✅ PASS - Beans configuration blocked (got %RESPONSE%)
) else if "%RESPONSE%"=="404" (
    echo ✅ PASS - Beans configuration blocked (got %RESPONSE%)
) else (
    echo ❌ FAIL - SECURITY RISK! Beans exposed (got %RESPONSE%)
)
echo.

REM Test 5: Config Properties (Should Be Blocked)
echo Test 5: Config properties (should be blocked)...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/configprops > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="403" (
    echo ✅ PASS - Config properties blocked (got %RESPONSE%)
) else if "%RESPONSE%"=="404" (
    echo ✅ PASS - Config properties blocked (got %RESPONSE%)
) else (
    echo ❌ FAIL - SECURITY RISK! Config properties exposed (got %RESPONSE%)
)
echo.

REM Test 6: Mappings (Should Be Blocked)
echo Test 6: Route mappings (should be blocked)...
curl -s -o nul -w "%%{http_code}" %BASE_URL%/actuator/mappings > temp.txt
set /p RESPONSE=<temp.txt
if "%RESPONSE%"=="403" (
    echo ✅ PASS - Route mappings blocked (got %RESPONSE%)
) else if "%RESPONSE%"=="404" (
    echo ✅ PASS - Route mappings blocked (got %RESPONSE%)
) else (
    echo ❌ FAIL - SECURITY RISK! Route mappings exposed (got %RESPONSE%)
)
echo.

REM Test 7: Detailed Health Without Auth (Should Be Blocked)
echo Test 7: Detailed health without auth (should show minimal info)...
curl -s %BASE_URL%/actuator/health > health.txt
findstr /C:"components" health.txt >nul
if errorlevel 1 (
    echo ✅ PASS - Only basic health status shown
) else (
    echo ❌ FAIL - SECURITY RISK! Detailed health info exposed
    type health.txt
)
echo.

REM Test 8: Admin Access
echo Test 8: Admin access to detailed health...
echo ℹ️  Skipping - requires admin credentials
echo    To test manually:
echo    1. Login: curl -X POST %BASE_URL%/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
echo    2. Access: curl -H "Authorization: Bearer <token>" %BASE_URL%/actuator/health
echo.

REM Cleanup
if exist temp.txt del temp.txt
if exist health.txt del health.txt

echo =========================================
echo 📊 Test Summary
echo =========================================
echo.
echo If all tests passed (✅), your actuator endpoints are secure!
echo If any test failed (❌), there's a security vulnerability!
echo.
echo Next steps:
echo 1. Restart your application if you just made changes
echo 2. Check application.properties for actuator settings
echo 3. Check SecurityConfig.java for endpoint security
echo.
pause
