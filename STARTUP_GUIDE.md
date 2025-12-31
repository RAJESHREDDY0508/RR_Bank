# RR-Bank Complete Startup Guide

## Prerequisites

### Required Software
1. **Java 21** - `java -version` should show 21+
2. **PostgreSQL 15+** - Running on port 5432
3. **Redis 7+** - Running on port 6379
4. **Maven** - Or use included `mvnw` wrapper

### Optional
- Docker Desktop (for containerized startup)
- Kafka (if KAFKA_ENABLED=true)

---

## Option 1: Local Development (Recommended for Development)

### Step 1: Start PostgreSQL and Redis

**Windows (if using Docker):**
```powershell
docker run -d --name rrbank-postgres -e POSTGRES_DB=rrbank -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15-alpine

docker run -d --name rrbank-redis -p 6379:6379 redis:7-alpine
```

**Or if PostgreSQL/Redis are installed locally, just ensure they're running.**

### Step 2: Create Database and Run Migrations

Connect to PostgreSQL and run:
```sql
-- Create database if not exists
CREATE DATABASE rrbank;

-- Connect to rrbank database, then run migrations
-- (See PHASE3_TO_6_IMPLEMENTATION.md for full SQL)
```

### Step 3: Set Environment Variables

**PowerShell:**
```powershell
$env:JWT_SECRET = "your-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "rrbank"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:KAFKA_ENABLED = "false"
```

### Step 4: Start Services in Order

Open 4 separate PowerShell terminals:

**Terminal 1 - Discovery Server (Eureka):**
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank\discovery-server
..\mvnw spring-boot:run
```
Wait until you see: `Started DiscoveryServerApplication`
Access: http://localhost:8761

**Terminal 2 - Config Server:**
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank\config-server
..\mvnw spring-boot:run
```
Wait until you see: `Started ConfigServerApplication`
Access: http://localhost:8888

**Terminal 3 - Banking Service:**
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank\banking-service
$env:JWT_SECRET = "your-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security"
..\mvnw spring-boot:run
```
Wait until you see: `Started BankingServiceApplication`
Access: http://localhost:8081

**Terminal 4 - API Gateway:**
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank\api-gateway
$env:JWT_SECRET = "your-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security"
..\mvnw spring-boot:run
```
Wait until you see: `Started ApiGatewayApplication`
Access: http://localhost:8080

---

## Option 2: Using Startup Script

### Create and run this script:

Save as `start-all.ps1` in project root:
```powershell
# RR-Bank Complete Startup Script
$projectRoot = "C:\Users\rajes\Desktop\projects\RR-Bank"

# Set environment variables
$env:JWT_SECRET = "your-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security"
$env:KAFKA_ENABLED = "false"

Write-Host "Starting RR-Bank Microservices..." -ForegroundColor Green

# Start Discovery Server
Write-Host "Starting Discovery Server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\discovery-server'; ..\mvnw spring-boot:run"

Start-Sleep -Seconds 30

# Start Config Server
Write-Host "Starting Config Server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\config-server'; ..\mvnw spring-boot:run"

Start-Sleep -Seconds 20

# Start Banking Service
Write-Host "Starting Banking Service..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\banking-service'; `$env:JWT_SECRET='$env:JWT_SECRET'; ..\mvnw spring-boot:run"

Start-Sleep -Seconds 30

# Start API Gateway
Write-Host "Starting API Gateway..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\api-gateway'; `$env:JWT_SECRET='$env:JWT_SECRET'; ..\mvnw spring-boot:run"

Write-Host ""
Write-Host "All services starting!" -ForegroundColor Green
Write-Host "Eureka Dashboard: http://localhost:8761" -ForegroundColor Cyan
Write-Host "API Gateway: http://localhost:8080" -ForegroundColor Cyan
Write-Host "Banking Service: http://localhost:8081" -ForegroundColor Cyan
```

Run with:
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank
.\start-all.ps1
```

---

## Option 3: Docker Compose (Production-like)

### Step 1: Build all services
```powershell
cd C:\Users\rajes\Desktop\projects\RR-Bank

# Build each service
cd discovery-server
..\mvnw clean package -DskipTests
cd ..\config-server
..\mvnw clean package -DskipTests
cd ..\banking-service
..\mvnw clean package -DskipTests
cd ..\api-gateway
..\mvnw clean package -DskipTests
cd ..
```

### Step 2: Create .env file
```powershell
# Create .env file in project root
@"
JWT_SECRET=your-super-secret-jwt-key-that-is-at-least-512-bits-long-for-hs512-algorithm-security
DB_USERNAME=rrbank
DB_PASSWORD=rrbank_secret_password
KAFKA_ENABLED=false
"@ | Out-File -FilePath ".env" -Encoding utf8
```

### Step 3: Start with Docker Compose
```powershell
docker-compose up -d
```

### Step 4: Check status
```powershell
docker-compose ps
docker-compose logs -f banking-service
```

### Step 5: Stop all
```powershell
docker-compose down
```

---

## Verify System is Running

### Check all services are registered in Eureka:
Open: http://localhost:8761

You should see:
- BANKING-SERVICE
- API-GATEWAY
- CONFIG-SERVER

### Test API Gateway:
```powershell
# Health check
curl http://localhost:8080/actuator/health

# Or in PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

### Test Banking Service directly:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health"
```

---

## Quick API Test

### 1. Register a User
```powershell
$body = @{
    username = "testuser"
    email = "test@example.com"
    password = "Password123!"
    firstName = "Test"
    lastName = "User"
    phoneNumber = "+1234567890"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $body -ContentType "application/json"
```

### 2. Login
```powershell
$loginBody = @{
    username = "testuser"
    password = "Password123!"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
$token = $response.accessToken
Write-Host "Token: $token"
```

### 3. Get Accounts (with token)
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}
Invoke-RestMethod -Uri "http://localhost:8080/api/accounts/me" -Headers $headers
```

---

## Service Ports Summary

| Service | Port | URL |
|---------|------|-----|
| Eureka Discovery | 8761 | http://localhost:8761 |
| Config Server | 8888 | http://localhost:8888 |
| Banking Service | 8081 | http://localhost:8081 |
| API Gateway | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

---

## Troubleshooting

### Service won't start
1. Check if port is already in use: `netstat -ano | findstr :8081`
2. Kill process: `taskkill /PID <pid> /F`

### Database connection error
1. Verify PostgreSQL is running: `pg_isready -h localhost -p 5432`
2. Check credentials in application.yml

### Redis connection error
1. Verify Redis is running: `redis-cli ping`

### Eureka registration issues
1. Wait 30 seconds for heartbeat
2. Check Eureka dashboard for registered services

### JWT errors
1. Ensure JWT_SECRET is set in both banking-service and api-gateway
2. Must be same value in both services
3. Must be at least 64 characters for HS512

---

## Stop All Services

### If started manually:
Close each PowerShell terminal or press Ctrl+C

### If started with Docker:
```powershell
docker-compose down
```

### Kill by port:
```powershell
# Find and kill process on port
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```
