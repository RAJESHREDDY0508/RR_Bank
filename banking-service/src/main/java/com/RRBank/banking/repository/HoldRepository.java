package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Hold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Hold Repository
 * Phase 3: Data access for account holds
 */
@Repository
public interface HoldRepository extends JpaRepository<Hold, UUID> {

    List<Hold> findByAccountIdAndStatus(UUID accountId, Hold.HoldStatus status);

    List<Hold> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    @Query("SELECT h FROM Hold h WHERE h.accountId = :accountId AND h.status = 'ACTIVE'")
    List<Hold> findActiveHoldsByAccount(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM Hold h WHERE h.accountId = :accountId AND h.status = 'ACTIVE'")
    BigDecimal getTotalActiveHoldsAmount(@Param("accountId") UUID accountId);

    @Query("SELECT h FROM Hold h WHERE h.status = 'ACTIVE' AND h.expiresAt < :now")
    List<Hold> findExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT h FROM Hold h WHERE h.transactionId = :transactionId")
    List<Hold> findByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("SELECT h FROM Hold h WHERE h.holdType = :holdType AND h.status = 'ACTIVE'")
    List<Hold> findActiveByType(@Param("holdType") Hold.HoldType holdType);

    @Modifying
    @Query("UPDATE Hold h SET h.status = 'EXPIRED', h.releasedAt = :now, h.releaseReason = 'Auto-expired' " +
           "WHERE h.status = 'ACTIVE' AND h.expiresAt < :now")
    int expireHolds(@Param("now") LocalDateTime now);

    long countByAccountIdAndStatus(UUID accountId, Hold.HoldStatus status);
}
