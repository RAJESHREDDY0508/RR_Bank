# RR-Bank Production Deployment Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
        ┌───────────────────┐           ┌───────────────────┐
        │      VERCEL       │           │     RAILWAY       │
        │    (Frontend)     │           │   (API Gateway)   │
        │                   │           │    Port 8080      │
        │  React + Vite     │──────────▶│                   │
        │                   │   HTTPS   │                   │
        └───────────────────┘           └─────────┬─────────┘
                                                  │
                                    Internal Railway Network
                    ┌─────────────────────────────┼─────────────────────────────┐
                    │                             │                             │
        ┌───────────┴───────────┐   ┌─────────────┴─────────────┐   ┌──────────┴──────────┐
        │                       │   │                           │   │                      │
        ▼                       ▼   ▼                           ▼   ▼                      ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ auth-service │  │account-service│  │transaction-  │  │ ledger-     │  │ fraud-       │
│   :8081      │  │   :8083      │  │ service:8084 │  │ service:8085│  │ service:8087 │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │                 │                 │
        └─────────────────┴─────────────────┴─────────────────┴─────────────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    │                       │                       │
                    ▼                       ▼                       ▼
        ┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
        │     SUPABASE      │   │      UPSTASH      │   │     RAILWAY       │
        │   (PostgreSQL)    │   │      (Redis)      │   │  Other Services   │
        │                   │   │                   │   │  customer:8082    │
        │   All databases   │   │   Session cache   │   │  notification:8086│
        │                   │   │   Rate limiting   │   │  audit:8088       │
        └───────────────────┘   └───────────────────┘   └───────────────────┘
```

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Supabase Setup (Database)](#1-supabase-setup-database)
3. [Upstash Setup (Redis)](#2-upstash-setup-redis)
4. [Railway Setup (Backend Services)](#3-railway-setup-backend-services)
5. [Vercel Setup (Frontend)](#4-vercel-setup-frontend)
6. [Environment Variables Reference](#5-environment-variables-reference)
7. [Post-Deployment Verification](#6-post-deployment-verification)
8. [Troubleshooting](#7-troubleshooting)

---

## Prerequisites

Before starting, ensure you have:
- GitHub account with the RR-Bank repository pushed
- Supabase account (https://supabase.com)
- Upstash account (https://upstash.com)
- Railway account (https://railway.app)
- Vercel account (https://vercel.com)

---

## 1. Supabase Setup (Database)

### Step 1.1: Create Supabase Project

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Click **"New Project"**
3. Fill in:
   - **Name**: `rrbank-production`
   - **Database Password**: Generate a strong password (SAVE THIS!)
   - **Region**: Choose closest to your users
4. Click **"Create new project"**
5. Wait for project to initialize (~2 minutes)

### Step 1.2: Get Connection Details

1. Go to **Settings** → **Database**
2. Find **Connection string** section
3. Copy the **URI** (Connection pooling recommended for production):
   ```
   postgresql://postgres.[project-ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres
   ```
4. Note these values:
   - **Host**: `aws-0-[region].pooler.supabase.com`
   - **Port**: `6543` (pooler) or `5432` (direct)
   - **Database**: `postgres`
   - **User**: `postgres.[project-ref]`
   - **Password**: Your database password

### Step 1.3: Create Database Schema

1. Go to **SQL Editor** in Supabase Dashboard
2. Click **"New Query"**
3. Copy and run the following SQL:

```sql
-- =====================================================
-- RR-BANK DATABASE SCHEMA
-- Run this in Supabase SQL Editor
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- AUTH SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS login_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_type VARCHAR(50),
    location VARCHAR(255),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- CUSTOMER SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    date_of_birth DATE,
    kyc_verified BOOLEAN DEFAULT FALSE,
    kyc_verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ACCOUNT SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    customer_id UUID,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    overdraft_limit DECIMAL(19,4) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_account_user ON accounts(user_id);
CREATE INDEX idx_account_status ON accounts(status);

-- =====================================================
-- TRANSACTION SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_reference VARCHAR(50) UNIQUE NOT NULL,
    from_account_id UUID,
    to_account_id UUID,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'PENDING',
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(100) UNIQUE,
    initiated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_txn_reference ON transactions(transaction_reference);
