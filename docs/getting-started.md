# Getting Started

This guide walks through setting up the RR-Bank platform for local development and first-time use.

## Prerequisites

| Tool            | Version  | Purpose                         |
|-----------------|----------|---------------------------------|
| Docker Desktop  | 20.10+   | Container runtime               |
| Docker Compose  | 2.0+     | Multi-container orchestration   |
| Git             | 2.30+    | Source control                  |
| Java            | 17+      | Backend development (optional)  |
| Maven           | 3.8+     | Java build tool (optional)      |
| Node.js         | 18+      | Frontend development (optional) |

> Java, Maven, and Node.js are only required for local development outside Docker.

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd RR-Bank
```

### 2. Configure Environment

```bash
# Copy the environment template
cp .env.example .env
```

Edit `.env` to customize database credentials, JWT secrets, and service ports if needed. Defaults work out of the box for local development.

### 3. Start the Platform

```bash
# Build and start all services
docker-compose up -d

# Monitor startup progress
docker-compose logs -f
```

First startup takes several minutes as Docker builds all service images and initializes databases.

### 4. Verify Services

```bash
# Check that all containers are running
docker-compose ps
```

All services should show a `healthy` or `running` status. If a service fails to start, check its logs:

```bash
docker-compose logs <service-name>
```

## First-Time Walkthrough

### 1. Access the Admin Console

Navigate to http://localhost:3001 and log in with the default admin credentials:

| Field    | Value           |
|----------|-----------------|
| Username | `superadmin`    |
| Password | `Admin@123456`  |

### 2. Register a Customer

From the Customer Portal at http://localhost:3000, register a new customer account. The system sets the KYC status to `PENDING` automatically.

### 3. Approve KYC

In the Admin Console, navigate to **KYC Requests** and approve the pending customer. This unlocks transaction capabilities for that customer.

### 4. Perform Transactions

After KYC approval, the customer can:
- Create savings or checking accounts
- Deposit funds
- Transfer between accounts
- View transaction history

## Service Startup Order

Docker Compose manages dependencies, but the recommended startup order is:

1. **Infrastructure**: PostgreSQL, Redis, Kafka, Zookeeper
2. **Core Services**: Auth, Customer, Account, Ledger
3. **Processing Services**: Transaction, Fraud, Notification, Audit
4. **Admin & Gateway**: Admin Service, API Gateway
5. **Frontends**: Customer App, Admin Console

## Common Operations

### Stop All Services

```bash
docker-compose down
```

### Rebuild a Single Service

```bash
docker-compose build --no-cache <service-name>
docker-compose up -d <service-name>
```

### Rebuild Everything

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### View Logs for a Specific Service

```bash
docker-compose logs -f transaction-service
```

### Reset Database State

```bash
docker-compose down -v    # Removes volumes (destroys data)
docker-compose up -d      # Fresh start with clean databases
```

## Troubleshooting

### Service fails to connect to PostgreSQL

PostgreSQL may not be ready when the service starts. Docker health checks handle this, but if it persists:

```bash
docker-compose restart <service-name>
```

### Kafka connection errors on startup

Kafka takes longer to initialize than most services. Wait 30-60 seconds after startup, then verify:

```bash
docker-compose logs kafka | tail -20
```

### Port conflicts

If ports are already in use, update the port mappings in `docker-compose.yml` or stop the conflicting process.

### DNS resolution failures during Maven builds

If Maven cannot resolve dependencies inside Docker, ensure Docker Desktop has DNS configured correctly. The `docker-compose.yml` includes DNS overrides (`8.8.8.8`, `1.1.1.1`) for this purpose.

## Next Steps

- [API Reference](api-reference.md) -- Explore the API endpoints
- [Development Guide](development.md) -- Set up for local backend/frontend development
- [Architecture](architecture.md) -- Understand the system design
