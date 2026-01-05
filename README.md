# RR-Bank Microservices

A production-ready banking application with microservices architecture.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           FRONTEND (React)                          │
│                              Port: 3000                             │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY (8080)                           │
│              JWT Validation │ Routing │ Rate Limiting               │
└───────────────────────────────────┬─────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐     ┌───────────────────┐     ┌───────────────────┐
│ AUTH SERVICE  │     │ ACCOUNT SERVICE   │     │TRANSACTION SERVICE│
│    (8081)     │     │     (8083)        │     │     (8084)        │
│ Registration  │     │ Account CRUD      │     │  SAGA Orchestrator│
│ Login/JWT     │     │ Balance (→Ledger) │     │ Deposit/Withdraw  │
└───────────────┘     └───────────────────┘     │ Transfer          │
                                                └─────────┬─────────┘
                                                          │
                      ┌─────────────────┬─────────────────┼─────────────────┐
                      │                 │                 │                 │
                      ▼                 ▼                 ▼                 ▼
              ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
              │LEDGER SERVICE │ │ FRAUD SERVICE │ │NOTIFICATION   │ │CUSTOMER SERV  │
              │   (8085)      │ │   (8087)      │ │   (8086)      │ │   (8082)      │
              │ Source of     │ │ Daily Limits  │ │ Kafka Consumer│ │ Profile/KYC   │
              │ Truth         │ │ Velocity Check│ │ Push/Email    │ └───────────────┘
              │ Balance Calc  │ │ Risk Scoring  │ └───────────────┘
              └───────────────┘ └───────────────┘
                                              │
                                              ▼
                                    ┌───────────────────┐
                                    │  AUDIT SERVICE    │
                                    │     (8088)        │
                                    │  Immutable Logs   │
                                    └───────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                               │
├─────────────────┬─────────────────┬─────────────────────────────────┤
│   PostgreSQL    │      Redis      │            Kafka                │
│   (5432)        │     (6379)      │           (9092)                │
│ 8 Databases     │   Caching       │    Event Streaming              │
└─────────────────┴─────────────────┴─────────────────────────────────┘
```

## Project Structure

```
RR-Bank/
├── api-gateway/                 # Spring Cloud Gateway (8080)
├── services/
│   ├── auth-service/            # Authentication & JWT (8081)
│   ├── customer-service/        # Customer profiles (8082)
│   ├── account-service/         # Account management (8083)
│   ├── transaction-service/     # SAGA orchestrator (8084)
│   ├── ledger-service/          # Source of truth (8085)
│   ├── notification-service/    # Notifications (8086)
│   ├── fraud-service/           # Fraud detection (8087)
│   └── audit-service/           # Audit logging (8088)
├── frontend/                    # React application
├── docker/                      # Docker configurations
├── docker-compose.yml           # Full stack orchestration
└── .env.example                 # Environment template
```

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- Node.js 18+ (for frontend development)

### Start with Docker

```bash
# Navigate to project
cd RR-Bank

# Create .env file
copy .env.example .env

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Service URLs

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |
| Customer Service | 8082 | http://localhost:8082 |
| Account Service | 8083 | http://localhost:8083 |
| Transaction Service | 8084 | http://localhost:8084 |
| Ledger Service | 8085 | http://localhost:8085 |
| Notification Service | 8086 | http://localhost:8086 |
| Fraud Service | 8087 | http://localhost:8087 |
| Audit Service | 8088 | http://localhost:8088 |

## API Examples

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "Password123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john@example.com",
    "password": "Password123!"
  }'
```

### Create Account
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "SAVINGS"
  }'
```

### Deposit
```bash
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "uuid-here",
    "amount": 1000.00,
    "description": "Initial deposit"
  }'
```

### Transfer
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "source-uuid",
    "toAccountId": "dest-uuid",
    "amount": 500.00
  }'
```

## Key Features

### Ledger Service (Source of Truth)
- Append-only ledger entries
- Balance calculated from entries (not stored)
- Immutable transaction history

### Transaction SAGA Orchestrator
- Orchestration-based SAGA pattern
- Compensation for failed transfers
- Idempotency support

### Fraud Service
- Daily transaction limits ($10,000 default)
- Velocity checks (5 withdrawals/hour)
- Risk scoring

### Event-Driven Architecture
- Kafka for async communication
- Events: user-created, transaction-completed, transaction-failed
- Notification service subscribes to events

## License

MIT License
