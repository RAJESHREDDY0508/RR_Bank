package com.rrbank.customer.repository;

import com.rrbank.customer.entity.Customer;
import com.rrbank.customer.entity.Customer.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    
    // Stats queries
    long countByKycVerified(Boolean kycVerified);

    // KYC Status queries
    List<Customer> findByKycStatus(KycStatus kycStatus);
    Page<Customer> findByKycStatus(KycStatus kycStatus, Pageable pageable);
    long countByKycStatus(KycStatus kycStatus);
    
    // Migration query to set APPROVED for existing users without kycStatus
    @Modifying
    @Query("UPDATE Customer c SET c.kycStatus = 'APPROVED' WHERE c.kycStatus IS NULL AND c.kycVerified = true")
    int migrateVerifiedToApproved();
    
    @Modifying
    @Query("UPDATE Customer c SET c.kycStatus = 'PENDING' WHERE c.kycStatus IS NULL AND (c.kycVerified = false OR c.kycVerified IS NULL)")
    int migrateUnverifiedToPending();
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);
    
    // Count by created after date
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    // Search customers
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Customer> searchCustomers(@Param("search") String search, Pageable pageable);
}
