package com.rrbank.transaction.repository;

import com.rrbank.transaction.entity.Transaction;
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

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    // Stats queries
    long countByStatus(Transaction.TransactionStatus status);
    long countByTransactionType(Transaction.TransactionType type);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByType(@Param("type") Transaction.TransactionType type);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :since AND t.status = 'COMPLETED'")
    BigDecimal sumAmountSince(@Param("since") LocalDateTime since);
    
    // Paginated queries for admin
    @Query("SELECT t FROM Transaction t WHERE CAST(t.status AS string) = :status")
    Page<Transaction> findByStatusString(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE CAST(t.transactionType AS string) = :type")
    Page<Transaction> findByTypeString(@Param("type") String type, Pageable pageable);
    
    Optional<Transaction> findByTransactionReference(String reference);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            UUID fromAccountId, UUID toAccountId, Pageable pageable);
    
    Page<Transaction> findByInitiatedByOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    // Date range filtering
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // Date range filtering with transaction type
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "AND (:type IS NULL OR t.transactionType = :type) ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRangeAndType(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("type") Transaction.TransactionType type,
            Pageable pageable);
    
    // For export - get all transactions without pagination
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
