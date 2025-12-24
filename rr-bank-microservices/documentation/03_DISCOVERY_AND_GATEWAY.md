# DISCOVERY SERVER (Eureka) + API GATEWAY

## DISCOVERY SERVER

### pom.xml
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
    <artifactId>discovery-server</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
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

### DiscoveryServerApplication.java
```java
package com.rrbank.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.java, args);
    }
}
```

### application.yml
```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server
  
  security:
    user:
      name: ${EUREKA_USERNAME:eurekauser}
      password: ${EUREKA_PASSWORD:eurekapass}

eureka:
  instance:
    hostname: ${EUREKA_HOSTNAME:localhost}
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 15000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/discovery-server-1.0.0.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## API GATEWAY

### pom.xml
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
    <artifactId>api-gateway</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
        </dependency>
    </dependencies>
</project>
```

### ApiGatewayApplication.java
```java
package com.rrbank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

### GatewayConfig.java
```java
package com.rrbank.gateway.config;

import com.rrbank.gateway.filter.AuthenticationFilter;
import com.rrbank.gateway.filter.LoggingFilter;
import com.rrbank.gateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {
    
    private final AuthenticationFilter authenticationFilter;
    private final LoggingFilter loggingFilter;
    private final RateLimitFilter rateLimitFilter;
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Auth Service - Public endpoints
            .route("auth-service-public", r -> r
                .path("/api/auth/**")
                .filters(f -> f
                    .filter(loggingFilter)
                    .filter(rateLimitFilter)
                    .circuitBreaker(config -> config
                        .setName("authCircuitBreaker")
                        .setFallbackUri("forward:/fallback/auth"))
                )
                .uri("lb://auth-service"))
            
            // User Service - Protected
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                    .filter(rateLimitFilter)
                    .circuitBreaker(config -> config
                        .setName("userCircuitBreaker"))
                )
                .uri("lb://user-service"))
            
            // Account Service - Protected
            .route("account-service", r -> r
                .path("/api/accounts/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                    .filter(rateLimitFilter)
                    .circuitBreaker(config -> config
                        .setName("accountCircuitBreaker"))
                )
                .uri("lb://account-service"))
            
            // Transaction Service - Protected
            .route("transaction-service", r -> r
                .path("/api/transactions/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                    .filter(rateLimitFilter)
                    .circuitBreaker(config -> config
                        .setName("transactionCircuitBreaker"))
                )
                .uri("lb://transaction-service"))
            
            // Payment Service - Protected
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                    .filter(rateLimitFilter)
                )
                .uri("lb://payment-service"))
            
            // Statement Service - Protected
            .route("statement-service", r -> r
                .path("/api/statements/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                )
                .uri("lb://statement-service"))
            
            // Admin Service - Protected + Admin only
            .route("admin-service", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .filter(authenticationFilter)
                    .filter(loggingFilter)
                )
                .uri("lb://admin-service"))
            
            .build();
    }
}
```

### AuthenticationFilter.java
```java
package com.rrbank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter implements GatewayFilter {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Check if Authorization header exists
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
        }
        
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
        }
        
        String token = authHeader.substring(7);
        
        try {
            Claims claims = validateToken(token);
            
            // Add user info to request headers
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }
    
    private Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(jwtSecret.getBytes())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }
}
```

### LoggingFilter.java
```java
package com.rrbank.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        log.info("Incoming request: {} {}", method, path);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode().value();
            
            log.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                method, path, statusCode, duration);
        }));
    }
}
```

### RateLimitFilter.java
```java
package com.rrbank.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GatewayFilter {
    
    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private static final long RATE_LIMIT = 100; // requests per minute
    private static final Duration WINDOW = Duration.ofMinutes(1);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String key = "rate_limit:" + clientIp;
        
        return redisTemplate.opsForValue()
            .increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    // Set expiration on first request
                    return redisTemplate.expire(key, WINDOW)
                        .flatMap(success -> processRequest(exchange, chain, count));
                }
                return processRequest(exchange, chain, count);
            })
            .onErrorResume(e -> {
                log.error("Rate limit check failed: {}", e.getMessage());
                return chain.filter(exchange);
            });
    }
    
    private Mono<Void> processRequest(ServerWebExchange exchange, GatewayFilterChain chain, Long count) {
        if (count > RATE_LIMIT) {
            log.warn("Rate limit exceeded for IP: {}", 
                exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

### application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  cloud:
    config:
      uri: http://localhost:8888
      username: configuser
      password: configpass
    
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
  
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-change-in-production}

resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
    instances:
      authCircuitBreaker:
        baseConfig: default
      userCircuitBreaker:
        baseConfig: default
      accountCircuitBreaker:
        baseConfig: default
      transactionCircuitBreaker:
        baseConfig: default

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    com.rrbank.gateway: DEBUG
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
