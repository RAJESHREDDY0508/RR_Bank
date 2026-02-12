# Development Guide

This guide covers local development setup, project conventions, and workflow for contributing to RR-Bank.

## Local Development Setup

### Backend (Java / Spring Boot)

#### Requirements

- Java 17+
- Maven 3.8+
- Docker (for infrastructure services)

#### Running a Service Locally

Start the infrastructure dependencies first:

```bash
docker-compose up -d postgres redis kafka zookeeper
```

Then run the desired service with Maven:

```bash
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Each service's `application.yml` contains default configuration suitable for local development. Override values using environment variables or a local profile (`application-local.yml`, which is gitignored).

### Frontend (React)

#### Customer Application

```bash
cd frontend
npm install
npm run dev
```

Runs on http://localhost:3000.

#### Admin Console

```bash
cd frontend/admin-app
npm install
npm run dev
```

Runs on http://localhost:3001.

Both frontends use Vite for fast HMR (Hot Module Replacement) during development.

---

## Project Conventions

### Backend

| Convention            | Standard                                           |
|-----------------------|----------------------------------------------------|
| Language              | Java 17                                            |
| Framework             | Spring Boot                                        |
| Build tool            | Maven                                              |
| Package structure     | `com.rrbank.<service>` (controller, service, repository, entity, dto) |
| API prefix            | `/api/<domain>` (e.g., `/api/auth`, `/api/accounts`) |
| Database naming       | snake_case for tables and columns                  |
| Configuration         | `application.yml` per service                      |

### Frontend (Admin Console)

| Convention            | Standard                                           |
|-----------------------|----------------------------------------------------|
| Language              | TypeScript 5.3                                     |
| Framework             | React 18                                           |
| State management      | Redux Toolkit                                      |
| UI library            | Material-UI 5                                      |
| Build tool            | Vite                                               |
| File naming           | PascalCase for components, camelCase for utilities  |
| API layer             | Axios client in `src/api/`                         |

### Git Conventions

- Branch from `main` for all feature work
- Use descriptive branch names: `feature/kyc-approval`, `fix/ledger-balance-cache`
- Write meaningful commit messages that explain the *why*, not just the *what*

---

## Service Port Assignments

| Port | Service              |
|------|----------------------|
| 3000 | Customer Frontend    |
| 3001 | Admin Console        |
| 5432 | PostgreSQL           |
| 6379 | Redis                |
| 8080 | API Gateway          |
| 8081 | Auth Service         |
| 8082 | Customer Service     |
| 8083 | Account Service      |
| 8084 | Transaction Service  |
| 8085 | Ledger Service       |
| 8086 | Notification Service |
| 8087 | Fraud Service        |
| 8088 | Audit Service        |
| 8089 | Admin Service        |
| 9092 | Kafka                |

---

## Database Development

### Schema Files

Database schemas are defined in `database/` and `docker/init-db.sql`. Changes to the schema should be:

1. Applied to the SQL files in `database/`
2. Tested locally with a fresh database (`docker-compose down -v && docker-compose up -d`)
3. Documented in the PR description

### Connecting to the Database

```bash
# Via Docker
docker exec -it rrbank-postgres psql -U rrbank -d auth_db

# Direct connection
psql -h localhost -p 5432 -U rrbank -d auth_db
```

---

## Adding a New Service

1. Create a new directory under `services/<service-name>/`
2. Initialize a Spring Boot project with the standard package structure
3. Add the service to `docker-compose.yml` with health checks and network configuration
4. Create its database in `docker/init-db.sql`
5. Add API Gateway routing rules in `api-gateway/src/main/resources/application.yml`
6. Update Kubernetes manifests if applicable

---

## Debugging

### Backend Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f transaction-service
```

### Frontend Debugging

Both frontend applications support browser DevTools debugging. Vite provides source maps in development mode.

### Kafka Events

To inspect Kafka topics:

```bash
# List topics
docker exec -it rrbank-kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume from a topic
docker exec -it rrbank-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transaction-completed \
  --from-beginning
```

### Database Queries

Use any PostgreSQL client or the CLI to inspect data:

```bash
docker exec -it rrbank-postgres psql -U rrbank -d ledger_db \
  -c "SELECT * FROM ledger_entries ORDER BY created_at DESC LIMIT 10;"
```

---

## Testing

### API Testing with Postman

Import the collection from `postman/` into Postman. The collection includes:
- Authentication flow (register, login)
- Account operations
- Transaction flows (deposit, withdraw, transfer)
- Admin operations

### Manual Integration Testing

1. Start all services via Docker Compose
2. Register a user through the Customer Portal
3. Approve KYC through the Admin Console
4. Perform transactions and verify ledger consistency

---

## Build Scripts

Build automation scripts are located in `scripts/`:

| Script                  | Purpose                                  |
|-------------------------|------------------------------------------|
| `build-all.sh`          | Build all service Docker images          |
| `push-to-ocir.sh`       | Push images to Oracle Container Registry |
| `deploy-to-oke.sh`      | Deploy to Oracle Kubernetes Engine       |
| `init-databases.sh`     | Initialize all service databases         |
| `generate-env-vars.sh`  | Generate environment variable templates  |
