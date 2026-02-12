<p align="center">
  <h1 align="center">RR-Bank</h1>
  <p align="center">Enterprise Microservices Banking Platform</p>
</p>

<p align="center">
  <a href="#architecture">Architecture</a> &middot;
  <a href="#quick-start">Quick Start</a> &middot;
  <a href="docs/api-reference.md">API Reference</a> &middot;
  <a href="docs/deployment.md">Deployment</a> &middot;
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

## Overview

RR-Bank is a production-grade banking platform built on a distributed microservices architecture. It provides end-to-end banking operations including customer onboarding, account management, real-time transaction processing with SAGA orchestration, fraud detection, KYC compliance, and comprehensive audit logging.

### Key Capabilities

- **Distributed Transaction Processing** -- SAGA-based orchestration with compensation and idempotency
- **Immutable Ledger** -- Append-only financial ledger as the single source of truth for all balances
- **Real-Time Fraud Detection** -- Velocity checks, daily limits, and risk scoring engine
- **KYC Compliance Workflow** -- End-to-end identity verification with admin approval pipeline
- **Event-Driven Architecture** -- Kafka-powered async communication across service boundaries
- **Role-Based Access Control** -- Granular RBAC for admin operations (Super Admin, Admin, Audit Officer, Fraud Analyst)

## Architecture

```
                         ┌──────────────────────┐
                         │   Client Applications │
                         │  Customer App (:3000) │
                         │  Admin Console (:3001)│
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │    API Gateway (:8080)│
                         │  JWT · Routing · CORS │
                         └──────────┬───────────┘
                                    │
          ┌────────────┬────────────┼────────────┬────────────┐
          ▼            ▼            ▼            ▼            ▼
   ┌────────────┐┌────────────┐┌────────────┐┌────────────┐┌────────────┐
   │   Auth     ││  Customer  ││  Account   ││Transaction ││   Admin    │
   │  (:8081)   ││  (:8082)   ││  (:8083)   ││  (:8084)   ││  (:8089)   │
   └────────────┘└────────────┘└────────────┘└─────┬──────┘└────────────┘
                                                   │
                              ┌──────────┬─────────┼─────────┐
                              ▼          ▼         ▼         ▼
                       ┌────────────┐┌────────────┐┌────────────┐┌────────────┐
                       │   Ledger   ││   Fraud    ││Notification││   Audit    │
                       │  (:8085)   ││  (:8087)   ││  (:8086)   ││  (:8088)   │
                       └────────────┘└────────────┘└────────────┘└────────────┘

   ┌─────────────────────────────────────────────────────────────────────────┐
   │                          Infrastructure                                 │
   │   PostgreSQL (:5432)  ·  Redis (:6379)  ·  Apache Kafka (:9092)        │
   └─────────────────────────────────────────────────────────────────────────┘
```

> For detailed architecture documentation, see [docs/architecture.md](docs/architecture.md).

## Technology Stack

| Layer          | Technology                                             |
|----------------|--------------------------------------------------------|
| **Backend**    | Java 17, Spring Boot, Spring Cloud Gateway, Spring Kafka |
| **Frontend**   | React 18, TypeScript, Material-UI, Redux Toolkit, Vite |
| **Database**   | PostgreSQL 15 (per-service isolation)                  |
| **Cache**      | Redis 7                                                |
| **Messaging**  | Apache Kafka 7.4                                       |
| **Containers** | Docker, Docker Compose                                 |
| **Orchestration** | Kubernetes (OKE-ready)                              |

## Quick Start

### Prerequisites

| Requirement     | Version |
|-----------------|---------|
| Docker Desktop  | 20.10+  |
| Docker Compose  | 2.0+    |
| Java (optional) | 17+     |
| Node.js (optional) | 18+  |

### Launch the Platform

```bash
# Clone the repository
git clone <repository-url> && cd RR-Bank

# Configure environment
cp .env.example .env

# Start all services
docker-compose up -d

# Verify health
docker-compose ps
```

### Access Points

| Application       | URL                        | Description                 |
|-------------------|----------------------------|-----------------------------|
| Customer Portal   | http://localhost:3000      | Customer-facing banking app |
| Admin Console     | http://localhost:3001      | Administration dashboard    |
| API Gateway       | http://localhost:8080      | Unified API entry point     |

> For detailed setup instructions including local development, see [docs/getting-started.md](docs/getting-started.md).

## Project Structure

```
RR-Bank/
├── api-gateway/                # Spring Cloud Gateway
├── services/
│   ├── auth-service/           # Authentication & JWT management
│   ├── customer-service/       # Customer profiles & KYC
│   ├── account-service/        # Account lifecycle management
│   ├── transaction-service/    # SAGA transaction orchestrator
│   ├── ledger-service/         # Immutable financial ledger
│   ├── notification-service/   # Event-driven notifications
│   ├── fraud-service/          # Fraud detection engine
│   ├── audit-service/          # Compliance audit logging
│   └── admin-service/          # Admin operations & reporting
├── frontend/
│   ├── src/                    # Customer application (React)
│   └── admin-app/              # Admin console (React + TypeScript)
├── database/                   # Schema definitions & migrations
├── kubernetes/                 # Kubernetes manifests
├── scripts/                    # Build & deployment automation
├── postman/                    # API test collections
├── docs/                       # Project documentation
├── docker-compose.yml          # Local orchestration
└── docker-compose.oracle.yml   # Oracle Cloud deployment
```

## Services

| Service              | Port | Responsibility                                      |
|----------------------|------|-----------------------------------------------------|
| **API Gateway**      | 8080 | Request routing, JWT validation, rate limiting       |
| **Auth Service**     | 8081 | User registration, login, JWT token management       |
| **Customer Service** | 8082 | Customer profiles, KYC status management             |
| **Account Service**  | 8083 | Account CRUD, balance queries (via Ledger)           |
| **Transaction Service** | 8084 | SAGA orchestrator for deposits, withdrawals, transfers |
| **Ledger Service**   | 8085 | Append-only ledger, authoritative balance calculation |
| **Notification Service** | 8086 | Kafka-driven email and push notifications         |
| **Fraud Service**    | 8087 | Transaction risk scoring, velocity & limit checks    |
| **Audit Service**    | 8088 | Immutable audit trail for compliance                 |
| **Admin Service**    | 8089 | Dashboard metrics, KYC management, system admin      |

## Documentation

| Document                                          | Description                              |
|---------------------------------------------------|------------------------------------------|
| [Architecture](docs/architecture.md)              | System design, patterns, data flow       |
| [Getting Started](docs/getting-started.md)        | Setup, configuration, first run          |
| [API Reference](docs/api-reference.md)            | Endpoint specifications and examples     |
| [Development Guide](docs/development.md)          | Local development, testing, conventions  |
| [Deployment Guide](docs/deployment.md)            | Docker, Kubernetes, Oracle Cloud         |
| [Contributing](CONTRIBUTING.md)                   | Contribution guidelines and workflow     |
| [Changelog](CHANGELOG.md)                         | Release history and version notes        |

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