CREATE INDEX idx_txn_from_account ON transactions(from_account_id);
CREATE INDEX idx_txn_to_account ON transactions(to_account_id);
CREATE INDEX idx_txn_status ON transactions(status);
CREATE INDEX idx_txn_created ON transactions(created_at);

-- =====================================================
-- LEDGER SERVICE TABLES (Source of Truth)
-- =====================================================

CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    transaction_id UUID,
    entry_type VARCHAR(10) NOT NULL, -- CREDIT or DEBIT
    amount DECIMAL(19,4) NOT NULL,
    running_balance DECIMAL(19,4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_created ON ledger_entries(created_at);

CREATE TABLE IF NOT EXISTS balance_cache (
    account_id UUID PRIMARY KEY,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- FRAUD SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS fraud_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID,
    account_id UUID,
    user_id UUID,
    event_type VARCHAR(50) NOT NULL,
    risk_score INTEGER,
    decision VARCHAR(20),
    reason VARCHAR(500),
    reviewed BOOLEAN DEFAULT FALSE,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- NOTIFICATION SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_user ON notifications(user_id);

-- =====================================================
-- AUDIT SERVICE TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    user_id UUID,
    action VARCHAR(50),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- =====================================================
-- GRANT PERMISSIONS (if using separate users)
-- =====================================================
-- By default, the postgres user has full access
-- Add any additional grants here if needed
```

4. Click **"Run"** to execute

### Step 1.4: Verify Tables Created

1. Go to **Table Editor** in Supabase
2. You should see all tables created:
   - users, refresh_tokens, password_reset_tokens, login_history
   - customers
   - accounts
   - transactions
   - ledger_entries, balance_cache
   - fraud_events
   - notifications
   - audit_logs

---

## 2. Upstash Setup (Redis)

### Step 2.1: Create Upstash Redis Database

1. Go to [Upstash Console](https://console.upstash.com)
2. Click **"Create Database"**
3. Configure:
   - **Name**: `rrbank-redis`
   - **Type**: Regional
   - **Region**: Choose same region as Railway/Supabase
   - **TLS**: Enabled (recommended)
4. Click **"Create"**

### Step 2.2: Get Redis Connection Details

After creation, you'll see the dashboard with connection details:

1. **REST API** (copy these):
   - `UPSTASH_REDIS_REST_URL`
   - `UPSTASH_REDIS_REST_TOKEN`

2. **Redis URL** (for Spring Boot):
   ```
   rediss://default:[password]@[endpoint]:6379
   ```
   
   Or individual values:
   - **Host**: `[your-endpoint].upstash.io`
   - **Port**: `6379`
   - **Password**: Your password (shown in dashboard)

3. Note these values for Railway:
   ```
   SPRING_REDIS_HOST=your-endpoint.upstash.io
   SPRING_REDIS_PORT=6379
   SPRING_REDIS_PASSWORD=your-password
   SPRING_REDIS_SSL=true
   ```

---

## 3. Railway Setup (Backend Services)

### Step 3.1: Create Railway Project

1. Go to [Railway Dashboard](https://railway.app/dashboard)
2. Click **"New Project"**
3. Select **"Empty Project"**
4. Name it `rrbank-backend`

### Step 3.2: Connect GitHub Repository

1. In your Railway project, click **"New"** → **"GitHub Repo"**
2. Select your RR-Bank repository
3. Railway will detect it's a monorepo

### Step 3.3: Deploy API Gateway

1. Click **"New"** → **"GitHub Repo"** → Select your repo
2. Configure the service:
   - **Name**: `api-gateway`
   - **Root Directory**: `api-gateway`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/api-gateway-1.0.0.jar`

3. Add Environment Variables (Settings → Variables):

```env
# Server
PORT=8080
SERVER_PORT=8080

# JWT
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long-for-security

# CORS - Update with your Vercel URL after deployment
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app,http://localhost:3000

# Service URLs (Railway internal networking)
AUTH_SERVICE_URL=http://auth-service.railway.internal:8081
CUSTOMER_SERVICE_URL=http://customer-service.railway.internal:8082
ACCOUNT_SERVICE_URL=http://account-service.railway.internal:8083
TRANSACTION_SERVICE_URL=http://transaction-service.railway.internal:8084
LEDGER_SERVICE_URL=http://ledger-service.railway.internal:8085
NOTIFICATION_SERVICE_URL=http://notification-service.railway.internal:8086
FRAUD_SERVICE_URL=http://fraud-service.railway.internal:8087
AUDIT_SERVICE_URL=http://audit-service.railway.internal:8088

# Logging
LOG_LEVEL=INFO
```

4. In **Settings** → **Networking**:
   - Generate a public domain (e.g., `rrbank-api.up.railway.app`)
   - Note this URL for Vercel configuration

### Step 3.4: Deploy Auth Service

1. Click **"New"** → **"GitHub Repo"** → Select your repo
2. Configure:
   - **Name**: `auth-service`
   - **Root Directory**: `services/auth-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/auth-service-1.0.0.jar`

3. Environment Variables:

```env
# Server
PORT=8081
SERVER_PORT=8081

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (Upstash)
SPRING_REDIS_HOST=your-endpoint.upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-upstash-password
SPRING_DATA_REDIS_SSL_ENABLED=true

# JWT
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long-for-security
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

4. In **Settings** → **Networking**:
   - Set **Private Networking** alias: `auth-service`

### Step 3.5: Deploy Account Service

1. Click **"New"** → **"GitHub Repo"** → Select your repo
2. Configure:
   - **Name**: `account-service`
   - **Root Directory**: `services/account-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/account-service-1.0.0.jar`

3. Environment Variables:

```env
# Server
PORT=8083
SERVER_PORT=8083

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (Upstash)
SPRING_REDIS_HOST=your-endpoint.upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-upstash-password
SPRING_DATA_REDIS_SSL_ENABLED=true

# Service URLs
SERVICES_LEDGER_URL=http://ledger-service.railway.internal:8085

# JWT
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long-for-security

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production

# Flyway (disable if schema already created)
SPRING_FLYWAY_ENABLED=false
```

4. Private Networking alias: `account-service`

### Step 3.6: Deploy Transaction Service

1. Configure:
   - **Name**: `transaction-service`
   - **Root Directory**: `services/transaction-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/transaction-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8084
SERVER_PORT=8084

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (Upstash)
SPRING_REDIS_HOST=your-endpoint.upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-upstash-password
SPRING_DATA_REDIS_SSL_ENABLED=true

# Service URLs
SERVICES_LEDGER_URL=http://ledger-service.railway.internal:8085
SERVICES_FRAUD_URL=http://fraud-service.railway.internal:8087
SERVICES_ACCOUNT_URL=http://account-service.railway.internal:8083

# JWT
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long-for-security

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `transaction-service`

### Step 3.7: Deploy Ledger Service

1. Configure:
   - **Name**: `ledger-service`
   - **Root Directory**: `services/ledger-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/ledger-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8085
SERVER_PORT=8085

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (Upstash)
SPRING_REDIS_HOST=your-endpoint.upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-upstash-password
SPRING_DATA_REDIS_SSL_ENABLED=true

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `ledger-service`

### Step 3.8: Deploy Customer Service

1. Configure:
   - **Name**: `customer-service`
   - **Root Directory**: `services/customer-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/customer-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8082
SERVER_PORT=8082

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `customer-service`

### Step 3.9: Deploy Fraud Service

1. Configure:
   - **Name**: `fraud-service`
   - **Root Directory**: `services/fraud-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/fraud-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8087
SERVER_PORT=8087

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (Upstash) - Required for rate limiting
SPRING_REDIS_HOST=your-endpoint.upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-upstash-password
SPRING_DATA_REDIS_SSL_ENABLED=true

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `fraud-service`

### Step 3.10: Deploy Notification Service

1. Configure:
   - **Name**: `notification-service`
   - **Root Directory**: `services/notification-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/notification-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8086
SERVER_PORT=8086

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `notification-service`

### Step 3.11: Deploy Audit Service

1. Configure:
   - **Name**: `audit-service`
   - **Root Directory**: `services/audit-service`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/audit-service-1.0.0.jar`

2. Environment Variables:

```env
# Server
PORT=8088
SERVER_PORT=8088

# Database (Supabase)
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Logging
LOG_LEVEL=INFO

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

3. Private Networking alias: `audit-service`

### Step 3.12: Verify Railway Deployment

1. Check all services are **Active** in Railway dashboard
2. Check logs for each service (click service → **Logs**)
3. Test API Gateway health:
   ```
   curl https://your-api-gateway.up.railway.app/actuator/health
   ```

---

## 4. Vercel Setup (Frontend)

### Step 4.1: Prepare Frontend for Deployment

1. Ensure your `frontend/package.json` has build script:
   ```json
   {
     "scripts": {
       "dev": "vite",
       "build": "vite build",
       "preview": "vite preview"
     }
   }
   ```

### Step 4.2: Create Vercel Project

1. Go to [Vercel Dashboard](https://vercel.com/dashboard)
2. Click **"Add New"** → **"Project"**
3. Import your GitHub repository
4. Configure:
   - **Framework Preset**: Vite
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

### Step 4.3: Configure Environment Variables

In Vercel project settings → **Environment Variables**:

```env
VITE_API_URL=https://your-api-gateway.up.railway.app/api
```

Replace `your-api-gateway.up.railway.app` with your actual Railway API Gateway URL.

### Step 4.4: Deploy

1. Click **"Deploy"**
2. Wait for build to complete
3. Note your Vercel URL (e.g., `rrbank.vercel.app`)

### Step 4.5: Update CORS in Railway

1. Go back to Railway → API Gateway service
2. Update `CORS_ALLOWED_ORIGINS` with your Vercel URL:
   ```
   CORS_ALLOWED_ORIGINS=https://rrbank.vercel.app,https://your-custom-domain.com
   ```
3. Redeploy API Gateway

---

## 5. Environment Variables Reference

### Complete Environment Variables List

#### API Gateway
```env
PORT=8080
SERVER_PORT=8080
JWT_SECRET=your-jwt-secret-minimum-32-chars
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
AUTH_SERVICE_URL=http://auth-service.railway.internal:8081
CUSTOMER_SERVICE_URL=http://customer-service.railway.internal:8082
ACCOUNT_SERVICE_URL=http://account-service.railway.internal:8083
TRANSACTION_SERVICE_URL=http://transaction-service.railway.internal:8084
LEDGER_SERVICE_URL=http://ledger-service.railway.internal:8085
NOTIFICATION_SERVICE_URL=http://notification-service.railway.internal:8086
FRAUD_SERVICE_URL=http://fraud-service.railway.internal:8087
AUDIT_SERVICE_URL=http://audit-service.railway.internal:8088
LOG_LEVEL=INFO
```

#### All Backend Services (Common)
```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://[host]:6543/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.[project-ref]
SPRING_DATASOURCE_PASSWORD=[password]

# Redis (for services that need it)
SPRING_REDIS_HOST=[endpoint].upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=[password]
SPRING_DATA_REDIS_SSL_ENABLED=true

# JWT (for services that validate tokens)
JWT_SECRET=your-jwt-secret-minimum-32-chars

# Spring
SPRING_PROFILES_ACTIVE=production
LOG_LEVEL=INFO
```

#### Frontend (Vercel)
```env
VITE_API_URL=https://your-api-gateway.up.railway.app/api
```

---

## 6. Post-Deployment Verification

### Step 6.1: Test Backend Health

```bash
# Test API Gateway
curl https://your-api-gateway.up.railway.app/actuator/health

# Expected response:
# {"status":"UP"}
```

### Step 6.2: Test Registration

```bash
curl -X POST https://your-api-gateway.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Step 6.3: Test Login

```bash
curl -X POST https://your-api-gateway.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "Password123!"
  }'
```

### Step 6.4: Test Frontend

1. Open your Vercel URL in browser
2. Try to register a new account
3. Log in with the account
4. Create a bank account
5. Make a deposit
6. View transactions

### Step 6.5: Verify Data in Supabase

1. Go to Supabase Dashboard → Table Editor
2. Check `users` table for new registrations
3. Check `accounts` table for created accounts
4. Check `ledger_entries` for transactions

---

## 7. Troubleshooting

### Common Issues

#### 1. CORS Errors
**Symptom**: Browser console shows CORS errors
**Solution**: 
- Ensure `CORS_ALLOWED_ORIGINS` in API Gateway includes your Vercel URL
- Check there are no trailing slashes in URLs
- Redeploy API Gateway after changing CORS settings

#### 2. Database Connection Failed
**Symptom**: Services crash with connection errors
**Solution**:
- Verify Supabase connection string format
- Ensure `?sslmode=require` is in the URL
- Check Supabase is not paused (free tier pauses after inactivity)
- Use pooler port (6543) instead of direct port (5432)

#### 3. Redis Connection Failed
**Symptom**: Auth service can't start, session errors
**Solution**:
- Verify Upstash credentials
- Ensure `SPRING_DATA_REDIS_SSL_ENABLED=true`
- Check Redis URL format

#### 4. Services Can't Communicate
**Symptom**: 500 errors when making transactions
**Solution**:
- Verify Railway private networking aliases match service URLs
- Use `.railway.internal` suffix for internal URLs
- Check all services are running

#### 5. JWT Errors
**Symptom**: 401 Unauthorized on all requests
**Solution**:
- Ensure `JWT_SECRET` is identical across all services
- JWT secret must be at least 32 characters
- Check token expiration settings

#### 6. Build Failures on Railway
**Symptom**: Deployment fails during build
**Solution**:
- Check build logs for specific errors
- Ensure Maven wrapper is included (`mvnw`, `mvnw.cmd`)
- Verify Java version compatibility (Java 17)

### Viewing Logs

#### Railway Logs
1. Go to Railway Dashboard
2. Click on the service
3. Click **"Logs"** tab
4. Use filter to find errors

#### Vercel Logs
1. Go to Vercel Dashboard
2. Click on your project
3. Go to **"Functions"** tab (for API routes)
4. Or check **"Deployments"** → specific deployment → **"Logs"**

### Health Check URLs

Test each service individually:

```bash
# API Gateway
curl https://your-api-gateway.up.railway.app/actuator/health

# Through API Gateway (if routes are configured)
curl https://your-api-gateway.up.railway.app/api/auth/health
curl https://your-api-gateway.up.railway.app/api/accounts/health
curl https://your-api-gateway.up.railway.app/api/transactions/health
curl https://your-api-gateway.up.railway.app/api/ledger/health
```

---

## Quick Reference Card

### URLs After Deployment

| Service | URL |
|---------|-----|
| Frontend | https://your-app.vercel.app |
| API Gateway | https://your-api.up.railway.app |
| Supabase | https://app.supabase.com/project/[id] |
| Upstash | https://console.upstash.com |
| Railway | https://railway.app/project/[id] |

### Port Assignments

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Auth Service | 8081 |
| Customer Service | 8082 |
| Account Service | 8083 |
| Transaction Service | 8084 |
| Ledger Service | 8085 |
| Notification Service | 8086 |
| Fraud Service | 8087 |
| Audit Service | 8088 |

### Important Files

| File | Purpose |
|------|---------|
| `api-gateway/src/main/resources/application.yml` | Gateway routing config |
| `frontend/.env` | Frontend environment |
| `services/*/src/main/resources/application.yml` | Service configs |

---

## Cost Estimation (Monthly)

| Service | Free Tier | Paid Estimate |
|---------|-----------|---------------|
| Vercel | Yes (Hobby) | $20/mo (Pro) |
| Railway | $5 credit | ~$20-50/mo |
| Supabase | Yes (500MB) | $25/mo (Pro) |
| Upstash | Yes (10K/day) | $10/mo (Pay-as-go) |
| **Total** | **~$0-5** | **~$75-100** |

---

## Next Steps

1. Set up custom domain (optional)
2. Configure SSL certificates (automatic on Vercel/Railway)
3. Set up monitoring (Railway metrics, Supabase dashboard)
4. Configure backup strategy for Supabase
5. Set up CI/CD pipeline for automated deployments

---

**Deployment Complete! 🚀**

Your RR-Bank application is now live and accessible at your Vercel URL.
