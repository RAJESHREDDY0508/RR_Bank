package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Entity
 * Represents bank accounts (Savings, Checking, Credit)
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_customer_id", columnList = "customer_id"),
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_account_status", columnList = "status"),
    @Index(name = "idx_account_type", columnList = "account_type")
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

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "available_balance", precision = 15, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "minimum_balance", precision = 15, scale = 2)
    private BigDecimal minimumBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "overdraft_limit", precision = 15, scale = 2)
    private BigDecimal overdraftLimit;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_statement_date")
    private LocalDateTime lastStatementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        openedAt = LocalDateTime.now();
        
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = balance;
        }
        if (minimumBalance == null) {
            minimumBalance = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "USD";
        }
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (overdraftLimit == null) {
            overdraftLimit = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get account ID as string (for DTO compatibility)
     */
    public String getAccountId() {
        return id != null ? id.toString() : null;
    }

    /**
     * Check if account is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Check if account is frozen
     */
    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    /**
     * Check if account is closed
     */
    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    /**
     * Check if sufficient balance for withdrawal
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        BigDecimal available = balance.add(overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO);
        return available.compareTo(amount) >= 0;
    }

    /**
     * Credit amount to account
     */
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.availableBalance = this.balance.add(overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO);
    }

    /**
     * Debit amount from account
     */
    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.balance.add(overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO);
    }

    /**
     * Account Type Enum
     */
    public enum AccountType {
        SAVINGS,      // Savings account
        CHECKING,     // Checking/Current account
        CREDIT,       // Credit account
        BUSINESS      // Business account
    }

    /**
     * Account Status Enum
     */
    public enum AccountStatus {
        PENDING,      // Account awaiting admin approval
        ACTIVE,       // Account is active and operational
        FROZEN,       // Account is frozen (no transactions allowed)
        CLOSED,       // Account is closed permanently
        SUSPENDED     // Account is temporarily suspended
    }
}
