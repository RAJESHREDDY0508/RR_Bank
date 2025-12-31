# ============================================================
# RR-Bank Complete Startup Script
# ============================================================
# This script starts all microservices in the correct order
# ============================================================

param(
    [switch]$SkipBuild,
    [switch]$DockerDeps
)

$projectRoot = $PSScriptRoot
if (-not $projectRoot) {
    $projectRoot = "C:\Users\rajes\Desktop\projects\RR-Bank"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "           RR-Bank Microservices Startup" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Set environment variables
$env:JWT_SECRET = "rr-bank-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security-2024"
$env:KAFKA_ENABLED = "false"
$env:SPRING_PROFILES_ACTIVE = "local"

# Start Docker dependencies if requested
if ($DockerDeps) {
    Write-Host "[1/6] Starting Docker dependencies (PostgreSQL, Redis)..." -ForegroundColor Yellow
    
    # Check if containers exist and remove them
    docker rm -f rrbank-postgres 2>$null
    docker rm -f rrbank-redis 2>$null
    
    # Start PostgreSQL
    docker run -d --name rrbank-postgres `
        -e POSTGRES_DB=rrbank `
        -e POSTGRES_USER=postgres `
        -e POSTGRES_PASSWORD=postgres `
        -p 5432:5432 `
        postgres:15-alpine
    
    # Start Redis
    docker run -d --name rrbank-redis `
        -p 6379:6379 `
        redis:7-alpine
    
    Write-Host "Waiting for databases to be ready..." -ForegroundColor Gray
    Start-Sleep -Seconds 10
    Write-Host "Docker dependencies started!" -ForegroundColor Green
} else {
    Write-Host "[1/6] Skipping Docker deps (use -DockerDeps to start PostgreSQL/Redis)" -ForegroundColor Gray
}

Write-Host ""

# Build if not skipped
if (-not $SkipBuild) {
    Write-Host "[2/6] Building all services (use -SkipBuild to skip)..." -ForegroundColor Yellow
    
    Push-Location "$projectRoot\discovery-server"
    ..\mvnw clean package -DskipTests -q
    Pop-Location
    
    Push-Location "$projectRoot\config-server"
    ..\mvnw clean package -DskipTests -q
    Pop-Location
    
    Push-Location "$projectRoot\banking-service"
    ..\mvnw clean package -DskipTests -q
    Pop-Location
    
    Push-Location "$projectRoot\api-gateway"
    ..\mvnw clean package -DskipTests -q
    Pop-Location
    
    Write-Host "Build complete!" -ForegroundColor Green
} else {
    Write-Host "[2/6] Skipping build..." -ForegroundColor Gray
}

Write-Host ""

# Start Discovery Server
Write-Host "[3/6] Starting Discovery Server (Eureka) on port 8761..." -ForegroundColor Yellow
$discoveryProcess = Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$projectRoot\discovery-server'; Write-Host 'DISCOVERY SERVER' -ForegroundColor Cyan; ..\mvnw spring-boot:run"
) -PassThru

Write-Host "Waiting for Eureka to start (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Start Config Server
Write-Host "[4/6] Starting Config Server on port 8888..." -ForegroundColor Yellow
$configProcess = Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$projectRoot\config-server'; Write-Host 'CONFIG SERVER' -ForegroundColor Cyan; ..\mvnw spring-boot:run"
) -PassThru

Write-Host "Waiting for Config Server to start (20 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

# Start Banking Service
Write-Host "[5/6] Starting Banking Service on port 8081..." -ForegroundColor Yellow
$bankingProcess = Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "`$env:JWT_SECRET='$env:JWT_SECRET'; `$env:KAFKA_ENABLED='false'; Set-Location '$projectRoot\banking-service'; Write-Host 'BANKING SERVICE' -ForegroundColor Cyan; ..\mvnw spring-boot:run"
) -PassThru

Write-Host "Waiting for Banking Service to start (40 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 40

# Start API Gateway
Write-Host "[6/6] Starting API Gateway on port 8080..." -ForegroundColor Yellow
$gatewayProcess = Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "`$env:JWT_SECRET='$env:JWT_SECRET'; Set-Location '$projectRoot\api-gateway'; Write-Host 'API GATEWAY' -ForegroundColor Cyan; ..\mvnw spring-boot:run"
) -PassThru

Write-Host "Waiting for API Gateway to start (20 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "           All Services Started Successfully!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  Eureka Dashboard:  http://localhost:8761" -ForegroundColor White
Write-Host "  Config Server:     http://localhost:8888" -ForegroundColor White
Write-Host "  Banking Service:   http://localhost:8081" -ForegroundColor White
Write-Host "  API Gateway:       http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "API Endpoints (via Gateway):" -ForegroundColor Cyan
Write-Host "  Register:  POST http://localhost:8080/api/auth/register" -ForegroundColor White
Write-Host "  Login:     POST http://localhost:8080/api/auth/login" -ForegroundColor White
Write-Host "  Accounts:  GET  http://localhost:8080/api/accounts/me" -ForegroundColor White
Write-Host ""
Write-Host "To stop all services, close the PowerShell windows or run:" -ForegroundColor Yellow
Write-Host "  Get-Process -Name java | Stop-Process -Force" -ForegroundColor Gray
Write-Host ""
