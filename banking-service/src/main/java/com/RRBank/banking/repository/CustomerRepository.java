package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Customer Repository
 * Data access layer for Customer entity
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Find customer by user ID
     */
    Optional<Customer> findByUserId(UUID userId);

    /**
     * Find customer by phone number
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * Find all customers by KYC status
     */
    List<Customer> findByKycStatus(Customer.KycStatus kycStatus);

    /**
     * Find customers by city
     */
    List<Customer> findByCity(String city);

    /**
     * Find customers by state
     */
    List<Customer> findByState(String state);

    /**
     * Find customers by country
     */
    List<Customer> findByCountry(String country);

    /**
     * Search customers by name (case-insensitive)
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Customer> searchByName(@Param("search") String search);

    /**
     * Check if customer exists for user
     */
    boolean existsByUserId(UUID userId);

    /**
     * Count customers by KYC status
     */
    long countByKycStatus(Customer.KycStatus kycStatus);

    /**
     * Find pending KYC customers
     */
    @Query("SELECT c FROM Customer c WHERE c.kycStatus = 'PENDING' OR c.kycStatus = 'IN_PROGRESS'")
    List<Customer> findPendingKycCustomers();
}
