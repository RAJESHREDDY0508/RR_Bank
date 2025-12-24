# Phase 0 — Freeze & Baseline

## Overview
This document captures the baseline state of the RR-Bank application before production hardening.

**Date Created:** December 22, 2024  
**Branch:** `hardening/v1-production`  
**Status:** ✅ Baseline Captured

---

## 1. Project Structure

```
RR-Bank/
├── banking-service/          # Main Spring Boot application
│   ├── src/main/java/        # Application source code
│   ├── src/main/resources/   # Configuration files
│   ├── src/test/             # Test files
│   ├── docker-compose.yml    # Infrastructure services
│   └── pom.xml               # Maven dependencies
├── api-gateway/              # API Gateway (future microservice)
├── config-server/            # Config Server (future microservice)
├── discovery-server/         # Eureka Discovery (future microservice)
├── frontend/                 # React frontend
├── postman/                  # API Collections & Environments
│   ├── RR-Bank-API-Collection.postman_collection.json
│   ├── RR-Bank-Local.postman_environment.json
│   └── RR-Bank-Dev.postman_environment.json
├── start-local.bat           # Local startup script
└── PHASE0_BASELINE.md        # This document
```

---

## 2. Technology Stack

### Backend
- **Framework:** Spring Boot 3.3.4
- **Java Version:** 21
- **Database:** PostgreSQL 15
- **Cache:** Redis 7
- **Message Queue:** Apache Kafka (Confluent 7.5.0)
- **Build Tool:** Maven

### Security
- **Authentication:** JWT (Access + Refresh tokens)
- **Password Hashing:** BCrypt
- **Authorization:** Role-Based Access Control (RBAC)
- **MFA Support:** TOTP, SMS OTP, Email OTP

### Frontend
- **Framework:** React + Vite
- **Styling:** TailwindCSS

---

## 3. Infrastructure Services

### Docker Compose Services
| Service | Container Name | Port | Purpose |
|---------|---------------|------|---------|
| PostgreSQL | rrbank-postgres | 5432 | Primary database |
| Redis | rrbank-redis | 6379 | Caching layer |
| Zookeeper | rrbank-zookeeper | 2181 | Kafka coordination |
| Kafka | rrbank-kafka | 9092 | Event messaging |

### Start Command
```bash
cd banking-service
docker-compose up -d
```

---

## 4. API Endpoints Summary

### Authentication (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | User login |
| POST | `/refresh` | Refresh access token |
| GET | `/health` | Health check |

### Customers (`/api/customers`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create customer |
| GET | `/{id}` | Get by ID |
| GET | `/user/{userId}` | Get by user ID |
| PUT | `/{id}` | Update customer |
| POST | `/kyc` | Submit KYC |
| PUT | `/{id}/kyc/verify` | Verify KYC (Admin) |
| GET | `/` | List all (Admin) |
| GET | `/search` | Search customers |
| GET | `/kyc/pending` | Pending KYC list |

### Accounts (`/api/accounts`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create account |
| GET | `/{id}` | Get by ID |
| GET | `/number/{accountNumber}` | Get by number |
| GET | `/customer/{customerId}` | Customer accounts |
| GET | `/{id}/balance` | Get balance |
| PUT | `/{id}/status` | Update status (Admin) |
| GET | `/` | List all (Admin) |
| GET | `/status/{status}` | By status |
| DELETE | `/{id}` | Delete (Admin) |

### Transactions (`/api/transactions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/transfer` | Transfer money |
| GET | `/{id}` | Get by ID |
| GET | `/reference/{ref}` | Get by reference |
| GET | `/account/{accountId}` | Account transactions |
| GET | `/account/{accountId}/recent` | Recent transactions |
| GET | `/search` | Search with filters |
| GET | `/account/{accountId}/stats` | Statistics |
| GET | `/` | List all (Admin) |

### Payments (`/api/payments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/bill` | Bill payment |
| POST | `/merchant` | Merchant payment |
| GET | `/{id}` | Get by ID |
| GET | `/customer/{customerId}` | Customer payments |
| GET | `/account/{accountId}` | Account payments |
| GET | `/` | List all (Admin) |

### Statements (`/api/statements`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/generate` | Generate statement |
| GET | `/account/{accountId}` | Account statements |
| GET | `/{id}` | Get by ID |
| GET | `/{id}/download/pdf` | Download PDF |
| GET | `/{id}/download/csv` | Download CSV |

