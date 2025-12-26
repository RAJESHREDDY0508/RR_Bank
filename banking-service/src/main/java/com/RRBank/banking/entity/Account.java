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
 * Account Entity - Bank accounts (Savings, Checking, Credit, Business)
 * 
 * IMPORTANT:
 * - account.id is UUID (native PostgreSQL UUID)
 * - account.customer_id references customers.id (UUID)
 * - account.user_id is VARCHAR(36) for direct user reference
 * 
 * Database constraints:
 * - account_type: SAVINGS, CHECKING, CREDIT, BUSINESS
 * - status: PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_customer_id", columnList = "customer_id"),
    @Index(name = "idx_accounts_number", columnList = "account_number"),
    @Index(name = "idx_accounts_status", columnList = "status"),
    @Index(name = "idx_accounts_type", columnList = "account_type"),
    @Index(name = "idx_accounts_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /**
     * Direct reference to user - VARCHAR(36) in database
     * This is the owning user of the account
     */
    @Column(name = "user_id", length = 36)
    private String userId;

    /**
     * Lazy-loaded User entity for convenience
     * Uses insertable=false, updatable=false because userId is the actual column
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", 
                insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "minimum_balance", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @Column(name = "last_statement_date")
    private LocalDateTime lastStatementDate;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        openedAt = now;
        
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
            status = AccountStatus.PENDING;
        }
        if (overdraftLimit == null) {
            overdraftLimit = BigDecimal.ZERO;
        }
        if (interestRate == null) {
            interestRate = BigDecimal.ZERO;
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
     * Check if account is pending approval
     */
    public boolean isPending() {
        return status == AccountStatus.PENDING;
    }

    /**
     * Check if sufficient balance for withdrawal
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        BigDecimal limit = overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO;
        BigDecimal available = balance.add(limit);
        return available.compareTo(amount) >= 0;
    }

    /**
     * Credit amount to account
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.availableBalance = this.balance.add(
            overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO
        );
        this.lastTransactionDate = LocalDateTime.now();
    }

    /**
     * Debit amount from account
     */
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.balance.add(
            overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO
        );
        this.lastTransactionDate = LocalDateTime.now();
    }

    /**
     * Account Type - MUST match database constraint
     * Database: CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS'))
     */
    public enum AccountType {
        SAVINGS,    // Savings account with interest
        CHECKING,   // Checking/Current account
        CREDIT,     // Credit account
        BUSINESS    // Business account
    }

    /**
     * Account Status - MUST match database constraint
     * Database: CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'CLOSED', 'SUSPENDED'))
     */
    public enum AccountStatus {
        PENDING,    // Account awaiting admin approval
        ACTIVE,     // Account is active and operational
        FROZEN,     // Account is frozen (no transactions allowed)
        CLOSED,     // Account is closed permanently
        SUSPENDED   // Account is temporarily suspended
    }
}
