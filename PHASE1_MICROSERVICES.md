# RR-Bank Phase 1 - Microservices Architecture

## Overview

Phase 1 implements a complete microservices setup with:
- **Eureka Discovery Server** (port 8761) - Service registry
- **Config Server** (port 8888) - Centralized configuration
- **Banking Service** (port 8081) - Core banking APIs
- **API Gateway** (port 8080) - Single entry point, routing

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (Frontend)                       │
│                    http://localhost:3000                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     API GATEWAY                              │
│                   http://localhost:8080                      │
│            Routes /api/** → Banking Service                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  BANKING    │   │   EUREKA    │   │   CONFIG    │
│  SERVICE    │   │   SERVER    │   │   SERVER    │
│  :8081      │   │   :8761     │   │   :8888     │
└─────────────┘   └─────────────┘   └─────────────┘
```

## Quick Start

### Prerequisites
- Java 21
- Maven
- PostgreSQL running on port 5432
- Redis running on port 6379 (optional)

### Step 1: Set Environment Variables (PowerShell)

```powershell
$env:JWT_SECRET = "CTl/xK0434/aLcxx5A+Hn6lIEG5vxiqdcu1Wh8yT0I5yRzeZVtlsON6u83FSWe6ZuIdTHznvuSuEutP8GE30Fg=="
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:CONFIG_SERVER_ENABLED = "false"
$env:CONFIG_SERVER_URI = "http://localhost:8888"
```

### Step 2: Start Services (4 terminals)

**Terminal 1 - Eureka Discovery Server:**
```powershell
cd discovery-server
..\mvnw spring-boot:run
```

**Terminal 2 - Config Server:**
```powershell
cd config-server
..\mvnw spring-boot:run
```

**Terminal 3 - Banking Service:**
```powershell
cd banking-service
$env:SERVER_PORT = "8081"
..\mvnw spring-boot:run
```

**Terminal 4 - API Gateway:**
```powershell
cd api-gateway
$env:GATEWAY_PORT = "8080"
..\mvnw spring-boot:run
```

## Verification Checklist

| Check | URL | Expected |
|-------|-----|----------|
| Eureka UI | http://localhost:8761 | Dashboard with registered services |
| Config Server Health | http://localhost:8888/actuator/health | `{"status":"UP"}` |
| Banking Service Health | http://localhost:8081/actuator/health | `{"status":"UP"}` |
| API Gateway Health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Gateway → Banking | http://localhost:8080/api/auth/health | Routes to banking service |

### Eureka Should Show:
- `BANKING-SERVICE`
- `API-GATEWAY`

## Service Ports

| Service | Default Port | Env Variable |
|---------|--------------|--------------|
| Eureka Discovery | 8761 | - |
| Config Server | 8888 | - |
| Banking Service | 8081 | `SERVER_PORT` |
| API Gateway | 8080 | `GATEWAY_PORT` |

## Key Changes Made

### 1. API Gateway
- ✅ Added `@EnableDiscoveryClient`
- ✅ Added Eureka Client, Config Client, Bootstrap dependencies
- ✅ Fixed routing (removed StripPrefix)
- ✅ `bootstrap.yml`: `fail-fast: false`

### 2. Banking Service  
- ✅ Added `@EnableDiscoveryClient`
- ✅ Added Eureka Client, Config Client, Bootstrap dependencies
- ✅ Default port changed to 8081
- ✅ CORS includes gateway origin (localhost:8080)
- ✅ `bootstrap.yml`: `fail-fast: false`

### 3. Config Server
- ✅ Switched to native profile (classpath config)
- ✅ No git dependency (works on any machine)

### 4. All Services
- ✅ Aligned Spring Boot version (3.3.4)
- ✅ Aligned Spring Cloud version (2023.0.3)

## Troubleshooting

### Services not registering with Eureka?
- Check `EUREKA_URL` env variable
- Ensure Eureka is running first
- Check logs for connection errors

### Gateway returning 404?
- Verify banking-service is registered in Eureka
- Check gateway routes in `application.yml`
- Ensure banking-service is running on port 8081

### Config Server not loading configs?
- Configs are now in `config-server/src/main/resources/config-repo/`
- Check for YAML syntax errors

## Files Modified

```
RR-Bank/
├── api-gateway/
│   ├── pom.xml                    # Added Eureka, Config, Bootstrap deps
│   ├── src/main/java/.../ApiGatewayApplication.java  # @EnableDiscoveryClient
│   └── src/main/resources/
│       ├── application.yml        # Fixed routing, added Eureka config
│       └── bootstrap.yml          # fail-fast: false
├── banking-service/
│   ├── pom.xml                    # Added Eureka, Config, Bootstrap deps
│   ├── src/main/java/.../BankingApplication.java     # @EnableDiscoveryClient
│   └── src/main/resources/
│       ├── application.yml        # Port 8081, Eureka config
│       └── bootstrap.yml          # fail-fast: false
├── config-server/
│   ├── pom.xml                    # Aligned versions
│   └── src/main/resources/
│       ├── application.yml        # Native profile
│       └── config-repo/
│           ├── api-gateway.yml    # Gateway config
│           └── banking-service.yml # Banking config
├── discovery-server/
│   └── pom.xml                    # Aligned versions
└── start-phase1.ps1               # Startup script
```
