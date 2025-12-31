# RR-Bank Production-Ready Microservices Architecture

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                    CLIENTS                                        │
│                    (React Frontend / Mobile Apps / Third Party)                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY (8080)                                   │
│              - JWT Validation - Rate Limiting - CORS - Routing                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        ▼                               ▼                               ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│  AUTH SERVICE │              │   CUSTOMER    │              │   ACCOUNTS    │
│    (8082)     │              │   SERVICE     │              │   SERVICE     │
│               │              │    (8083)     │              │    (8084)     │
│ - Login       │              │ - Profiles    │              │ - Account CRUD│
│ - Register    │              │ - KYC-lite    │              │ - Status mgmt │
│ - JWT Issue   │              │ - Preferences │              │ - Types       │
│ - Refresh     │              └───────────────┘              └───────────────┘
└───────────────┘                                                     │
                                                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      TRANSACTION ORCHESTRATOR SERVICE (8085)                      │
│                                                                                   │
│  - Saga Orchestration for Deposit/Withdraw/Transfer                              │
│  - Validates requests via Limits-Fraud Service                                   │
│  - Creates Ledger entries atomically                                             │
│  - Emits events to Kafka                                                         │
└─────────────────────────────────────────────────────────────────────────────────┘
        │                               │                               │
        ▼                               ▼                               ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│    LEDGER     │              │ LIMITS-FRAUD  │              │ NOTIFICATIONS │
│   SERVICE     │              │   SERVICE     │              │   SERVICE     │
│    (8086)     │              │    (8087)     │              │    (8088)     │
│               │              │               │              │               │
│ - Append-only │              │ - Daily limit │              │ - Email stub  │
│ - Balance     │              │ - Velocity    │              │ - Push stub   │
│   computation │              │ - Fraud rules │              │ - In-app inbox│
│ - Source of   │              │ - Risk score  │              │ - Kafka sub   │
│   truth       │              └───────────────┘              └───────────────┘
└───────────────┘
        │
        ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│    AUDIT      │              │  STATEMENTS   │              │    ADMIN      │
│   SERVICE     │              │   SERVICE     │              │   SERVICE     │
│    (8089)     │              │    (8090)     │              │    (8091)     │
│               │              │               │              │               │
│ - Immutable   │              │ - Monthly gen │              │ - User mgmt   │
│   audit log   │              │ - PDF/CSV     │              │ - Account ops │
│ - Kafka sub   │              │ - History     │              │ - Reports     │
│ - Who/What    │              └───────────────┘              └───────────────┘
└───────────────┘

                    ┌─────────────────────────────────────────┐
                    │              KAFKA (9092)                │
                    │                                         │
                    │  Topics:                                │
                    │  - transaction.created                  │
                    │  - transaction.completed                │
                    │  - transaction.failed                   │
                    │  - ledger.entry.created                 │
                    │  - notification.requested               │
                    │  - audit.event                          │
                    │  - fraud.alert                          │
                    └─────────────────────────────────────────┘

                    ┌─────────────────────────────────────────┐
                    │           POSTGRESQL (5432)              │
                    │                                         │
                    │  Schemas (database-per-service):        │
                    │  - auth_db                              │
                    │  - customer_db                          │
                    │  - accounts_db                          │
                    │  - orchestrator_db                      │
                    │  - ledger_db                            │
                    │  - limits_fraud_db                      │
                    │  - notifications_db                     │
                    │  - audit_db                             │
                    │  - statements_db                        │
                    │  - admin_db                             │
                    └─────────────────────────────────────────┘
