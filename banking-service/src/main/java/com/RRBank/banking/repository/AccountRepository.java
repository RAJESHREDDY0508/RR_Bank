package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account Repository
 * Data access layer for Account entity
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find account by account number
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find all accounts for a customer
     */
    List<Account> findByCustomerId(UUID customerId);

    /**
     * Find accounts by customer ID and status
     */
    List<Account> findByCustomerIdAndStatus(UUID customerId, Account.AccountStatus status);

    /**
     * Find accounts by status
     */
    List<Account> findByStatus(Account.AccountStatus status);

    /**
     * Find accounts by type
     */
    List<Account> findByAccountType(Account.AccountType accountType);

    /**
     * Find active accounts for customer
     */
    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByCustomer(@Param("customerId") UUID customerId);

    /**
     * Check if account exists for customer
     */
    boolean existsByCustomerIdAndAccountType(UUID customerId, Account.AccountType accountType);

    /**
     * Count accounts by customer
     */
    long countByCustomerId(UUID customerId);

    /**
     * Count accounts by status
     */
    long countByStatus(Account.AccountStatus status);

    /**
     * Get total balance for customer
     */
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.customerId = :customerId AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByCustomer(@Param("customerId") UUID customerId);

    /**
     * Find account with pessimistic lock (for transactions)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    /**
     * Find account by account number with lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /**
     * Search accounts by customer name or account number
     */
    @Query("SELECT a FROM Account a WHERE " +
           "LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Account> searchAccounts(@Param("search") String search);

    /**
     * Find accounts with balance greater than amount
     */
    @Query("SELECT a FROM Account a WHERE a.balance > :amount AND a.status = 'ACTIVE'")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("amount") BigDecimal amount);

    /**
     * Find accounts with low balance (less than minimum)
     */
    @Query("SELECT a FROM Account a WHERE a.balance < :minBalance AND a.status = 'ACTIVE' AND a.accountType != 'CREDIT'")
    List<Account> findLowBalanceAccounts(@Param("minBalance") BigDecimal minBalance);
}
