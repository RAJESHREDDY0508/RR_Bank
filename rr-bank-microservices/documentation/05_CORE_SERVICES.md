# CORE BUSINESS SERVICES - Account, Transaction, User, Payment

## ACCOUNT SERVICE (Port: 8083)

### Key Files

**AccountService.java**
```java
package com.rrbank.account.service;

import com.rrbank.account.entity.Account;
import com.rrbank.account.repository.AccountRepository;
import com.rrbank.shared.dto.AccountDto;
import com.rrbank.shared.events.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public AccountDto createAccount(UUID userId, String accountType) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(userId)
                .accountType(accountType)
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .status("ACTIVE")
                .build();
        
        account = accountRepository.save(account);
        publishAccountCreatedEvent(account);
        return mapToDto(account);
    }
    
    @Transactional
    public void updateBalance(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }
    
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
    
    private void publishAccountCreatedEvent(Account account) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .build();
        kafkaTemplate.send("account.created", event);
    }
    
    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }
}
```

**AccountController.java**
```java
package com.rrbank.account.controller;

import com.rrbank.account.service.AccountService;
import com.rrbank.shared.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestParam UUID userId, 
                                                     @RequestParam String accountType) {
        return ResponseEntity.ok(accountService.createAccount(userId, accountType));
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDto>> getUserAccounts(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }
}
```

---

## TRANSACTION SERVICE (Port: 8084)

### Key Files

**TransactionService.java**
```java
package com.rrbank.transaction.service;

import com.rrbank.transaction.entity.Transaction;
import com.rrbank.transaction.feign.AccountFeignClient;
import com.rrbank.transaction.repository.TransactionRepository;
import com.rrbank.shared.dto.TransactionDto;
import com.rrbank.shared.events.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountFeignClient accountFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public TransactionDto transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
        // Create pending transaction
        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .transactionType("TRANSFER")
                .status("PENDING")
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Publish transaction initiated event
        publishTransactionEvent(transaction, "INITIATED");
        
        try {
            // Debit from source account
            accountFeignClient.updateBalance(fromAccountId, amount.negate());
            
            // Credit to destination account
            accountFeignClient.updateBalance(toAccountId, amount);
            
            // Mark as completed
            transaction.setStatus("COMPLETED");
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            publishTransactionEvent(transaction, "COMPLETED");
            
            log.info("Transfer completed: {}", transaction.getTransactionReference());
            
        } catch (Exception e) {
            // Mark as failed
            transaction.setStatus("FAILED");
            transaction = transactionRepository.save(transaction);
            
            publishTransactionEvent(transaction, "FAILED");
            
            log.error("Transfer failed: {}", transaction.getTransactionReference(), e);
            throw new RuntimeException("Transfer failed", e);
        }
        
        return mapToDto(transaction);
    }
    
    private String generateReference() {
        return "TXN" + System.currentTimeMillis();
    }
    
    private void publishTransactionEvent(Transaction transaction, String eventType) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .eventType(eventType)
                .eventTime(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .build();
        
        kafkaTemplate.send("transaction." + eventType.toLowerCase(), event);
    }
    
    private TransactionDto mapToDto(Transaction transaction) {
        return TransactionDto.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
```

**AccountFeignClient.java**
```java
package com.rrbank.transaction.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "account-service")
public interface AccountFeignClient {
    
    @PutMapping("/api/accounts/{accountId}/balance")
    @CircuitBreaker(name = "accountService", fallbackMethod = "updateBalanceFallback")
    void updateBalance(@PathVariable UUID accountId, @RequestParam BigDecimal amount);
    
    default void updateBalanceFallback(UUID accountId, BigDecimal amount, Throwable throwable) {
        throw new RuntimeException("Account service is unavailable. Please try again later.");
    }
}
```

---

## USER SERVICE (Port: 8082)

### Key Files

**UserService.java**
```java
package com.rrbank.user.service;

import com.rrbank.user.entity.User;
import com.rrbank.user.event.UserEventConsumer;
import com.rrbank.user.repository.UserRepository;
import com.rrbank.shared.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public UserDto createUser(UUID userId, String username, String email, String role) {
        User user = User.builder()
                .id(userId)
                .username(username)
                .email(email)
                .role(role)
                .active(true)
                .build();
        
        user = userRepository.save(user);
        return mapToDto(user);
    }
    
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }
    
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
```

**UserEventConsumer.java**
```java
package com.rrbank.user.event;

import com.rrbank.shared.events.UserCreatedEvent;
import com.rrbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {
    
    private final UserService userService;
    
    @KafkaListener(topics = "user.created", groupId = "user-service")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent: {}", event.getUserId());
        
        try {
            userService.createUser(
                event.getUserId(),
                event.getUsername(),
                event.getEmail(),
                event.getRole()
            );
            log.info("User profile created: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create user profile", e);
        }
    }
}
```

---

## PAYMENT SERVICE (Port: 8085)

### Key Files

**PaymentService.java**
```java
package com.rrbank.payment.service;

import com.rrbank.payment.entity.Payment;
import com.rrbank.payment.feign.TransactionFeignClient;
import com.rrbank.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final TransactionFeignClient transactionFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public Payment processPayment(UUID fromAccountId, UUID toAccountId, 
                                   BigDecimal amount, String description) {
        Payment payment = Payment.builder()
                .paymentReference(generateReference())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .description(description)
                .status("PENDING")
                .build();
        
        payment = paymentRepository.save(payment);
        
        try {
            // Process via transaction service
            transactionFeignClient.transfer(fromAccountId, toAccountId, amount);
            
            payment.setStatus("COMPLETED");
            payment.setCompletedAt(LocalDateTime.now());
            
            log.info("Payment completed: {}", payment.getPaymentReference());
            
        } catch (Exception e) {
            payment.setStatus("FAILED");
            log.error("Payment failed: {}", payment.getPaymentReference(), e);
        }
        
        return paymentRepository.save(payment);
    }
    
    private String generateReference() {
        return "PAY" + System.currentTimeMillis();
    }
}
```

---

## NOTIFICATION SERVICE (Port: 8087)

### Key Files

**NotificationService.java**
```java
package com.rrbank.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final JavaMailSender emailSender;
    
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            emailSender.send(message);
            
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }
    
    public void sendSms(String phoneNumber, String message) {
        // Integrate with Twilio/SNS
        log.info("SMS sent to: {}", phoneNumber);
    }
}
```

**TransactionEventConsumer.java**
```java
package com.rrbank.notification.event;

import com.rrbank.notification.service.NotificationService;
import com.rrbank.shared.events.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "transaction.completed", groupId = "notification-service")
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("Sending notification for transaction: {}", event.getTransactionId());
        
        String subject = "Transaction Completed";
        String body = String.format("Your transaction of $%.2f has been completed. " +
                "Reference: %s", event.getAmount(), event.getTransactionReference());
        
        // Get user email and send notification
        // notificationService.sendEmail(userEmail, subject, body);
    }
}
```

---

## Common Dependencies (pom.xml for all services)

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Spring Cloud -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Resilience4j -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
        <version>2.1.0</version>
    </dependency>
    
    <!-- Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Shared Library -->
    <dependency>
        <groupId>com.rrbank</groupId>
        <artifactId>shared-library</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

---

## Common application.yml (bootstrap.yml)

```yaml
spring:
  application:
    name: ${SERVICE_NAME}
  
  config:
    import: optional:configserver:http://config-server:8888
  
  cloud:
    config:
      username: configuser
      password: configpass
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000

eureka:
  client:
    serviceUrl:
      defaultZone: http://discovery-server:8761/eureka/
  instance:
    prefer-ip-address: true
```
