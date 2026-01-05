# RR-Bank Production Deployment Guide

This guide covers deploying the RR-Bank application with:
- **Backend (API Gateway + Services)**: Railway
- **Database**: Supabase (PostgreSQL)
- **Frontend**: Vercel

> **Note on Kafka**: The project has Kafka infrastructure prepared but not actively used.
> Kafka auto-configuration has been disabled. You do NOT need to deploy Kafka/Zookeeper.

## Architecture Overview

```
┌─────────────┐      ┌─────────────────┐      ┌─────────────┐
│   Vercel    │──────│     Railway     │──────│  Supabase   │
│  (Frontend) │      │  (API Gateway)  │      │ (PostgreSQL)│
└─────────────┘      └─────────────────┘      └─────────────┘
```

## Prerequisites

1. Railway account (https://railway.app)
2. Vercel account (https://vercel.com)
3. Supabase account (https://supabase.com)
4. GitHub repository with the RR-Bank code

---

## Step 1: Supabase Database Setup

### 1.1 Create a Supabase Project

1. Go to https://supabase.com and create a new project
2. Note down the following credentials from Project Settings > Database:
   - **Host**: `db.xxxxx.supabase.co`
   - **Database name**: `postgres`
   - **Port**: `5432` (or `6543` for connection pooling)
   - **User**: `postgres`
   - **Password**: Your database password

### 1.2 Create Required Databases

Run these SQL commands in the Supabase SQL Editor:

```sql
-- Create schemas for each service (instead of separate databases)
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS customer_service;
CREATE SCHEMA IF NOT EXISTS account_service;
CREATE SCHEMA IF NOT EXISTS transaction_service;
CREATE SCHEMA IF NOT EXISTS ledger_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS fraud_service;
CREATE SCHEMA IF NOT EXISTS audit_service;
```

### 1.3 Connection String Format

For Railway services, use this connection string format:
```
jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?currentSchema=auth_service
```

---

## Step 2: Railway Backend Deployment

### 2.1 Create Railway Project

1. Go to https://railway.app and create a new project
2. Choose "Deploy from GitHub repo"
3. Connect your RR-Bank repository

### 2.2 Deploy API Gateway

1. In Railway, click "New Service" > "GitHub Repo"
2. Select your repo and set:
   - **Root Directory**: `api-gateway`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/*.jar`

3. Add Environment Variables:
```env
# Server
PORT=8080
SPRING_PROFILES_ACTIVE=production

# JWT
JWT_SECRET=your-production-jwt-secret-minimum-32-characters

# Service URLs (will be internal Railway URLs after deploying services)
AUTH_SERVICE_URL=http://auth-service.railway.internal:8081
CUSTOMER_SERVICE_URL=http://customer-service.railway.internal:8082
ACCOUNT_SERVICE_URL=http://account-service.railway.internal:8083
TRANSACTION_SERVICE_URL=http://transaction-service.railway.internal:8084
LEDGER_SERVICE_URL=http://ledger-service.railway.internal:8085
NOTIFICATION_SERVICE_URL=http://notification-service.railway.internal:8086
FRAUD_SERVICE_URL=http://fraud-service.railway.internal:8087
AUDIT_SERVICE_URL=http://audit-service.railway.internal:8088

# CORS (replace with your Vercel frontend URL)
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app,http://localhost:3000,http://localhost:5173
```

### 2.3 Deploy Each Microservice

Repeat for each service (auth-service, customer-service, account-service, transaction-service, ledger-service, notification-service, fraud-service, audit-service):

1. Click "New Service" > "GitHub Repo"
2. Set Root Directory to `services/{service-name}`
3. Add Environment Variables:

**Example for auth-service:**
```env
PORT=8081
SPRING_PROFILES_ACTIVE=production

# Supabase Database
SPRING_DATASOURCE_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?currentSchema=auth_service
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-supabase-password

# Redis (use Railway Redis or external provider)
SPRING_REDIS_HOST=redis.railway.internal
SPRING_REDIS_PORT=6379

# JWT
JWT_SECRET=your-production-jwt-secret-minimum-32-characters
```

### 2.4 Add Redis Service

1. In Railway, click "New Service" > "Database" > "Redis"
2. Note the internal URL for use in service configurations

### 2.5 Configure Service Discovery

Railway supports internal networking. Update service URLs to use `.railway.internal` format:
- `http://auth-service.railway.internal:8081`
- `http://account-service.railway.internal:8083`
- etc.

### 2.6 Generate Public URL

1. Select the API Gateway service
2. Go to Settings > Networking
3. Click "Generate Domain" to get a public URL like `api-gateway-xxx.railway.app`
4. Note this URL for frontend configuration

---

## Step 3: Vercel Frontend Deployment

### 3.1 Deploy to Vercel

1. Go to https://vercel.com and import your GitHub repository
2. Set Root Directory to `frontend`
3. Vercel should auto-detect Vite

### 3.2 Configure Environment Variables

In Vercel Project Settings > Environment Variables, add:

```env
VITE_API_URL=https://api-gateway-xxx.railway.app/api
VITE_ENV=production
```

### 3.3 Deploy

1. Click "Deploy"
2. Note your Vercel deployment URL (e.g., `your-app.vercel.app`)

### 3.4 Update CORS

Go back to Railway and update the API Gateway's `CORS_ALLOWED_ORIGINS`:
```
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

---

## Step 4: Post-Deployment Configuration

### 4.1 Verify Health Checks

Check each service's health endpoint:
```
https://api-gateway-xxx.railway.app/actuator/health
```

### 4.2 Test Authentication

1. Open your Vercel frontend
2. Register a new user
3. Login with the new user
4. Create an account
5. Make a deposit
6. Make a transfer

### 4.3 Monitor Logs

Use Railway's log viewer to monitor service logs for any errors.

---

## Environment Variables Reference

### API Gateway
| Variable | Description | Example |
|----------|-------------|---------|
| PORT | Server port | 8080 |
| JWT_SECRET | JWT signing key (32+ chars) | your-secret-key |
| CORS_ALLOWED_ORIGINS | Allowed frontend origins | https://app.vercel.app |
| AUTH_SERVICE_URL | Auth service URL | http://auth-service:8081 |

### Microservices
| Variable | Description | Example |
|----------|-------------|---------|
| PORT | Server port | 8081-8088 |
| SPRING_DATASOURCE_URL | Supabase connection | jdbc:postgresql://... |
| SPRING_DATASOURCE_USERNAME | DB username | postgres |
| SPRING_DATASOURCE_PASSWORD | DB password | your-password |
| SPRING_REDIS_HOST | Redis host | redis.railway.internal |
| JWT_SECRET | JWT key (same as gateway) | your-secret-key |

### Frontend
| Variable | Description | Example |
|----------|-------------|---------|
| VITE_API_URL | Backend API URL | https://api.railway.app/api |
| VITE_ENV | Environment name | production |

---

## Troubleshooting

### CORS Errors
- Ensure `CORS_ALLOWED_ORIGINS` includes your Vercel URL
- Check browser console for the exact origin being rejected

### Database Connection Issues
- Verify Supabase credentials
- Check if connection pooling port (6543) is needed
- Ensure schema exists in Supabase

### 401 Unauthorized
- Verify JWT_SECRET is identical across all services
- Check token expiration settings

### Service Communication Failures
- Ensure services are using Railway internal URLs
- Check service health endpoints
- Verify network configuration in Railway

---

## Security Checklist

- [ ] Change default JWT_SECRET to a strong random string
- [ ] Enable SSL/TLS on all connections
- [ ] Configure rate limiting
- [ ] Set up monitoring and alerting
- [ ] Enable database backups in Supabase
- [ ] Review and restrict CORS origins
- [ ] Use environment-specific secrets
- [ ] Enable audit logging

---

## Scaling Considerations

1. **Horizontal Scaling**: Railway supports auto-scaling
2. **Database**: Supabase handles connection pooling
3. **Caching**: Consider adding Redis caching for frequent queries
4. **CDN**: Vercel automatically provides CDN for frontend

---

## Support

For issues with deployment:
1. Check Railway/Vercel/Supabase status pages
2. Review service logs
3. Verify environment variables
4. Test individual services with health endpoints
