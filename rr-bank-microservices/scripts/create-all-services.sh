#!/bin/bash

# ============================================
# COMPLETE MICROSERVICES BUILDER
# ============================================

echo "=========================================="
echo "  RR-BANK MICROSERVICES - FULL BUILD"
echo "=========================================="
echo ""

BASE_DIR="/home/claude/rr-bank-microservices"
cd $BASE_DIR

# Function to create complete service
create_service() {
    SERVICE_NAME=$1
    PORT=$2
    DB_PORT=$3
    
    echo "Creating $SERVICE_NAME..."
    
    mkdir -p $SERVICE_NAME/src/main/java/com/rrbank/${SERVICE_NAME//-/}/{controller,service,repository,entity,dto,config,event,feign}
    mkdir -p $SERVICE_NAME/src/main/resources/db/migration
    mkdir -p $SERVICE_NAME/src/test/java/com/rrbank/${SERVICE_NAME//-/}/service
    
    # Create pom.xml
    cat > $SERVICE_NAME/pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.rrbank</groupId>
    <artifactId>$SERVICE_NAME</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.rrbank</groupId>
            <artifactId>shared-library</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>\${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF

    # Create application.yml
    cat > $SERVICE_NAME/src/main/resources/application.yml << EOF
server:
  port: $PORT

spring:
  application:
    name: $SERVICE_NAME
  
  datasource:
    url: jdbc:postgresql://localhost:$DB_PORT/${SERVICE_NAME//-/_}_db
    username: postgres
    password: postgres
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
  
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ${SERVICE_NAME}
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.rrbank: DEBUG
    org.springframework.cloud: DEBUG
EOF

    # Create Dockerfile
    cat > $SERVICE_NAME/Dockerfile << EOF
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/$SERVICE_NAME-1.0.0.jar app.jar
EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    echo "✅ $SERVICE_NAME created"
}

# Create all services
create_service "user-service" 8082 5434
create_service "account-service" 8083 5435
create_service "transaction-service" 8084 5436
create_service "payment-service" 8085 5437
create_service "notification-service" 8087 5438

echo ""
echo "✅ All service templates created!"
echo ""
echo "Services created:"
echo "  - user-service (8082)"
echo "  - account-service (8083)"
echo "  - transaction-service (8084)"
echo "  - payment-service (8085)"
echo "  - notification-service (8087)"
