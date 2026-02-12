package com.rrbank.ledger.repository;

import com.rrbank.ledger.entity.BalanceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface BalanceCacheRepository extends JpaRepository<BalanceCache, UUID> {
    
    /**
     * Sum all account balances
     */
    @Query("SELECT COALESCE(SUM(b.balance), 0) FROM BalanceCache b")
    BigDecimal sumAllBalances();
    
    /**
     * Count accounts with positive balance
     */
    @Query("SELECT COUNT(b) FROM BalanceCache b WHERE b.balance > 0")
    long countWithPositiveBalance();
}
