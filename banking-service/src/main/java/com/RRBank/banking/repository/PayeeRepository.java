package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Payee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payee Repository
 * Phase 3: Data access for payees/beneficiaries
 */
@Repository
public interface PayeeRepository extends JpaRepository<Payee, UUID> {

    List<Payee> findByCustomerIdOrderByNicknameAsc(UUID customerId);

    List<Payee> findByCustomerIdAndStatus(UUID customerId, Payee.PayeeStatus status);

    Optional<Payee> findByCustomerIdAndPayeeAccountNumber(UUID customerId, String payeeAccountNumber);

    Optional<Payee> findByCustomerIdAndNickname(UUID customerId, String nickname);

    @Query("SELECT p FROM Payee p WHERE p.customerId = :customerId AND p.status = 'ACTIVE'")
    List<Payee> findActivePayeesByCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT p FROM Payee p WHERE p.customerId = :customerId AND p.isVerified = true")
    List<Payee> findVerifiedPayeesByCustomer(@Param("customerId") UUID customerId);

    @Query("SELECT p FROM Payee p WHERE p.status = 'PENDING_VERIFICATION'")
    List<Payee> findPendingVerification();

    boolean existsByCustomerIdAndPayeeAccountNumber(UUID customerId, String payeeAccountNumber);

    long countByCustomerId(UUID customerId);

    @Query("SELECT p FROM Payee p WHERE p.customerId = :customerId AND " +
           "(LOWER(p.nickname) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.payeeName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Payee> searchByCustomer(@Param("customerId") UUID customerId, @Param("search") String search);
}
