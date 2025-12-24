# DOCKER COMPOSE - Complete Stack

## docker-compose.yml (Complete Infrastructure + Services)

```yaml
version: '3.8'

services:
  # ==================== DATABASES ====================
  
  auth-db:
    image: postgres:15-alpine
    container_name: auth-db
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - auth-db-data:/var/lib/postgresql/data
    networks:
      - rr-bank-network
  
  user-db:
    image: postgres:15-alpine
    container_name: user-db
    environment:
      POSTGRES_DB: user_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5434:5432"
    volumes:
      - user-db-data:/var/lib/postgresql/data
    networks:
      - rr-bank-network
  
  account-db:
    image: postgres:15-alpine
    container_name: account-db
    environment:
      POSTGRES_DB: account_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5435:5432"
    volumes:
      - account-db-data:/var/lib/postgresql/data
    networks:
      - rr-bank-network
  
  transaction-db:
    image: postgres:15-alpine
    container_name: transaction-db
    environment:
      POSTGRES_DB: transaction_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5436:5432"
    volumes:
      - transaction-db-data:/var/lib/postgresql/data
    networks:
      - rr-bank-network
  
  payment-db:
    image: postgres:15-alpine
    container_name: payment-db
    environment:
      POSTGRES_DB: payment_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5437:5432"
    volumes:
      - payment-db-data:/var/lib/postgresql/data
    networks:
      - rr-bank-network
  
  # ==================== MESSAGE BROKER ====================
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - rr-bank-network
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    networks:
      - rr-bank-network
  
  # ==================== CACHING ====================
  
  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    networks:
      - rr-bank-network
  
  # ==================== MONITORING & LOGGING ====================
  
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - rr-bank-network
  
  logstash:
    image: docker.elastic.co/logstash/logstash:8.10.0
    container_name: logstash
    depends_on:
      - elasticsearch
    ports:
      - "5000:5000"
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
    networks:
      - rr-bank-network
  
  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    container_name: kibana
    depends_on:
      - elasticsearch
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    networks:
      - rr-bank-network
  
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - rr-bank-network
  
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    depends_on:
      - prometheus
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - rr-bank-network
  
  # ==================== INFRASTRUCTURE SERVICES ====================
  
  config-server:
    image: rrbank/config-server:latest
    container_name: config-server
    build:
      context: ./config-server
      dockerfile: Dockerfile
    ports:
      - "8888:8888"
    environment:
      CONFIG_GIT_URI: https://github.com/your-org/config-repo
      CONFIG_SERVER_USERNAME: configuser
      CONFIG_SERVER_PASSWORD: configpass
    networks:
      - rr-bank-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  discovery-server:
    image: rrbank/discovery-server:latest
    container_name: discovery-server
    build:
      context: ./discovery-server
      dockerfile: Dockerfile
    depends_on:
      - config-server
    ports:
      - "8761:8761"
    environment:
      EUREKA_USERNAME: eurekauser
      EUREKA_PASSWORD: eurekapass
    networks:
      - rr-bank-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  api-gateway:
    image: rrbank/api-gateway:latest
    container_name: api-gateway
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - redis
    ports:
      - "8080:8080"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      REDIS_HOST: redis
      JWT_SECRET: your-256-bit-secret-key-here-change-in-production
    networks:
      - rr-bank-network
  
  # ==================== BUSINESS SERVICES ====================
  
  auth-service:
    image: rrbank/auth-service:latest
    container_name: auth-service
    build:
      context: ./auth-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - auth-db
      - kafka
    ports:
      - "8081:8081"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      DB_URL: jdbc:postgresql://auth-db:5432/auth_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      JWT_SECRET: your-256-bit-secret-key-here-change-in-production
    networks:
      - rr-bank-network
  
  user-service:
    image: rrbank/user-service:latest
    container_name: user-service
    build:
      context: ./user-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - user-db
      - kafka
    ports:
      - "8082:8082"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      DB_URL: jdbc:postgresql://user-db:5432/user_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    networks:
      - rr-bank-network
  
  account-service:
    image: rrbank/account-service:latest
    container_name: account-service
    build:
      context: ./account-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - account-db
      - kafka
    ports:
      - "8083:8083"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      DB_URL: jdbc:postgresql://account-db:5432/account_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    networks:
      - rr-bank-network
  
  transaction-service:
    image: rrbank/transaction-service:latest
    container_name: transaction-service
    build:
      context: ./transaction-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - transaction-db
      - kafka
    ports:
      - "8084:8084"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      DB_URL: jdbc:postgresql://transaction-db:5432/transaction_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    networks:
      - rr-bank-network
  
  payment-service:
    image: rrbank/payment-service:latest
    container_name: payment-service
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - payment-db
      - kafka
    ports:
      - "8085:8085"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      DB_URL: jdbc:postgresql://payment-db:5432/payment_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    networks:
      - rr-bank-network
  
  notification-service:
    image: rrbank/notification-service:latest
    container_name: notification-service
    build:
      context: ./notification-service
      dockerfile: Dockerfile
    depends_on:
      - config-server
      - discovery-server
      - kafka
    ports:
      - "8087:8087"
    environment:
      CONFIG_SERVER_URI: http://config-server:8888
      EUREKA_SERVER: http://discovery-server:8761/eureka/
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SMTP_HOST: smtp.gmail.com
      SMTP_PORT: 587
      SMTP_USERNAME: your-email@gmail.com
      SMTP_PASSWORD: your-app-password
    networks:
      - rr-bank-network

networks:
  rr-bank-network:
    driver: bridge

volumes:
  auth-db-data:
  user-db-data:
  account-db-data:
  transaction-db-data:
  payment-db-data:
  redis-data:
  elasticsearch-data:
  prometheus-data:
  grafana-data:
```

## Makefile for Easy Management

```makefile
.PHONY: build start stop restart logs clean

# Build all services
build:
	mvn clean package -DskipTests
	docker-compose build

# Start all services
start:
	docker-compose up -d

# Stop all services
stop:
	docker-compose down

# Restart all services
restart:
	docker-compose restart

# View logs
logs:
	docker-compose logs -f

# Clean everything
clean:
	docker-compose down -v
	mvn clean

# Start only infrastructure
infra:
	docker-compose up -d config-server discovery-server kafka redis postgres

# Start individual service
start-auth:
	docker-compose up -d auth-service

start-user:
	docker-compose up -d user-service

# Health check
health:
	@echo "Checking service health..."
	@curl -s http://localhost:8888/actuator/health | jq
	@curl -s http://localhost:8761/actuator/health | jq
	@curl -s http://localhost:8080/actuator/health | jq
```

## Quick Start Commands

```bash
# 1. Build all services
make build

# 2. Start infrastructure first
make infra

# 3. Wait for infrastructure to be ready (30 seconds)
sleep 30

# 4. Start all services
make start

# 5. Check logs
make logs

# 6. Health check
make health

# 7. Stop everything
make stop

# 8. Clean up
make clean
```
