package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction Repository Data access layer for Transaction entity
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find transaction by reference
     */
    Optional<Transaction> findByTransactionReference(String transactionReference);

    /**
     * Find transaction by idempotency key (for duplicate prevention)
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Count transactions created after a specific date
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // ========== PAGINATED METHODS ✅ (FOR PRODUCTION USE) ==========
    /**
     * Find all transactions for an account (as sender or receiver) - with
     * pagination
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    /**
     * Find transactions by status - with pagination
     */
    Page<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by type - with pagination
     */
    Page<Transaction> findByTransactionTypeOrderByCreatedAtDesc(Transaction.TransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by date range - with pagination
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find transactions by account and date range - with pagination
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) "
            + "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find transactions by account and status - with pagination
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) "
            + "AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndStatus(@Param("accountId") UUID accountId,
            @Param("status") Transaction.TransactionStatus status,
            Pageable pageable);

    // ========== NON-PAGINATED METHODS (DEPRECATED - Use paginated versions above) ==========
    /**
     * Find all transactions for an account (as sender or receiver)
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") UUID accountId);

    /**
     * Find transactions where account is sender
     */
    List<Transaction> findByFromAccountIdOrderByCreatedAtDesc(UUID fromAccountId);

    /**
     * Find transactions where account is receiver
     */
    List<Transaction> findByToAccountIdOrderByCreatedAtDesc(UUID toAccountId);

    /**
     * Find transactions by status
     */
    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.TransactionStatus status);

    /**
     * Find transactions by type
     */
    List<Transaction> findByTransactionTypeOrderByCreatedAtDesc(Transaction.TransactionType transactionType);

    /**
     * Find transactions by date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find transactions by account and date range
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) "
            + "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find transactions by account and status
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) "
            + "AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountIdAndStatus(@Param("accountId") UUID accountId,
            @Param("status") Transaction.TransactionStatus status);

    /**
     * Find pending transactions older than specified time
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :cutoffTime")
    List<Transaction> findStaleTransactions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count transactions by account
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    long countByAccountId(@Param("accountId") UUID accountId);

    /**
     * Count transactions by status
     */
    long countByStatus(Transaction.TransactionStatus status);

    /**
     * Get total transfer amount for account (outgoing)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.fromAccountId = :accountId AND t.status = 'COMPLETED'")
    BigDecimal getTotalOutgoingAmount(@Param("accountId") UUID accountId);

    /**
     * Get total transfer amount for account (incoming)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.toAccountId = :accountId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomingAmount(@Param("accountId") UUID accountId);

    /**
     * Find recent transactions for account (last N)
     */
    @Query(value = "SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId "
            + "ORDER BY t.createdAt DESC LIMIT :limit")
    List<Transaction> findRecentTransactionsByAccountId(@Param("accountId") UUID accountId,
            @Param("limit") int limit);

    /**
     * Search transactions by description
     */
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "ORDER BY t.createdAt DESC")
    List<Transaction> searchByDescription(@Param("search") String search);

    /**
     * Find transactions by amount range
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount BETWEEN :minAmount AND :maxAmount "
            + "ORDER BY t.createdAt DESC")
    List<Transaction> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Check if idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
