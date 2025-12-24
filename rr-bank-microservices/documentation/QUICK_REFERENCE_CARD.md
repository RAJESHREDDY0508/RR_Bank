# 📋 RR-BANK MICROSERVICES - QUICK REFERENCE CARD

## 🚀 ONE-COMMAND DEPLOYMENT

```bash
# Complete deployment in one command
curl -L https://github.com/your-org/rr-bank-microservices/raw/main/scripts/deploy.sh | bash
```

---

## 🎯 SERVICE PORTS REFERENCE

| Service | Port | Purpose | Health Check |
|---------|------|---------|--------------|
| Config Server | 8888 | Configuration | http://localhost:8888/actuator/health |
| Discovery Server | 8761 | Service Registry | http://localhost:8761/actuator/health |
| **API Gateway** | **8080** | **Main Entry** | http://localhost:8080/actuator/health |
| Auth Service | 8081 | Authentication | http://localhost:8081/actuator/health |
| User Service | 8082 | User Management | http://localhost:8082/actuator/health |
| Account Service | 8083 | Account Operations | http://localhost:8083/actuator/health |
| Transaction Service | 8084 | Transactions | http://localhost:8084/actuator/health |
| Payment Service | 8085 | Payments | http://localhost:8085/actuator/health |
| Statement Service | 8086 | Statements | http://localhost:8086/actuator/health |
| Notification Service | 8087 | Notifications | http://localhost:8087/actuator/health |
| Audit Service | 8088 | Audit Logs | http://localhost:8088/actuator/health |
| Fraud Service | 8089 | Fraud Detection | http://localhost:8089/actuator/health |
| Admin Service | 8090 | Admin Operations | http://localhost:8090/actuator/health |
| PostgreSQL | 5432 | Primary Database | - |
| Kafka | 9092 | Message Broker | - |
| Redis | 6379 | Cache | - |
| Elasticsearch | 9200 | Logging | http://localhost:9200 |
| Kibana | 5601 | Log Viewer | http://localhost:5601 |
| Prometheus | 9090 | Metrics | http://localhost:9090 |
| Grafana | 3000 | Dashboards | http://localhost:3000 |

---

## 🔑 COMMON API ENDPOINTS

### **Authentication**
```bash
# Register
POST /api/auth/register
{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePass@123",
  "role": "CUSTOMER"
}

# Login
POST /api/auth/login
{
  "username": "john.doe",
  "password": "SecurePass@123"
}

# Refresh Token
POST /api/auth/refresh
Header: Authorization: Bearer <REFRESH_TOKEN>
```

### **User Management**
```bash
# Get User Profile
GET /api/users/{userId}
Header: Authorization: Bearer <ACCESS_TOKEN>

# Update Profile
PUT /api/users/{userId}
Header: Authorization: Bearer <ACCESS_TOKEN>
```

### **Account Operations**
```bash
# Create Account
POST /api/accounts
{
  "userId": "uuid",
  "accountType": "SAVINGS"
}

# Get Account Balance
GET /api/accounts/{accountId}/balance

# Get User Accounts
GET /api/accounts/user/{userId}
```

### **Transactions**
```bash
# Transfer Money
POST /api/transactions/transfer
{
  "fromAccountId": "uuid1",
  "toAccountId": "uuid2",
  "amount": 500.00,
  "description": "Payment"
}

# Get Transaction History
GET /api/transactions/account/{accountId}?page=0&size=20

# Get Transaction by ID
GET /api/transactions/{transactionId}
```

### **Payments**
```bash
# Make Payment
POST /api/payments
{
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 150.00,
  "description": "Utility bill"
}

# Schedule Payment
POST /api/payments/schedule
{
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 100.00,
  "frequency": "MONTHLY",
  "startDate": "2024-01-01"
}
```

---

## 🐳 DOCKER COMMANDS

```bash
# Build all services
docker-compose build

# Start all services
docker-compose up -d

# Start specific services
docker-compose up -d auth-service user-service

# Stop all services
docker-compose down

# View logs
docker-compose logs -f [service-name]

# Restart service
docker-compose restart auth-service

# Scale service
docker-compose up -d --scale transaction-service=3

# Remove all containers and volumes
docker-compose down -v

# Rebuild and start
docker-compose up -d --build
```

---

## ☸️ KUBERNETES COMMANDS

```bash
# Deploy all services
kubectl apply -f kubernetes/

# Check pods
kubectl get pods -n rr-bank

# Check services
kubectl get services -n rr-bank

# Check deployments
kubectl get deployments -n rr-bank

# View logs
kubectl logs -f deployment/auth-service -n rr-bank

# Scale deployment
kubectl scale deployment/auth-service --replicas=5 -n rr-bank

# Port forward
kubectl port-forward service/api-gateway 8080:8080 -n rr-bank

# Describe pod
kubectl describe pod auth-service-xxx -n rr-bank

# Execute command in pod
kubectl exec -it auth-service-xxx -n rr-bank -- /bin/sh

# Delete all resources
kubectl delete namespace rr-bank

# Rollback deployment
kubectl rollout undo deployment/auth-service -n rr-bank

# Check rollout status
kubectl rollout status deployment/auth-service -n rr-bank
```

---

## 🔧 MAVEN COMMANDS

