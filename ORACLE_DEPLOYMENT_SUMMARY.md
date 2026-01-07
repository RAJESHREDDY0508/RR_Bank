# RR-Bank Oracle Cloud Deployment Summary

## Quick Start

### 1. Prerequisites Setup

```bash
# Install OCI CLI
bash -c "$(curl -L https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh)"

# Configure OCI CLI
oci setup config

# Install kubectl
# Download from https://kubernetes.io/docs/tasks/tools/

# Install Docker
# Download from https://docs.docker.com/get-docker/
```

### 2. OCI Resources to Create

| Resource | Service | Purpose |
|----------|---------|---------|
| PostgreSQL DB | OCI Database | Main database |
| Redis Cluster | OCI Cache | Session & rate limiting |
| Stream Pool | OCI Streaming | Kafka messaging |
| Container Registry | OCIR | Docker images |
| Kubernetes Cluster | OKE | Container orchestration |
| Load Balancer | OCI LB | Traffic distribution |
| Object Storage | OCI Storage | Frontend hosting |

### 3. Kafka Topics (OCI Streaming)

Create these streams in your Stream Pool:
- `user-events` - User registration, login, logout events
- `transaction-events` - Transaction lifecycle events
- `ledger-events` - Ledger entry events
- `balance-updated` - Balance change events
- `audit-events` - Audit trail events
- `notification-events` - Notification events

### 4. Environment Setup

```bash
# Copy environment template
cp .env.oracle.example .env.oracle

# Edit with your values
nano .env.oracle
```

### 5. Build & Deploy

#### Option A: Docker Compose (Single VM)

```bash
# Build images
./scripts/build-all.sh

# Push to OCIR
export OCI_USERNAME="tenancy/user"
export OCI_AUTH_TOKEN="your-token"
./scripts/push-to-ocir.sh

# Deploy
docker-compose -f docker-compose.oracle.yml up -d
```

#### Option B: Kubernetes (OKE)

```bash
# Build and push images
./scripts/build-all.sh
./scripts/push-to-ocir.sh

# Update Kubernetes secrets
kubectl apply -f kubernetes/00-namespace-secrets.yaml

# Deploy all services
./scripts/deploy-to-oke.sh
```

### 6. Database Setup

```bash
# Connect to your OCI PostgreSQL
psql -h <db-host> -U rrbank -d rrbank

# Run schema
\i database/oracle-schema.sql
```

### 7. Verification

```bash
# Check all services
curl https://your-api-gateway/actuator/health

# Test registration
curl -X POST https://your-api-gateway/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Test123!","firstName":"Test","lastName":"User"}'
```

---

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Public entry point |
| Auth Service | 8081 | Authentication |
| Customer Service | 8082 | Customer profiles |
| Account Service | 8083 | Bank accounts |
| Transaction Service | 8084 | Transactions |
| Ledger Service | 8085 | Source of truth |
| Notification Service | 8086 | Emails & notifications |
| Fraud Service | 8087 | Fraud detection |
| Audit Service | 8088 | Audit logging |
| Frontend | 3000/80 | Web UI |

---

## Kafka Event Flow

```
┌──────────────┐     user-events      ┌──────────────┐
│ Auth Service │─────────────────────▶│ Audit Service│
└──────────────┘                      └──────────────┘
                                             │
┌──────────────┐  transaction-events         │
│ Transaction  │─────────────────────────────┤
│   Service    │                             │
└──────────────┘                             │
       │                                     │
       │         ledger-events               │
       ▼                                     │
┌──────────────┐                             │
│Ledger Service│─────────────────────────────┤
└──────────────┘                             │
       │                                     │
       │        balance-updated              │
       └────────────────────────────────────▶│
                                             ▼
                                      ┌──────────────┐
                                      │   All Events │
                                      │   Logged     │
                                      └──────────────┘
```

---

## OCI Streaming (Kafka) Configuration

### SASL Configuration Format

```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="<tenancy>/<username>/<stream-pool-ocid>" \
  password="<auth-token>";
```

### Getting Auth Token

1. OCI Console → Identity & Security → Users
2. Select your user
3. Auth Tokens → Generate Token
4. Copy and save (shown only once)

### Stream Pool OCID Format

```
ocid1.streampool.oc1.<region>.<unique-id>
```

---

## Monitoring

### Health Endpoints

```bash
# All services expose /actuator/health
curl http://service:port/actuator/health

# Metrics
curl http://service:port/actuator/metrics
```

### OCI Monitoring

- Enable metrics for all compute instances
- Create alarms for:
  - CPU > 80%
  - Memory > 80%
  - Error rate > 1%
  - Response time > 2s

### Logging

- All services use SLF4J logging
- Logs go to stdout for container collection
- Enable OCI Logging Analytics for centralized logs

---

## Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   - Verify bootstrap servers URL
   - Check SASL credentials
   - Ensure Stream Pool is active

2. **Database Connection Failed**
   - Verify database URL format
   - Check SSL mode (require for OCI)
   - Verify credentials

3. **Redis Connection Failed**
   - Verify Redis endpoint
   - Check SSL enabled (true for OCI)
   - Verify password

4. **Services Can't Communicate**
   - Check service URLs in environment
   - Verify network/security rules
   - Check Kubernetes services

### Debug Commands

```bash
# Check pod logs (OKE)
kubectl logs -f deployment/auth-service -n rrbank

# Check pod status
kubectl get pods -n rrbank

# Describe pod for events
kubectl describe pod <pod-name> -n rrbank

# Check service endpoints
kubectl get endpoints -n rrbank
```

---

## Cost Estimation (Monthly)

| Resource | Specification | Estimated Cost |
|----------|--------------|----------------|
| OKE Cluster | 3 nodes, E4.Flex | $150-200 |
| PostgreSQL | Basic, 2 OCPU | $50-100 |
| Redis | 2GB | $30-50 |
| Streaming | 3 partitions | $20-30 |
| Load Balancer | 100Mbps | $20-30 |
| Object Storage | 10GB | $5-10 |
| **Total** | | **$275-420/mo** |

*Always Free Tier available for some resources*

---

## Security Checklist

- [ ] Use OCI Vault for secrets
- [ ] Enable encryption at rest for database
- [ ] Enable SSL for all connections
- [ ] Configure VCN security lists
- [ ] Set up IAM policies
- [ ] Enable audit logging
- [ ] Configure CORS properly
- [ ] Use strong JWT secret

---

## Files Reference

```
RR-Bank/
├── docker-compose.oracle.yml    # Docker Compose for OCI
├── .env.oracle.example          # Environment template
├── ORACLE_DEPLOYMENT_GUIDE.md   # Detailed guide
├── database/
│   └── oracle-schema.sql        # Database schema
├── kubernetes/
│   ├── 00-namespace-secrets.yaml
│   ├── 01-auth-service.yaml
│   ├── 02-ledger-service.yaml
│   └── 09-api-gateway.yaml
└── scripts/
    ├── build-all.sh             # Build Docker images
    ├── push-to-ocir.sh          # Push to OCIR
    └── deploy-to-oke.sh         # Deploy to OKE
```
