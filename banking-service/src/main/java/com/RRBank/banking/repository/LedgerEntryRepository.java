package com.RRBank.banking.repository;

import com.RRBank.banking.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LedgerEntry entity
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByReferenceId(String referenceId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId")
    BigDecimal calculateBalance(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e " +
           "WHERE e.accountId = :accountId AND e.entryType = 'CREDIT'")
    BigDecimal getTotalCredits(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e " +
           "WHERE e.accountId = :accountId AND e.entryType = 'DEBIT'")
    BigDecimal getTotalDebits(@Param("accountId") UUID accountId);

    @Query("SELECT e FROM LedgerEntry e WHERE e.accountId = :accountId " +
           "AND e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<LedgerEntry> findByAccountIdAndDateRange(
        @Param("accountId") UUID accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long countByAccountId(UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId AND e.createdAt <= :asOfDate")
    BigDecimal calculateBalanceAsOf(
        @Param("accountId") UUID accountId,
        @Param("asOfDate") LocalDateTime asOfDate
    );
}
