# RR-Bank Oracle Cloud Deployment Guide

## Architecture Overview for Oracle Cloud

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           ORACLE CLOUD INFRASTRUCTURE (OCI)                          │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌─────────────────┐    ┌─────────────────────────────────────────────────────┐     │
│  │   OCI Load      │    │              OKE (Kubernetes) / VM Instances         │     │
│  │   Balancer      │───▶│  ┌─────────────────────────────────────────────┐    │     │
│  │   (Public IP)   │    │  │              API Gateway (8080)              │    │     │
│  └─────────────────┘    │  └─────────────────────────────────────────────┘    │     │
│                         │                        │                             │     │
│                         │     ┌──────────────────┼──────────────────┐         │     │
│                         │     │                  │                  │         │     │
│                         │     ▼                  ▼                  ▼         │     │
│                         │  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  │     │
│                         │  │ Auth │  │Acct  │  │ Txn  │  │Ledger│  │Fraud │  │     │
│                         │  │ 8081 │  │ 8083 │  │ 8084 │  │ 8085 │  │ 8087 │  │     │
│                         │  └──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘  │     │
│                         │     │         │         │         │         │       │     │
│                         │     └─────────┴────┬────┴─────────┴─────────┘       │     │
│                         │                    │                                 │     │
│                         │     ┌──────────────┴──────────────┐                 │     │
│                         │     │     OCI Streaming (Kafka)    │                 │     │
│                         │     │     - user-events            │                 │     │
│                         │     │     - transaction-events     │                 │     │
│                         │     │     - audit-events           │                 │     │
│                         │     │     - notification-events    │                 │     │
│                         │     └──────────────────────────────┘                 │     │
│                         └─────────────────────────────────────────────────────┘     │
│                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                        MANAGED SERVICES                                      │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │    │
│  │  │ OCI Autonomous  │  │   OCI Cache     │  │  OCI Object     │              │    │
│  │  │   Database      │  │  with Redis     │  │    Storage      │              │    │
│  │  │  (PostgreSQL)   │  │                 │  │   (Backups)     │              │    │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘              │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                        FRONTEND (Static)                                     │    │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │    │
│  │  │  OCI Object Storage (Static Website) or OCI Container Instance       │    │    │
│  │  │  React Frontend served via CDN                                       │    │    │
│  │  └─────────────────────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [OCI Resources Setup](#2-oci-resources-setup)
3. [Database Setup](#3-database-setup)
4. [OCI Streaming (Kafka) Setup](#4-oci-streaming-kafka-setup)
5. [OCI Cache (Redis) Setup](#5-oci-cache-redis-setup)
6. [Container Registry Setup](#6-container-registry-setup)
7. [Kubernetes Deployment (OKE)](#7-kubernetes-deployment-oke)
8. [Alternative: VM-based Deployment](#8-alternative-vm-based-deployment)
9. [Environment Variables](#9-environment-variables)
10. [Monitoring & Logging](#10-monitoring--logging)

---

## 1. Prerequisites

### OCI Account Setup
- Active Oracle Cloud account
- Compartment created for RR-Bank
- User with appropriate IAM policies
- OCI CLI installed and configured

### Required OCI Services
- OCI Autonomous Database (PostgreSQL) or DB System
- OCI Streaming (Kafka-compatible)
- OCI Cache with Redis
- OCI Container Registry (OCIR)
- OCI Kubernetes Engine (OKE) or Compute Instances
- OCI Load Balancer
- OCI Object Storage (for frontend)

---

## 2. OCI Resources Setup

### 2.1 Create Compartment
```bash
oci iam compartment create \
  --compartment-id <parent-compartment-id> \
  --name "rrbank-prod" \
  --description "RR Bank Production Environment"
```

### 2.2 Create VCN (Virtual Cloud Network)
```bash
# Create VCN
oci network vcn create \
  --compartment-id <compartment-id> \
  --cidr-blocks '["10.0.0.0/16"]' \
  --display-name "rrbank-vcn"

# Create subnets (public and private)
# Public subnet for Load Balancer
# Private subnets for services, database, etc.
```

---

## 3. Database Setup

### Option A: OCI Autonomous Database (Recommended)
```bash
# Create Autonomous Database
oci db autonomous-database create \
  --compartment-id <compartment-id> \
  --db-name "rrbank" \
  --display-name "rrbank-db" \
  --db-workload "OLTP" \
  --is-free-tier true \
  --admin-password "<strong-password>" \
  --cpu-core-count 1 \
  --data-storage-size-in-tbs 1
```

### Option B: OCI DB System with PostgreSQL
```bash
# Create PostgreSQL DB System
oci postgresql db-system create \
  --compartment-id <compartment-id> \
  --display-name "rrbank-postgres" \
  --db-version "15" \
  --shape-name "PostgreSQL.VM.Standard.E4.Flex.2.32GB" \
  --instance-count 1 \
  --admin-username "rrbank" \
  --admin-password "<strong-password>"
```

### Database Connection String Format
```
jdbc:postgresql://<db-host>:5432/rrbank?sslmode=require
```

---

## 4. OCI Streaming (Kafka) Setup

### 4.1 Create Stream Pool
```bash
oci streaming admin stream-pool create \
  --compartment-id <compartment-id> \
  --name "rrbank-kafka-pool" \
  --kafka-settings '{"bootstrapServers": null, "autoCreateTopicsEnable": true, "numPartitions": 3}'
```

### 4.2 Create Streams (Topics)
```bash
# User Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "user-events" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>

# Transaction Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "transaction-events" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>

# Audit Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "audit-events" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>

# Notification Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "notification-events" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>

# Ledger Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "ledger-events" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>

# Balance Updated Events
oci streaming admin stream create \
  --compartment-id <compartment-id> \
  --name "balance-updated" \
  --partitions 3 \
  --stream-pool-id <stream-pool-id>
```

### 4.3 Get Kafka Connection Details
```bash
oci streaming admin stream-pool get --stream-pool-id <stream-pool-id>
```

**Connection Details Format:**
```
Bootstrap Servers: cell-1.streaming.<region>.oci.oraclecloud.com:9092
SASL Mechanism: PLAIN
Security Protocol: SASL_SSL
Username: <tenancy>/<username>/<stream-pool-id>
Password: <auth-token>
```

---

## 5. OCI Cache (Redis) Setup

### Create Redis Cluster
```bash
oci redis redis-cluster create \
  --compartment-id <compartment-id> \
  --display-name "rrbank-redis" \
  --node-count 1 \
  --node-memory-in-gbs 2 \
  --software-version "7.0" \
  --subnet-id <private-subnet-id>
```

### Redis Connection Details
```
Host: <redis-cluster-endpoint>
Port: 6379
SSL: true
Password: <redis-password>
```

---

## 6. Container Registry Setup

### 6.1 Create Repository
```bash
# Create repositories for each service
for service in api-gateway auth-service customer-service account-service \
               transaction-service ledger-service notification-service \
               fraud-service audit-service frontend; do
  oci artifacts container repository create \
    --compartment-id <compartment-id> \
    --display-name "rrbank/${service}" \
    --is-public false
done
```

### 6.2 Login to OCIR
```bash
docker login <region-code>.ocir.io -u '<tenancy-namespace>/<username>' -p '<auth-token>'
```

### 6.3 Build and Push Images
```bash
# Set variables
REGION=<region-code>  # e.g., iad, phx, fra
NAMESPACE=<tenancy-namespace>
TAG=latest

# Build and push each service
cd api-gateway
docker build -t ${REGION}.ocir.io/${NAMESPACE}/rrbank/api-gateway:${TAG} .
docker push ${REGION}.ocir.io/${NAMESPACE}/rrbank/api-gateway:${TAG}

# Repeat for all services...
```

---

## 7. Kubernetes Deployment (OKE)

### 7.1 Create OKE Cluster
```bash
oci ce cluster create \
  --compartment-id <compartment-id> \
  --name "rrbank-cluster" \
  --vcn-id <vcn-id> \
  --kubernetes-version "v1.28.2" \
  --service-lb-subnet-ids '["<public-subnet-id>"]' \
  --endpoint-subnet-id <public-subnet-id>
```

### 7.2 Create Node Pool
```bash
oci ce node-pool create \
  --cluster-id <cluster-id> \
  --compartment-id <compartment-id> \
  --name "rrbank-nodes" \
  --kubernetes-version "v1.28.2" \
  --node-shape "VM.Standard.E4.Flex" \
  --node-shape-config '{"ocpus": 2, "memoryInGBs": 16}' \
  --size 3 \
  --placement-configs '[{"availabilityDomain": "<AD>", "subnetId": "<private-subnet-id>"}]'
```

### 7.3 Configure kubectl
```bash
oci ce cluster create-kubeconfig \
  --cluster-id <cluster-id> \
  --file $HOME/.kube/config \
  --region <region> \
  --token-version 2.0.0
```

See `kubernetes/` directory for deployment manifests.

---

## 8. Alternative: VM-based Deployment

### 8.1 Create Compute Instances
```bash
# Create instance for services
oci compute instance launch \
  --compartment-id <compartment-id> \
  --availability-domain <AD> \
  --shape "VM.Standard.E4.Flex" \
  --shape-config '{"ocpus": 4, "memoryInGBs": 32}' \
  --display-name "rrbank-services" \
  --image-id <oracle-linux-image-id> \
  --subnet-id <private-subnet-id> \
  --assign-public-ip false
```

### 8.2 Install Docker on VM
```bash
# SSH into instance
ssh opc@<instance-ip>

# Install Docker
sudo yum install -y docker-engine
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker opc

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 8.3 Deploy using Docker Compose
See `docker-compose.oracle.yml` for Oracle Cloud specific configuration.

---

## 9. Environment Variables

### Common Variables for All Services
```env
# Database (OCI PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/rrbank?sslmode=require
SPRING_DATASOURCE_USERNAME=rrbank
SPRING_DATASOURCE_PASSWORD=<db-password>

# Redis (OCI Cache)
SPRING_REDIS_HOST=<redis-endpoint>
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=<redis-password>
SPRING_DATA_REDIS_SSL_ENABLED=true

# Kafka (OCI Streaming)
SPRING_KAFKA_ENABLED=true
SPRING_KAFKA_BOOTSTRAP_SERVERS=cell-1.streaming.<region>.oci.oraclecloud.com:9092
SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=SASL_SSL
SPRING_KAFKA_PROPERTIES_SASL_MECHANISM=PLAIN
SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="<tenancy>/<username>/<stream-pool-id>" password="<auth-token>";

# JWT
JWT_SECRET=<your-secure-jwt-secret-minimum-32-characters>

# Profile
SPRING_PROFILES_ACTIVE=oracle
```

---

## 10. Monitoring & Logging

### OCI Monitoring
- Enable metrics collection for all services
- Create alarms for CPU, memory, and error rates
- Set up notification channels

### OCI Logging
- Enable logging for OKE/Compute instances
- Stream logs to OCI Logging Analytics
- Create log groups for each service

### Health Checks
All services expose `/actuator/health` endpoint for monitoring.

---

## Quick Start Commands

```bash
# 1. Clone and setup
git clone <repository>
cd RR-Bank

# 2. Configure environment
cp .env.oracle.example .env.oracle
# Edit .env.oracle with your OCI credentials

# 3. Build images
./scripts/build-all.sh

# 4. Push to OCIR
./scripts/push-to-ocir.sh

# 5. Deploy to OKE
kubectl apply -f kubernetes/

# OR Deploy to VM with Docker Compose
docker-compose -f docker-compose.oracle.yml up -d
```
