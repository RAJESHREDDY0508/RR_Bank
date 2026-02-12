package com.rrbank.ledger.repository;

import com.rrbank.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    /**
     * SOURCE OF TRUTH: Calculate balance from ledger entries
     * Balance = SUM(CREDIT) - SUM(DEBIT)
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = com.rrbank.ledger.entity.LedgerEntry$EntryType.CREDIT THEN e.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN e.entryType = com.rrbank.ledger.entity.LedgerEntry$EntryType.DEBIT THEN e.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId")
    BigDecimal calculateBalance(@Param("accountId") UUID accountId);
    
    /**
     * Alternative simpler query - calculate using native SQL
     */
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END), 0) - " +
                   "COALESCE(SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END), 0) " +
                   "FROM ledger_entries WHERE account_id = :accountId", nativeQuery = true)
    BigDecimal calculateBalanceNative(@Param("accountId") UUID accountId);
    
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
    
    Optional<LedgerEntry> findTopByAccountIdOrderByCreatedAtDesc(UUID accountId);
    
    long countByAccountId(UUID accountId);
}
