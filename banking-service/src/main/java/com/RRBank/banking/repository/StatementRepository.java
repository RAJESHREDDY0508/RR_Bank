package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Statement Repository
 */
@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {

    /**
     * Find all statements for an account
     */
    List<Statement> findByAccountIdOrderByPeriodEndDateDesc(UUID accountId);

    /**
     * Find all statements for a customer
     */
    List<Statement> findByCustomerIdOrderByPeriodEndDateDesc(UUID customerId);

    /**
     * Find statement by account and period
     */
    Optional<Statement> findByAccountIdAndStatementPeriod(UUID accountId, String statementPeriod);

    /**
     * Find statements by status
     */
    List<Statement> findByStatusOrderByCreatedAtDesc(Statement.StatementStatus status);

    /**
     * Find statements by type
     */
    List<Statement> findByStatementTypeOrderByPeriodEndDateDesc(Statement.StatementType statementType);

    /**
     * Find pending statements for generation
     */
    @Query("SELECT s FROM Statement s WHERE s.status = 'PENDING' ORDER BY s.createdAt ASC")
    List<Statement> findPendingStatements();

    /**
     * Find statements by date range
     */
    @Query("SELECT s FROM Statement s WHERE s.periodStartDate >= :startDate AND s.periodEndDate <= :endDate " +
           "ORDER BY s.periodEndDate DESC")
    List<Statement> findByDateRange(@Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate);

    /**
     * Find statements for account by date range
     */
    @Query("SELECT s FROM Statement s WHERE s.accountId = :accountId " +
           "AND s.periodStartDate >= :startDate AND s.periodEndDate <= :endDate " +
           "ORDER BY s.periodEndDate DESC")
    List<Statement> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * Check if statement exists for period
     */
    boolean existsByAccountIdAndStatementPeriod(UUID accountId, String statementPeriod);

    /**
     * Count statements by status
     */
    long countByStatus(Statement.StatementStatus status);
}
