@echo off
echo ========================================
echo    RR-Bank - Clean and Rebuild
echo ========================================
echo.
echo This will delete all compiled files and rebuild from scratch.
echo.

cd /d C:\Users\rajes\Desktop\projects\RR-Bank\banking-service

echo [1/3] Cleaning target directory...
rmdir /s /q target 2>nul
echo Done.
echo.

echo [2/3] Cleaning Maven cache for this project...
call mvn dependency:purge-local-repository -DmanualInclude=com.rrbank:banking-service -DreResolve=false 2>nul
echo Done.
echo.

echo [3/3] Running clean compile...
call mvn clean compile -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo    BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo You can now run: mvn spring-boot:run
) else (
    echo.
    echo ========================================
    echo    BUILD FAILED - Check errors above
    echo ========================================
)

pause
