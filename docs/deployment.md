# Deployment Guide

This guide covers deploying RR-Bank across different environments: local Docker, Kubernetes, and Oracle Cloud Infrastructure.

## Deployment Targets

| Environment        | Configuration File            | Purpose              |
|--------------------|-------------------------------|----------------------|
| Local Development  | `docker-compose.yml`          | Development & testing|
| Oracle Cloud       | `docker-compose.oracle.yml`   | Oracle DB deployment |
| Kubernetes         | `kubernetes/*.yaml`           | Production cluster   |

---

## Docker Compose (Local)

### Full Stack Deployment

```bash
# Configure environment
cp .env.example .env

# Build and start
docker-compose up -d --build

# Verify
docker-compose ps
```

### Selective Service Deployment

```bash
# Start only infrastructure
docker-compose up -d postgres redis kafka zookeeper

# Start a specific service
docker-compose up -d --build auth-service
```

### Environment Variables

Key environment variables in `.env`:

| Variable              | Description                    | Default        |
|-----------------------|--------------------------------|----------------|
| `POSTGRES_USER`       | Database username              | `rrbank`       |
| `POSTGRES_PASSWORD`   | Database password              | `rrbank123`    |
| `JWT_SECRET`          | JWT signing key                | (generated)    |
| `KAFKA_BOOTSTRAP`     | Kafka broker address           | `kafka:9092`   |
| `REDIS_HOST`          | Redis server address           | `redis`        |

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.24+)
- `kubectl` configured with cluster access
- Container registry access (Docker Hub or OCIR)

### Deployment Steps

#### 1. Build and Push Images

```bash
# Build all service images
./scripts/build-all.sh

# Push to container registry
./scripts/push-to-ocir.sh
```

#### 2. Create Namespace and Secrets

```bash
kubectl apply -f kubernetes/00-namespace-secrets.yaml
```

#### 3. Deploy Services

```bash
# Deploy all manifests
kubectl apply -f kubernetes/
```

#### 4. Verify Deployment

```bash
kubectl get pods -n rrbank
kubectl get services -n rrbank
```

### Kubernetes Manifests

| File                              | Resource                        |
|-----------------------------------|---------------------------------|
| `00-namespace-secrets.yaml`       | Namespace, secrets, config maps |
| `09-api-gateway.yaml`            | API Gateway deployment          |
| `10-admin-service.yaml`          | Admin Service deployment        |
| `11-customer-frontend.yaml`      | Customer App deployment         |
| `12-admin-frontend.yaml`         | Admin Console deployment        |

---

## Oracle Cloud Infrastructure (OKE)

### Prerequisites

- Oracle Cloud account with OKE cluster provisioned
- OCI CLI configured
- OCIR (Oracle Container Image Registry) access

### Deployment Steps

#### 1. Configure Oracle Environment

```bash
cp .env.oracle.example .env
# Edit .env with Oracle-specific values (DB connection, OCIR path, etc.)
```

#### 2. Build for Oracle

```bash
docker-compose -f docker-compose.oracle.yml build
```

#### 3. Push to OCIR

```bash
./scripts/push-to-ocir.sh
```

#### 4. Deploy to OKE

```bash
./scripts/deploy-to-oke.sh
```

---

## Health Checks

All services expose health check endpoints used by Docker and Kubernetes:

| Service             | Health Endpoint                         |
|---------------------|-----------------------------------------|
| API Gateway         | `GET /actuator/health`                  |
| All Spring Services | `GET /actuator/health`                  |
| Frontend Apps       | HTTP 200 on root `/`                    |

Docker Compose uses these for dependency ordering. Kubernetes uses them for readiness and liveness probes.

---

## Scaling Considerations

### Horizontal Scaling

Stateless services (Auth, Account, Transaction, Fraud, Audit, Admin) can be scaled horizontally behind the API Gateway:

```bash
# Docker Compose
docker-compose up -d --scale transaction-service=3

# Kubernetes
kubectl scale deployment transaction-service -n rrbank --replicas=3
```

### Services That Require Care When Scaling

- **Ledger Service** -- Ensure balance cache consistency when running multiple replicas.
- **Notification Service** -- Kafka consumer group partitioning handles multi-instance scenarios automatically.

### Database Scaling

For production workloads, consider:
- Connection pooling (HikariCP is configured by default)
- Read replicas for query-heavy services
- Dedicated PostgreSQL instances per service instead of a shared instance

---

## Rollback Procedure

### Docker Compose

```bash
# Stop the affected service
docker-compose stop <service-name>

# Rebuild with the previous image
docker-compose build --no-cache <service-name>

# Restart
docker-compose up -d <service-name>
```

### Kubernetes

```bash
# View rollout history
kubectl rollout history deployment/<service-name> -n rrbank

# Rollback to previous revision
kubectl rollout undo deployment/<service-name> -n rrbank
```

---

## Production Checklist

- [ ] Change all default passwords and JWT secrets in `.env`
- [ ] Enable HTTPS/TLS termination at the load balancer or API Gateway
- [ ] Configure PostgreSQL with proper replication and backups
- [ ] Set up monitoring and alerting (Prometheus, Grafana recommended)
- [ ] Configure log aggregation (ELK stack or equivalent)
- [ ] Review and adjust fraud detection thresholds
- [ ] Set up Kafka topic replication factor > 1
- [ ] Enable Redis persistence (AOF or RDB)
- [ ] Run security audit on exposed endpoints
- [ ] Configure rate limiting at the API Gateway
