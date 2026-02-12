# Architecture

This document describes the system architecture, design patterns, and data flow of the RR-Bank platform.

## System Overview

RR-Bank follows a microservices architecture with 9 domain services, an API gateway, and two client applications. Each service owns its data store, communicates synchronously via REST and asynchronously via Apache Kafka, and is independently deployable.

```
                    ┌─────────────────────────────┐
                    │      Client Applications     │
                    │  Customer App · Admin Console │
                    └──────────────┬──────────────┘
                                   │ HTTPS
                    ┌──────────────▼──────────────┐
                    │       API Gateway (:8080)     │
                    │  JWT Validation · Routing     │
                    │  CORS · Rate Limiting         │
                    └──────────────┬──────────────┘
                                   │
        ┌──────────┬──────────┬────┴────┬──────────┬──────────┐
        ▼          ▼          ▼         ▼          ▼          ▼
   ┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
   │  Auth   ││Customer ││ Account ││  Txn    ││  Admin  ││  ...    │
   │ Service ││ Service ││ Service ││ Service ││ Service ││         │
   └────┬────┘└────┬────┘└────┬────┘└────┬────┘└────┬────┘└─────────┘
        │          │          │         │          │
        └──────────┴──────────┴────┬────┴──────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │        Apache Kafka          │
                    │     Event Bus (:9092)        │
                    └──────────────┬──────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
       ┌────────────┐      ┌────────────┐      ┌────────────┐
       │ PostgreSQL  │      │   Redis    │      │ Zookeeper  │
       │  (:5432)   │      │  (:6379)   │      │  (:2181)   │
       └────────────┘      └────────────┘      └────────────┘
```

## Design Principles

1. **Database-per-Service** -- Each microservice has its own PostgreSQL schema, preventing tight coupling at the data layer.
2. **API Gateway Pattern** -- All client traffic passes through a single entry point for centralized authentication, routing, and rate limiting.
3. **Event-Driven Communication** -- Services publish domain events to Kafka topics; interested services subscribe asynchronously.
4. **SAGA Orchestration** -- Distributed transactions use the orchestration-based SAGA pattern with explicit compensation logic.
5. **Immutable Ledger** -- Financial state is derived from an append-only ledger rather than mutable balance fields.

## Service Responsibilities

### Auth Service (:8081)
Handles user registration, login, and JWT token lifecycle. Issues signed JWTs consumed by the API Gateway for downstream authorization. On successful registration, publishes a `user-created` event to Kafka.

### Customer Service (:8082)
Manages customer profiles and KYC (Know Your Customer) status. Listens for `user-created` events to automatically provision customer records. Exposes endpoints for KYC status transitions (PENDING, APPROVED, REJECTED).

### Account Service (:8083)
Provides account lifecycle management (create, freeze, unfreeze, close). Delegates balance queries to the Ledger Service to ensure consistency with the financial source of truth.

### Transaction Service (:8084)
The SAGA orchestrator for all financial operations -- deposits, withdrawals, and transfers. Coordinates across Ledger, Fraud, and Notification services. Implements compensation logic for failed multi-step transactions and supports idempotent operations.

### Ledger Service (:8085)
The single source of truth for all financial data. Maintains an append-only ledger of debit/credit entries. Balances are calculated from the sum of ledger entries, never stored as a mutable field. Uses Redis for balance caching to improve read performance.

### Notification Service (:8086)
Subscribes to Kafka events (`transaction-completed`, `transaction-failed`, `user-created`) and dispatches notifications via email and push channels.

### Fraud Service (:8087)
Evaluates transaction risk before execution. Enforces:
- **Daily limits**: $10,000 per account per day
- **Velocity checks**: Maximum 5 withdrawals per hour
- **Risk scoring**: Composite score based on transaction amount, frequency, and history

### Audit Service (:8088)
Records immutable audit entries for all significant system events. Supports compliance queries, filtering, and export for regulatory reporting.

### Admin Service (:8089)
Provides aggregate dashboard metrics, KYC management workflows, and system administration capabilities. Powers the Admin Console frontend.

## Data Flow

### Transaction Lifecycle (Transfer)

```
Customer App → API Gateway → Transaction Service
                                     │
                    ┌────────────────┼────────────────┐
                    ▼                │                ▼
             Fraud Service           │          Ledger Service
             (risk check)           │          (debit source)
                    │                │                │
                    ▼                │                ▼
              [APPROVED]             │          Ledger Service
                                     │          (credit target)
                                     │                │
                              ┌──────┴──────┐         │
                              ▼             ▼         ▼
                       Audit Service  Kafka Event  [COMPLETED]
                                          │
                                          ▼
                                  Notification Service
                                  (email confirmation)
```

If any step fails, the Transaction Service executes compensation steps to reverse prior operations (e.g., reversing a debit if the credit fails).

### Event Topics

| Topic                    | Producer             | Consumers                        |
|--------------------------|----------------------|----------------------------------|
| `user-created`           | Auth Service         | Customer Service, Notification   |
| `transaction-completed`  | Transaction Service  | Notification, Audit              |
| `transaction-failed`     | Transaction Service  | Notification, Audit              |

## Database Architecture

Each service connects to its own isolated database within a shared PostgreSQL instance:

| Service              | Database         | Key Tables                           |
|----------------------|------------------|--------------------------------------|
| Auth Service         | `auth_db`        | users, roles, tokens                 |
| Customer Service     | `customer_db`    | customers, kyc_documents             |
| Account Service      | `account_db`     | accounts, account_types              |
| Transaction Service  | `transaction_db` | transactions, saga_state             |
| Ledger Service       | `ledger_db`      | ledger_entries, balance_cache        |
| Fraud Service        | `fraud_db`       | fraud_rules, fraud_alerts            |
| Notification Service | `notification_db`| notifications, templates             |
| Audit Service        | `audit_db`       | audit_logs                           |
| Admin Service        | `admin_db`       | admin_users, settings                |

## Caching Strategy

Redis is used for:
- **Balance caching** (Ledger Service) -- Computed balances are cached to avoid recalculating from the full ledger entry history on every read.
- **Session data** -- Short-lived session metadata for active user contexts.

Cache invalidation follows a write-through pattern: ledger writes update both the persistent store and the Redis cache atomically.

## Security Architecture

### Authentication Flow

1. Client submits credentials to `/api/auth/login`
2. Auth Service validates credentials and issues a JWT
3. Client includes JWT in `Authorization: Bearer <token>` header
4. API Gateway validates the JWT signature and expiry on every request
5. Validated requests are forwarded to downstream services with user context headers

### Authorization Model (Admin Console)

| Role             | Permissions                                          |
|------------------|------------------------------------------------------|
| SUPER_ADMIN      | Full system access, user management, settings        |
| ADMIN_USER       | Customer management, account operations, KYC review  |
| AUDIT_OFFICER    | Read-only access to audit logs and compliance data   |
| FRAUD_ANALYST    | Fraud alerts, risk analysis, transaction review      |

## Network Topology

All services communicate over a Docker bridge network (`rrbank-network`). Service discovery is handled by Docker DNS, with each service addressable by its container name (e.g., `auth-service:8081`).

External access is exclusively through the API Gateway on port 8080 and the frontend applications on ports 3000/3001.
