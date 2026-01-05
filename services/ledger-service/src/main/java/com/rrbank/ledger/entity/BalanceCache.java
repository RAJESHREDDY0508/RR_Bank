package com.rrbank.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BalanceCache - Read model for fast balance lookups
 * This is a PROJECTION that can be rebuilt from ledger_entries at any time.
 * NOT the source of truth - just a performance optimization.
 */
@Entity
@Table(name = "balance_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceCache {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    private Long version;
}
