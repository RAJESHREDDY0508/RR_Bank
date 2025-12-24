# SHARED LIBRARY - Common DTOs, Events, Enums

## pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.rrbank</groupId>
    <artifactId>shared-library</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
            <version>3.0.2</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.15.2</version>
        </dependency>
    </dependencies>
</project>
```

## src/main/java/com/rrbank/shared/dto/UserDto.java
```java
package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## src/main/java/com/rrbank/shared/dto/AccountDto.java
```java
package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private UUID accountId;
    private String accountNumber;
    private UUID userId;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## src/main/java/com/rrbank/shared/dto/TransactionDto.java
```java
package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private UUID transactionId;
    private String transactionReference;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
```

## src/main/java/com/rrbank/shared/events/UserCreatedEvent.java
```java
package com.rrbank.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private String eventId;
    private LocalDateTime eventTime;
}
```

## src/main/java/com/rrbank/shared/events/TransactionEvent.java
```java
package com.rrbank.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private UUID transactionId;
    private String transactionReference;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String transactionType;
    private BigDecimal amount;
    private String status;
    private String eventType; // INITIATED, COMPLETED, FAILED
    private LocalDateTime eventTime;
    private String eventId;
}
```

## src/main/java/com/rrbank/shared/enums/AccountType.java
```java
package com.rrbank.shared.enums;

public enum AccountType {
    SAVINGS,
    CHECKING,
    CREDIT,
    LOAN
}
```

## src/main/java/com/rrbank/shared/enums/TransactionStatus.java
```java
package com.rrbank.shared.enums;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED
}
```

## src/main/java/com/rrbank/shared/enums/UserRole.java
```java
package com.rrbank.shared.enums;

public enum UserRole {
    CUSTOMER,
    ADMIN,
    MANAGER
}
```

## src/main/java/com/rrbank/shared/exception/ErrorResponse.java
```java
package com.rrbank.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int code;
    private String error;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
    private String path;
}
```

## src/main/java/com/rrbank/shared/util/KafkaTopics.java
```java
package com.rrbank.shared.util;

public class KafkaTopics {
    // User Topics
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    
    // Account Topics
    public static final String ACCOUNT_CREATED = "account.created";
    public static final String ACCOUNT_UPDATED = "account.updated";
    public static final String ACCOUNT_CLOSED = "account.closed";
    
    // Transaction Topics
    public static final String TRANSACTION_INITIATED = "transaction.initiated";
    public static final String TRANSACTION_COMPLETED = "transaction.completed";
    public static final String TRANSACTION_FAILED = "transaction.failed";
    
    // Payment Topics
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    
    // Notification Topics
    public static final String NOTIFICATION_EMAIL = "notification.email";
    public static final String NOTIFICATION_SMS = "notification.sms";
    public static final String NOTIFICATION_PUSH = "notification.push";
    
    // Audit Topics
    public static final String AUDIT_LOG = "audit.log";
    
    // Fraud Topics
    public static final String FRAUD_ALERT = "fraud.alert";
    public static final String FRAUD_DETECTED = "fraud.detected";
}
```
