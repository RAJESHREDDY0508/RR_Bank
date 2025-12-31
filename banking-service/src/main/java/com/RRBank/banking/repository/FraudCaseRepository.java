package com.RRBank.banking.repository;

import com.RRBank.banking.entity.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fraud Case Repository
 * Phase 3: Data access for fraud cases
 */
@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {

    Optional<FraudCase> findByCaseNumber(String caseNumber);

    List<FraudCase> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<FraudCase> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Page<FraudCase> findByStatus(FraudCase.CaseStatus status, Pageable pageable);

    Page<FraudCase> findByPriority(FraudCase.Priority priority, Pageable pageable);

    @Query("SELECT f FROM FraudCase f WHERE f.status IN ('OPEN', 'UNDER_REVIEW') ORDER BY f.priority DESC, f.createdAt ASC")
    Page<FraudCase> findOpenCases(Pageable pageable);

    @Query("SELECT f FROM FraudCase f WHERE f.assignedTo = :userId AND f.status IN ('OPEN', 'UNDER_REVIEW')")
    List<FraudCase> findAssignedCases(@Param("userId") UUID userId);

    @Query("SELECT f FROM FraudCase f WHERE f.transactionId = :transactionId")
    Optional<FraudCase> findByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("SELECT f FROM FraudCase f WHERE f.status IN ('OPEN', 'UNDER_REVIEW') AND f.dueDate < :now")
    List<FraudCase> findOverdueCases(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM FraudCase f WHERE f.escalated = true AND f.status IN ('OPEN', 'UNDER_REVIEW')")
    List<FraudCase> findEscalatedCases();

    @Query("SELECT COUNT(f) FROM FraudCase f WHERE f.status IN ('OPEN', 'UNDER_REVIEW')")
    long countOpenCases();

    @Query("SELECT COUNT(f) FROM FraudCase f WHERE f.status IN ('OPEN', 'UNDER_REVIEW') AND f.priority = 'CRITICAL'")
    long countCriticalCases();

    @Query("SELECT f.caseType, COUNT(f) FROM FraudCase f WHERE f.createdAt > :since GROUP BY f.caseType")
    List<Object[]> getCaseTypeStats(@Param("since") LocalDateTime since);
}
