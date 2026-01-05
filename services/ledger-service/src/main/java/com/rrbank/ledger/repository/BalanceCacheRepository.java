package com.rrbank.ledger.repository;

import com.rrbank.ledger.entity.BalanceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BalanceCacheRepository extends JpaRepository<BalanceCache, UUID> {
}
