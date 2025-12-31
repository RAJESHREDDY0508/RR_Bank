package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Dispute;
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
 * Dispute Repository
 * Phase 3: Data access for transaction disputes
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByDisputeNumber(String disputeNumber);

    Optional<Dispute> findByTransactionId(UUID transactionId);

    List<Dispute> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Dispute> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Page<Dispute> findByStatus(Dispute.DisputeStatus status, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_INFO') " +
           "ORDER BY d.createdAt ASC")
    Page<Dispute> findOpenDisputes(Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.assignedTo = :userId AND d.status IN ('SUBMITTED', 'UNDER_REVIEW')")
    List<Dispute> findAssignedDisputes(@Param("userId") UUID userId);

    @Query("SELECT d FROM Dispute d WHERE d.status IN ('SUBMITTED', 'UNDER_REVIEW') AND d.dueDate < :now")
    List<Dispute> findOverdueDisputes(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_INFO')")
    long countOpenDisputes();

    @Query("SELECT d FROM Dispute d WHERE d.customerId = :customerId AND d.status IN ('SUBMITTED', 'UNDER_REVIEW')")
    List<Dispute> findActiveByCustomer(@Param("customerId") UUID customerId);

    boolean existsByTransactionId(UUID transactionId);
}
