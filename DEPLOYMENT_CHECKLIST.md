# RR-Bank Deployment Checklist

Use this checklist to track your deployment progress.

## Pre-Deployment

- [ ] GitHub repository is up to date with all code changes
- [ ] All services compile locally (`mvn clean package -DskipTests`)
- [ ] Frontend builds locally (`npm run build`)

---

## 1. Supabase (Database)

- [ ] Create Supabase account/project
- [ ] Note connection details:
  - [ ] Host: `_________________________.pooler.supabase.com`
  - [ ] Port: `6543`
  - [ ] Database: `postgres`
  - [ ] Username: `postgres._________________________`
  - [ ] Password: `_________________________`
- [ ] Run database schema SQL in SQL Editor
- [ ] Verify all tables created in Table Editor

---

## 2. Upstash (Redis)

- [ ] Create Upstash account/database
- [ ] Note connection details:
  - [ ] Host: `________`
  - [ ] Port: `6379`
  - [ ] Password: `_________________________`
- [ ] Enable TLS/SSL

---

## 3. Railway (Backend)

### Project Setup

- [ ] Create Railway project
- [ ] Connect GitHub repository

### Deploy Services (in this order)

#### API Gateway

- [ ] Create service from GitHub (root: `api-gateway`)
- [ ] Set environment variables
- [ ] Generate public domain
- [ ] Note URL: `https://_________________________.up.railway.app`

#### Auth Service (8081)

- [ ] Create service (root: `services/auth-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `auth-service`
- [ ] Verify healthy in logs

#### Customer Service (8082)

- [ ] Create service (root: `services/customer-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `customer-service`
- [ ] Verify healthy in logs

#### Account Service (8083)

- [ ] Create service (root: `services/account-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `account-service`
- [ ] Verify healthy in logs

#### Transaction Service (8084)

- [ ] Create service (root: `services/transaction-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `transaction-service`
- [ ] Verify healthy in logs

#### Ledger Service (8085)

- [ ] Create service (root: `services/ledger-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `ledger-service`
- [ ] Verify healthy in logs

#### Notification Service (8086)

- [ ] Create service (root: `services/notification-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `notification-service`
- [ ] Verify healthy in logs

#### Fraud Service (8087)

- [ ] Create service (root: `services/fraud-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `fraud-service`
- [ ] Verify healthy in logs

#### Audit Service (8088)

- [ ] Create service (root: `services/audit-service`)
- [ ] Set environment variables
- [ ] Set private network alias: `audit-service`
- [ ] Verify healthy in logs

### Verify Railway

- [ ] All 9 services showing "Active"
- [ ] API Gateway health check returns `{"status":"UP"}`

---

## 4. Vercel (Frontend)

- [ ] Create Vercel account/project
- [ ] Import GitHub repository
- [ ] Set root directory to `frontend`
- [ ] Set environment variable:
  - [ ] `VITE_API_URL=https://[your-railway-api].up.railway.app/api`
- [ ] Deploy
- [ ] Note URL: `https://_________________________.vercel.app`

---

## 5. Post-Deployment

- [ ] Update Railway API Gateway `CORS_ALLOWED_ORIGINS` with Vercel URL
- [ ] Redeploy API Gateway
- [ ] Test registration on frontend
- [ ] Test login on frontend
- [ ] Test create account
- [ ] Test deposit
- [ ] Test transfer
- [ ] Test transactions history
- [ ] Verify data appears in Supabase tables

---

## Environment Variables Quick Copy

### Supabase Connection String

```
jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
```

### Upstash Redis

```
SPRING_REDIS_HOST=[endpoint].upstash.io
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=[password]
SPRING_DATA_REDIS_SSL_ENABLED=true
```

### JWT Secret (use same across all services)

```
JWT_SECRET=[your-secret-minimum-32-characters]
```

---

## Troubleshooting Quick Reference

| Issue                      | Check                                |
| -------------------------- | ------------------------------------ |
| CORS errors                | API Gateway CORS_ALLOWED_ORIGINS     |
| 401 Unauthorized           | JWT_SECRET same across services      |
| Database connection        | Supabase URL format, sslmode=require |
| Redis connection           | Upstash SSL enabled                  |
| Services can't communicate | Railway private network aliases      |
| Build fails                | Maven wrapper present, Java 17       |

---

## Final URLs

| Component | URL                                                  |
| --------- | ---------------------------------------------------- |
| Frontend  | `https://_________________________.vercel.app`     |
| API       | `https://_________________________.up.railway.app` |
| Database  | Supabase Dashboard                                   |
| Redis     | Upstash Console                                      |

---

## Completion

- [ ] All services running
- [ ] Frontend accessible
- [ ] Full user flow tested
- [ ] **DEPLOYMENT COMPLETE! 🎉**
