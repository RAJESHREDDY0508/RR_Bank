@echo off
echo ============================================
echo RR-Bank API Test Script
echo ============================================
echo.

set BASE_URL=http://localhost:8080/api

echo Testing Health Endpoint...
curl -s %BASE_URL%/auth/health
echo.
echo.

echo Testing Login (admin)...
curl -s -X POST %BASE_URL%/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"usernameOrEmail\":\"admin\",\"password\":\"Admin@123\"}"
echo.
echo.

echo ============================================
echo If you see accessToken in the response, the API is working!
echo ============================================
pause