```bash
# Build all projects
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Generate code coverage report
mvn jacoco:report

# Run Spring Boot application
mvn spring-boot:run

# Debug Spring Boot application
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 📊 KAFKA COMMANDS

```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create topic
kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic user.created --partitions 3 --replication-factor 1

# Describe topic
kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic user.created

# Produce message
kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic user.created

# Consume messages
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic user.created --from-beginning

# List consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Describe consumer group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group user-service

# Reset consumer offset
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group user-service --reset-offsets --to-earliest --topic user.created --execute
```

---

## 🗄️ DATABASE COMMANDS

```bash
# Connect to database
docker exec -it postgres psql -U postgres -d auth_db

# List databases
\l

# Connect to database
\c auth_db

# List tables
\dt

# Describe table
\d user_credentials

# Run query
SELECT * FROM user_credentials;

# Export data
pg_dump -U postgres -d auth_db > backup.sql

# Import data
psql -U postgres -d auth_db < backup.sql

# Check connections
SELECT * FROM pg_stat_activity;
```

---

## 🔍 DEBUGGING COMMANDS

```bash
# Check if service is listening on port
netstat -an | grep 8081

# Check DNS resolution
nslookup auth-service

# Check network connectivity
curl -v http://auth-service:8081/actuator/health

# Check environment variables
docker exec auth-service env

# Check disk usage
docker system df

# Check resource usage
docker stats

# View container processes
docker top auth-service

# Inspect container
docker inspect auth-service
```

---

## 📈 MONITORING QUERIES

### **Prometheus Queries**
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# CPU usage
container_cpu_usage_seconds_total

# Memory usage
container_memory_usage_bytes

# Active connections
hikaricp_connections_active

# Kafka lag
kafka_consumergroup_lag
```

---

## 🚨 COMMON ISSUES & SOLUTIONS

### **Issue: Service not registering with Eureka**
```bash
# Solution 1: Check Eureka URL in application.yml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/

# Solution 2: Check network connectivity
curl http://discovery-server:8761/eureka/apps

# Solution 3: Restart service
docker-compose restart user-service
```

### **Issue: Database connection failed**
```bash
# Solution 1: Check database is running
docker-compose ps postgres

# Solution 2: Check connection string
docker-compose logs auth-service | grep datasource

# Solution 3: Verify credentials
docker exec -it postgres psql -U postgres -d auth_db
```

### **Issue: Kafka not receiving messages**
```bash
# Solution 1: Check Kafka is running
docker-compose ps kafka

# Solution 2: Check topic exists
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Solution 3: Check consumer group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group user-service
```

### **Issue: High memory usage**
```bash
# Solution 1: Check heap size
docker exec auth-service java -XX:+PrintFlagsFinal -version | grep HeapSize

# Solution 2: Adjust memory limits
docker-compose.yml:
  auth-service:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1g"

# Solution 3: Check for memory leaks
docker stats auth-service
```

---

## 🔐 SECURITY CHECKLIST

- [ ] JWT secret changed from default
- [ ] Database passwords changed from default
- [ ] Config Server secured with credentials
- [ ] Eureka secured with credentials
- [ ] HTTPS enabled on API Gateway
- [ ] CORS configured correctly
- [ ] Rate limiting enabled
- [ ] API keys rotated
- [ ] Secrets stored in Kubernetes Secrets
- [ ] Network policies applied
- [ ] Pod security policies enabled
- [ ] Image vulnerability scanning enabled

---

## 🎯 PERFORMANCE TUNING

### **JVM Options**
```bash
JAVA_OPTS="
  -Xms512m
  -Xmx2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+UnlockExperimentalVMOptions
  -XX:+DisableExplicitGC
  -Djava.security.egd=file:/dev/./urandom
"
```

### **Database Connection Pool**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### **Kafka Producer**
```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      buffer-memory: 33554432
      compression-type: snappy
      linger-ms: 10
```

---

## 📱 POSTMAN COLLECTION

Import this collection for quick testing:

```json
{
  "info": {
    "name": "RR-Bank Microservices",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register",
          "request": {
            "method": "POST",
            "url": "{{base_url}}/api/auth/register",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"testuser\",\n  \"email\": \"test@example.com\",\n  \"password\": \"Test@123\",\n  \"role\": \"CUSTOMER\"\n}"
            }
          }
        },
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "url": "{{base_url}}/api/auth/login",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"testuser\",\n  \"password\": \"Test@123\"\n}"
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    }
  ]
}
```

---

## 🎓 LEARNING RESOURCES

1. **Spring Boot**: https://spring.io/projects/spring-boot
2. **Spring Cloud**: https://spring.io/projects/spring-cloud
3. **Kubernetes**: https://kubernetes.io/docs/
4. **Docker**: https://docs.docker.com/
5. **Kafka**: https://kafka.apache.org/documentation/
6. **Microservices Patterns**: https://microservices.io/patterns/

---

## 📞 EMERGENCY CONTACTS

- **DevOps Lead**: devops@rrbank.com
- **Security Team**: security@rrbank.com
- **On-Call**: +1-555-0100 (24/7)
- **Slack**: #rr-bank-emergencies

---

**Keep this reference card handy for quick lookups during development!** 📋
