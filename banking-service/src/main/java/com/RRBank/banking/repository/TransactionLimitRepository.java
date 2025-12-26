package com.RRBank.banking.repository;

import com.RRBank.banking.entity.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, UUID> {

    List<TransactionLimit> findByUserId(String userId);

    Optional<TransactionLimit> findByUserIdAndLimitType(String userId, TransactionLimit.LimitType limitType);

    List<TransactionLimit> findByUserIdAndEnabledTrue(String userId);

    @Query("SELECT l FROM TransactionLimit l WHERE l.userId = :userId " +
           "AND (l.limitType = :type OR l.limitType = 'ALL') " +
           "AND l.enabled = true " +
           "ORDER BY CASE WHEN l.limitType = :type THEN 0 ELSE 1 END")
    List<TransactionLimit> findApplicableLimits(
        @Param("userId") String userId,
        @Param("type") TransactionLimit.LimitType type
    );

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