```

## Service List (11 Services Total)

| # | Service | Port | Database | Responsibility |
|---|---------|------|----------|----------------|
| 1 | api-gateway | 8080 | - | JWT validation, routing, rate limiting |
| 2 | auth-service | 8082 | auth_db | Authentication, JWT issuance, roles |
| 3 | customer-service | 8083 | customer_db | Customer profiles, KYC-lite |
| 4 | accounts-service | 8084 | accounts_db | Account CRUD, status management |
| 5 | transaction-orchestrator | 8085 | orchestrator_db | Saga orchestration, transaction coordination |
| 6 | ledger-service | 8086 | ledger_db | Append-only entries, balance computation |
| 7 | limits-fraud-service | 8087 | limits_fraud_db | Limits, velocity, fraud rules |
| 8 | notifications-service | 8088 | notifications_db | Email/push stubs, in-app notifications |
| 9 | audit-service | 8089 | audit_db | Immutable audit trail |
| 10 | statements-service | 8090 | statements_db | Statement generation |
| 11 | admin-service | 8091 | admin_db | Admin operations, reports |

## Database Strategy: Database-Per-Service

**Choice: Option A - Separate databases per service**

Rationale:
- True data isolation between services
- Independent scaling and backup strategies
- Clear ownership boundaries
- Easier to migrate to different database technologies later
- Each service can evolve its schema independently

Implementation:
- Single PostgreSQL instance with multiple databases
- Each service connects only to its own database
- Cross-service data accessed via APIs, not direct DB queries

## Event-Driven Architecture

### Kafka Topics

| Topic | Publisher | Subscribers | Purpose |
|-------|-----------|-------------|---------|
| `transaction.created` | orchestrator | audit, notifications | New transaction initiated |
| `transaction.completed` | orchestrator | audit, notifications, statements | Transaction succeeded |
| `transaction.failed` | orchestrator | audit, notifications | Transaction failed |
| `ledger.entry.created` | ledger | audit | Ledger entry recorded |
| `notification.requested` | orchestrator | notifications | Send notification |
| `audit.event` | all services | audit | Audit trail event |
| `fraud.alert` | limits-fraud | notifications, audit | Fraud detected |

### Event Payloads

```json
// transaction.created
{
  "eventId": "uuid",
  "transactionId": "uuid",
  "type": "DEPOSIT|WITHDRAW|TRANSFER",
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 100.00,
  "currency": "USD",
  "userId": "uuid",
  "timestamp": "2025-01-01T10:00:00Z"
}

// ledger.entry.created
{
  "eventId": "uuid",
  "entryId": "uuid",
  "accountId": "uuid",
  "entryType": "CREDIT|DEBIT",
  "amount": 100.00,
  "runningBalance": 1000.00,
  "transactionId": "uuid",
  "timestamp": "2025-01-01T10:00:00Z"
}

// notification.requested
{
  "eventId": "uuid",
  "userId": "uuid",
  "type": "TRANSACTION_COMPLETED|FRAUD_ALERT|STATEMENT_READY",
  "title": "Transaction Complete",
  "message": "Your deposit of $100 was successful",
  "channel": "IN_APP|EMAIL|PUSH",
  "timestamp": "2025-01-01T10:00:00Z"
}
```

## Saga Pattern: Transaction Orchestrator

### Deposit Saga

```
1. [Orchestrator] Receive deposit request
2. [Orchestrator] Validate request (amount > 0, account exists)
3. [Orchestrator] → [Limits-Fraud] Check limits
   - If REJECT → Return error
   - If REVIEW → Flag for manual review, proceed
   - If APPROVE → Continue
4. [Orchestrator] → [Ledger] Create CREDIT entry
5. [Orchestrator] Update transaction status to COMPLETED
6. [Orchestrator] → [Kafka] Emit transaction.completed
7. [Orchestrator] → [Kafka] Emit notification.requested
```

### Withdraw Saga

```
1. [Orchestrator] Receive withdraw request
2. [Orchestrator] → [Ledger] Get computed balance
3. [Orchestrator] Check sufficient funds
   - If insufficient → Return error
4. [Orchestrator] → [Limits-Fraud] Check limits + fraud
   - If REJECT → Return error
