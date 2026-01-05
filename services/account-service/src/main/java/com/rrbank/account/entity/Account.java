package com.rrbank.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_account_user", columnList = "user_id"),
    @Index(name = "idx_account_customer", columnList = "customer_id"),
    @Index(name = "idx_account_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (accountNumber == null) {
            accountNumber = generateAccountNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Generate a bank-style account number
     * Format: TTNN-NNNN-NNNN (14 chars total)
     * TT = Type prefix (SA=Savings, CH=Checking, CR=Credit, BU=Business)
     * N = Random digits
     */
    private String generateAccountNumber() {
        String prefix = switch (accountType) {
            case SAVINGS -> "SA";
            case CHECKING -> "CH";
            case CREDIT -> "CR";
            case BUSINESS -> "BU";
        };
        
        Random random = new Random();
        // Generate 10 random digits
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            digits.append(random.nextInt(10));
        }
        
        // Format: TTNN-NNNN-NNNN
        String number = prefix + digits.toString();
        return number.substring(0, 4) + "-" + number.substring(4, 8) + "-" + number.substring(8, 12);
    }

    public enum AccountType {
        SAVINGS, CHECKING, CREDIT, BUSINESS
    }

    public enum AccountStatus {
        PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED
    }
}
