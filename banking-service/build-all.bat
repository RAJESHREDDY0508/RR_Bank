@echo off
echo ========================================
echo    RR-Bank - Full Clean Rebuild
echo ========================================
echo.

cd /d C:\Users\rajes\Desktop\projects\RR-Bank\banking-service

echo [1/2] Deleting target directory...
if exist target rmdir /s /q target
echo Done.
echo.

echo [2/2] Running Maven clean compile (including tests)...
call mvn clean compile test-compile -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo    BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo All source files compiled successfully.
    echo.
    echo To run the application:
    echo   mvn spring-boot:run
    echo.
    echo To run tests:
    echo   mvn test
) else (
    echo.
    echo ========================================
    echo    BUILD FAILED
    echo ========================================
    echo.
    echo Please check the errors above.
)

pause
