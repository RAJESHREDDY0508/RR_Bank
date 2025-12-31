# ============================================================
# RR-Bank Phase 1 - Microservices Startup Script (PowerShell)
# ============================================================
# Run this script from the RR-Bank root directory
# ============================================================

Write-Host "=============================================="
Write-Host "  RR-Bank Phase 1 - Environment Setup"
Write-Host "=============================================="

# Set environment variables
$env:JWT_SECRET = "CTl/xK0434/aLcxx5A+Hn6lIEG5vxiqdcu1Wh8yT0I5yRzeZVtlsON6u83FSWe6ZuIdTHznvuSuEutP8GE30Fg=="
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:CONFIG_SERVER_ENABLED = "false"
$env:CONFIG_SERVER_URI = "http://localhost:8888"

Write-Host ""
Write-Host "Environment variables set:"
Write-Host "  JWT_SECRET: [CONFIGURED]"
Write-Host "  EUREKA_URL: $env:EUREKA_URL"
Write-Host "  CONFIG_SERVER_ENABLED: $env:CONFIG_SERVER_ENABLED"
Write-Host "  CONFIG_SERVER_URI: $env:CONFIG_SERVER_URI"
Write-Host ""

Write-Host "=============================================="
Write-Host "  Starting Services..."
Write-Host "=============================================="
Write-Host ""
Write-Host "Open 4 separate PowerShell terminals and run:"
Write-Host ""
Write-Host "Terminal 1 - Eureka (Discovery Server):"
Write-Host "  cd discovery-server"
Write-Host "  ..\mvnw spring-boot:run"
Write-Host ""
Write-Host "Terminal 2 - Config Server (wait for Eureka):"
Write-Host "  cd config-server"
Write-Host "  ..\mvnw spring-boot:run"
Write-Host ""
Write-Host "Terminal 3 - Banking Service (wait for Eureka):"
Write-Host "  cd banking-service"
Write-Host '  $env:SERVER_PORT = "8081"'
Write-Host "  ..\mvnw spring-boot:run"
Write-Host ""
Write-Host "Terminal 4 - API Gateway (wait for Eureka + Banking):"
Write-Host "  cd api-gateway"
Write-Host '  $env:GATEWAY_PORT = "8080"'
Write-Host "  ..\mvnw spring-boot:run"
Write-Host ""
Write-Host "=============================================="
Write-Host "  Verification Checklist"
Write-Host "=============================================="
Write-Host ""
Write-Host "1. Eureka UI: http://localhost:8761"
Write-Host "2. Config Server Health: http://localhost:8888/actuator/health"
Write-Host "3. Banking Service Health: http://localhost:8081/actuator/health"
Write-Host "4. API Gateway Health: http://localhost:8080/actuator/health"
Write-Host "5. Gateway -> Banking: http://localhost:8080/api/auth/health"
Write-Host ""
Write-Host "Eureka should show:"
Write-Host "  - BANKING-SERVICE"
Write-Host "  - API-GATEWAY"
Write-Host ""