### Fraud Detection (`/api/fraud`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/alerts` | All alerts |
| GET | `/alerts/high-risk` | High risk alerts |
| GET | `/alerts/recent` | Recent alerts |
| GET | `/score/{transactionId}` | Risk score |
| GET | `/rules` | All rules |
| POST | `/rules` | Create rule |
| PUT | `/rules/{id}` | Update rule |
| DELETE | `/rules/{id}` | Delete rule |

### Audit (`/api/audit`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/logs` | All audit logs |
| GET | `/customer/{customerId}` | Customer logs |
| GET | `/account/{accountId}` | Account logs |
| POST | `/search` | Search logs |
| GET | `/stats` | Statistics |
| GET | `/suspicious` | Suspicious activity |

### Notifications (`/api/notifications`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/user/{userId}` | User notifications |
| GET | `/user/{userId}/unread` | Unread only |
| PUT | `/{id}/read` | Mark as read |
| PUT | `/user/{userId}/read-all` | Mark all read |

### MFA (`/api/mfa`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/status` | MFA status |
| POST | `/totp/setup` | Setup TOTP |
| POST | `/totp/verify` | Verify TOTP |
| POST | `/email/setup` | Setup Email OTP |
| POST | `/email/verify` | Verify Email OTP |
| POST | `/sms/setup` | Setup SMS OTP |
| POST | `/sms/verify` | Verify SMS OTP |
| POST | `/disable` | Disable MFA |

### Actuator (`/actuator`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/info` | App info |
| GET | `/prometheus` | Metrics |

---

## 5. Configuration Files

### application.properties
- Database: PostgreSQL (env vars: DB_URL, DB_USERNAME, DB_PASSWORD)
- Redis: localhost:6379
- Kafka: localhost:9092
- JWT: Secret via JWT_SECRET env var
- Flyway: Enabled for migrations
- Actuator: Limited to health, info, prometheus

### Environment Variables Required
```
DB_URL=jdbc:postgresql://localhost:5432/rrbank
DB_USERNAME=postgres
DB_PASSWORD=<secure-password>
JWT_SECRET=<256-bit-secret>
```

---

## 6. Postman Collection

### Files Created
- `postman/RR-Bank-API-Collection.postman_collection.json` - Complete API collection
- `postman/RR-Bank-Local.postman_environment.json` - Local environment
- `postman/RR-Bank-Dev.postman_environment.json` - Dev environment

### Importing to Postman
1. Open Postman
2. Click "Import"
3. Select both JSON files
4. Select "RR-Bank - Local Development" environment

### Test Flow
1. Register user → Auto-saves tokens
2. Login → Auto-saves tokens
3. Create customer → Auto-saves customerId
4. Create account → Auto-saves accountId
5. Perform transactions, payments, etc.

---

## 7. Known Issues at Baseline

### Compilation Errors (To Fix)
1. Test files in wrong directory (`Banking_Application` → `banking`)
2. FraudRule entity missing fields
3. Event classes missing fields for AuditEventsConsumer
4. JwtTokenProvider using deprecated API
5. SecurityConfig incorrect constructor

### Missing Security Features (To Implement)
1. Rate limiting
2. Enhanced session management
3. Transaction signing
4. Field-level encryption
5. API versioning

---

## 8. Git Commands for Phase 0

### Create Baseline Branch
```bash
cd C:\Users\rajes\Desktop\projects\RR-Bank
git checkout -b hardening/v1-production
git add .
git commit -m "Phase 0: Baseline snapshot with Postman collection"
```

### Verify Baseline
```bash
git log --oneline -1
git status
```

---

## 9. Local Testing Checklist

### Pre-flight Checks
- [ ] Docker Desktop running
- [ ] Port 5432 available (PostgreSQL)
- [ ] Port 6379 available (Redis)
- [ ] Port 9092 available (Kafka)
- [ ] Port 8080 available (Backend)
- [ ] Port 5173 available (Frontend)

### Startup Sequence
1. Start Docker services: `docker-compose up -d`
2. Wait for health checks
3. Start backend: `mvnw spring-boot:run`
4. Verify: `curl http://localhost:8080/actuator/health`
5. Start frontend: `npm run dev`

### API Smoke Tests
- [ ] `GET /actuator/health` → 200 OK
- [ ] `POST /api/auth/register` → 200 OK
- [ ] `POST /api/auth/login` → 200 OK with tokens
- [ ] `GET /api/auth/health` → 200 OK

---

## 10. Next Steps (Phase 1)

After baseline is confirmed working:
1. Fix compilation errors
2. Implement rate limiting
3. Add API versioning
4. Enhance logging
5. Security hardening

---

**Document Version:** 1.0  
**Last Updated:** December 22, 2024
