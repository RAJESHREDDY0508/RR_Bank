package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Repository
 * Data access layer for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by reference
     */
    Optional<Payment> findByPaymentReference(String paymentReference);

    /**
     * Find all payments for an account
     */
    List<Payment> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Find all payments for a customer
     */
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);

    /**
     * Find payments by type
     */
    List<Payment> findByPaymentTypeOrderByCreatedAtDesc(Payment.PaymentType paymentType);

    /**
     * Find payments by customer and status
     */
    List<Payment> findByCustomerIdAndStatusOrderByCreatedAtDesc(UUID customerId, Payment.PaymentStatus status);

    /**
     * Find payments by account and status
     */
    List<Payment> findByAccountIdAndStatusOrderByCreatedAtDesc(UUID accountId, Payment.PaymentStatus status);

    /**
     * Find payments scheduled for a specific date
     */
    List<Payment> findByScheduledDateAndStatus(LocalDate scheduledDate, Payment.PaymentStatus status);

    /**
     * Find payments by date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find pending/scheduled payments that need processing
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN ('PENDING', 'SCHEDULED') " +
           "AND p.scheduledDate <= :today ORDER BY p.scheduledDate ASC")
    List<Payment> findPendingPaymentsDueToday(@Param("today") LocalDate today);

    /**
     * Count payments by customer
     */
    long countByCustomerId(UUID customerId);

    /**
     * Count payments by status
     */
    long countByStatus(Payment.PaymentStatus status);

    /**
     * Get total payment amount by customer
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.customerId = :customerId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaymentAmountByCustomer(@Param("customerId") UUID customerId);

    /**
     * Get total payment amount by account
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.accountId = :accountId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaymentAmountByAccount(@Param("accountId") UUID accountId);

    /**
     * Find recent payments for customer
     */
    @Query(value = "SELECT p FROM Payment p WHERE p.customerId = :customerId " +
                   "ORDER BY p.createdAt DESC LIMIT :limit")
    List<Payment> findRecentPaymentsByCustomer(@Param("customerId") UUID customerId,
                                               @Param("limit") int limit);

    /**
     * Find payment by idempotency key
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Search payments by payee name
     */
    @Query("SELECT p FROM Payment p WHERE LOWER(p.payeeName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY p.createdAt DESC")
    List<Payment> searchByPayeeName(@Param("search") String search);

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                    @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find failed payments for retry
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' " +
           "AND p.createdAt > :cutoffDate ORDER BY p.createdAt DESC")
    List<Payment> findFailedPaymentsForRetry(@Param("cutoffDate") LocalDateTime cutoffDate);
}
