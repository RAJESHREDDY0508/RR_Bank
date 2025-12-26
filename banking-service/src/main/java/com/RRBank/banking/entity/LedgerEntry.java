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
 * LedgerEntry Entity - Immutable ledger entries for double-entry accounting
 * 
 * This is the core of the banking ledger system.
 * Balance = SUM(CREDIT) - SUM(DEBIT) for an account
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_ledger_created_at", columnList = "created_at"),
    @Index(name = "idx_ledger_reference", columnList = "reference_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "running_balance", precision = 19, scale = 4)
    private BigDecimal runningBalance;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum EntryType {
        DEBIT,
        CREDIT
    }

    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    public BigDecimal getSignedAmount() {
        return isDebit() ? amount.negate() : amount;
    }
}
