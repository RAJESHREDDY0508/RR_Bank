package com.rrbank.account.repository;

import com.rrbank.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(UUID userId);
    List<Account> findByCustomerId(UUID customerId);
    List<Account> findByUserIdAndStatus(UUID userId, Account.AccountStatus status);
    boolean existsByAccountNumber(String accountNumber);
    
    // Paginated queries
    Page<Account> findByStatus(String status, Pageable pageable);
    Page<Account> findByAccountType(String accountType, Pageable pageable);
    
    @Query("SELECT a FROM Account a WHERE " +
           "LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Account> searchAccounts(@Param("search") String search, Pageable pageable);
    
    // Stats queries
    long countByStatus(String status);
    long countByAccountType(String accountType);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatusEnum(@Param("status") Account.AccountStatus status);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountType = :type")
    long countByAccountTypeEnum(@Param("type") Account.AccountType type);
}