5. [Orchestrator] → [Ledger] Create DEBIT entry
6. [Orchestrator] Update transaction status to COMPLETED
7. [Orchestrator] → [Kafka] Emit transaction.completed
```

### Transfer Saga (with Compensation)

```
1. [Orchestrator] Receive transfer request
2. [Orchestrator] → [Ledger] Get source balance
3. [Orchestrator] Check sufficient funds
4. [Orchestrator] → [Limits-Fraud] Check limits + fraud
5. [Orchestrator] → [Ledger] Create DEBIT on source
   - Store debit entry ID for compensation
6. [Orchestrator] → [Ledger] Create CREDIT on destination
   - If fails:
     - [Orchestrator] → [Ledger] Reverse DEBIT (compensation)
     - Update transaction to FAILED
     - Emit transaction.failed
     - Return error
7. [Orchestrator] Update transaction status to COMPLETED
8. [Orchestrator] → [Kafka] Emit transaction.completed
```

## API Specifications

### Auth Service (/api/auth/*)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login, get JWT |
| POST | /api/auth/refresh | Refresh access token |
| POST | /api/auth/logout | Logout, invalidate token |
| GET | /api/auth/me | Get current user |

### Accounts Service (/api/accounts/*)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/accounts | Create account |
| GET | /api/accounts | List user's accounts |
| GET | /api/accounts/{id} | Get account details |
| GET | /api/accounts/{id}/balance | Get balance (from ledger) |
| PUT | /api/accounts/{id}/status | Update status |

### Transaction Orchestrator (/api/transactions/*)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/transactions/deposit | Initiate deposit |
| POST | /api/transactions/withdraw | Initiate withdraw |
| POST | /api/transactions/transfer | Initiate transfer |
| GET | /api/transactions/{id} | Get transaction status |
| GET | /api/transactions | List transactions |

### Ledger Service (Internal Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /internal/ledger/entries | Create entry |
| GET | /internal/ledger/accounts/{id}/balance | Get computed balance |
| GET | /internal/ledger/accounts/{id}/entries | List entries |

### Limits-Fraud Service (Internal Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /internal/limits/check | Check transaction limits |
| POST | /internal/fraud/analyze | Analyze for fraud |
| GET | /internal/limits/user/{id} | Get user limits |

## Implementation Sequence

### Phase 1: Foundation (Keep System Running)
1. Create shared library module for common DTOs/events
2. Restructure docker-compose with all databases
3. Add Kafka infrastructure

### Phase 2: Extract Core Services
1. Extract auth-service from banking-service
2. Extract customer-service
3. Extract accounts-service

### Phase 3: Build New Services
1. Create ledger-service (append-only)
2. Create limits-fraud-service
3. Create transaction-orchestrator (saga)

### Phase 4: Supporting Services
1. Create notifications-service
2. Create audit-service
3. Create statements-service
4. Create admin-service

### Phase 5: Integration & Cleanup
1. Update gateway routes
2. Fix frontend to use VITE_API_BASE_URL
3. Remove old banking-service
4. Create test scripts

## Security

### Service-to-Service Communication
- Internal services use `X-Internal-Token` header
- Token: SHA256 hash of shared secret
- Gateway strips this header from external requests

### JWT Structure
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "roles": ["CUSTOMER", "ADMIN"],
  "iat": 1704067200,
  "exp": 1704153600
}
```

## Folder Structure

```
RR-Bank/
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
├── shared-lib/
│   ├── pom.xml
│   └── src/main/java/com/rrbank/shared/
│       ├── dto/
│       ├── event/
│       └── security/
├── api-gateway/
├── auth-service/
├── customer-service/
├── accounts-service/
├── transaction-orchestrator/
├── ledger-service/
├── limits-fraud-service/
├── notifications-service/
├── audit-service/
├── statements-service/
├── admin-service/
├── frontend/
└── scripts/
    ├── test-flows.sh
    └── init-databases.sql
```
