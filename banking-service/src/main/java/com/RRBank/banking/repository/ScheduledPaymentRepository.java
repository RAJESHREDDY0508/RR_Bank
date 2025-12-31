package com.RRBank.banking.repository;

import com.RRBank.banking.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scheduled Payment Repository
 * Phase 3: Data access for scheduled/recurring payments
 */
@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {

    Optional<ScheduledPayment> findByScheduleReference(String scheduleReference);

    List<ScheduledPayment> findByCustomerIdOrderByNextExecutionDateAsc(UUID customerId);

    List<ScheduledPayment> findByAccountIdOrderByNextExecutionDateAsc(UUID accountId);

    List<ScheduledPayment> findByStatus(ScheduledPayment.ScheduleStatus status);

    @Query("SELECT s FROM ScheduledPayment s WHERE s.status = 'ACTIVE' AND s.nextExecutionDate <= :today")
    List<ScheduledPayment> findDueForExecution(@Param("today") LocalDate today);

    @Query("SELECT s FROM ScheduledPayment s WHERE s.customerId = :customerId AND s.status = 'ACTIVE'")
    List<ScheduledPayment> findActiveByCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT s FROM ScheduledPayment s WHERE s.status = 'SUSPENDED'")
    List<ScheduledPayment> findSuspended();

    long countByCustomerIdAndStatus(UUID customerId, ScheduledPayment.ScheduleStatus status);

    @Query("SELECT s FROM ScheduledPayment s WHERE s.payeeId = :payeeId")
    List<ScheduledPayment> findByPayeeId(@Param("payeeId") UUID payeeId);
}
