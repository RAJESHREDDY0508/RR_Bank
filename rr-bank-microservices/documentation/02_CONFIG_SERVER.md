# CONFIG SERVER - Centralized Configuration Management

## pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.rrbank</groupId>
    <artifactId>config-server</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

## src/main/java/com/rrbank/config/ConfigServerApplication.java
```java
package com.rrbank.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

## src/main/resources/application.yml
```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  
  cloud:
    config:
      server:
        git:
          uri: ${CONFIG_GIT_URI:https://github.com/your-org/config-repo}
          clone-on-start: true
          default-label: main
          search-paths: 
            - '{application}'
          username: ${CONFIG_GIT_USERNAME:}
          password: ${CONFIG_GIT_PASSWORD:}
        # Alternative: Native (filesystem) backend
        native:
          search-locations: classpath:/config
  
  security:
    user:
      name: ${CONFIG_SERVER_USERNAME:configuser}
      password: ${CONFIG_SERVER_PASSWORD:configpass}

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh,env
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.springframework.cloud.config: DEBUG
    org.springframework.security: DEBUG
```

## src/main/java/com/rrbank/config/SecurityConfig.java
```java
package com.rrbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic();
        
        return http.build();
    }
}
```

## Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/config-server-1.0.0.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## k8s-deployment.yml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
  namespace: rr-bank
spec:
  replicas: 2
  selector:
    matchLabels:
      app: config-server
  template:
    metadata:
      labels:
        app: config-server
    spec:
      containers:
      - name: config-server
        image: rrbank/config-server:latest
        ports:
        - containerPort: 8888
        env:
        - name: CONFIG_GIT_URI
          valueFrom:
            configMapKeyRef:
              name: config-server-config
              key: git-uri
        - name: CONFIG_SERVER_USERNAME
          valueFrom:
            secretKeyRef:
              name: config-server-secret
              key: username
        - name: CONFIG_SERVER_PASSWORD
          valueFrom:
            secretKeyRef:
              name: config-server-secret
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8888
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8888
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: config-server
  namespace: rr-bank
spec:
  selector:
    app: config-server
  ports:
  - port: 8888
    targetPort: 8888
  type: ClusterIP
```

## Config Repository Structure (config-repo/)

### application.yml (Common config for all services)
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  level:
    root: INFO
    com.rrbank: DEBUG

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_SERVER:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true
```

### auth-service.yml
```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/auth_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-change-in-production}
  expiration: ${JWT_EXPIRATION:900000}  # 15 minutes
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:86400000}  # 24 hours

oauth2:
  client:
    google:
      client-id: ${GOOGLE_CLIENT_ID:}
      client-secret: ${GOOGLE_CLIENT_SECRET:}
    github:
      client-id: ${GITHUB_CLIENT_ID:}
      client-secret: ${GITHUB_CLIENT_SECRET:}
```

### user-service.yml
```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/user_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      accountService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
```

### account-service.yml
```yaml
server:
  port: 8083

spring:
  application:
    name: account-service
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/account_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true
```

### transaction-service.yml
```yaml
server:
  port: 8084

spring:
  application:
    name: transaction-service
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/transaction_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  
  jpa:
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true

transaction:
  max-amount: ${MAX_TRANSACTION_AMOUNT:100000}
  fraud-check-enabled: ${FRAUD_CHECK_ENABLED:true}
```
