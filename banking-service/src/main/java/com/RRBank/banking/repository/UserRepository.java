package com.RRBank.banking.repository;

import com.RRBank.banking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository interface for User entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all users by role
     */
    List<User> findByRole(User.UserRole role);
    
    /**
     * Find all users by status
     */
    List<User> findByStatus(User.UserStatus status);
    
    /**
     * Find users by KYC verification status
     */
    List<User> findByKycVerified(Boolean kycVerified);
    
    /**
     * Custom query to find users with failed login attempts
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :attempts AND u.status = 'ACTIVE'")
    List<User> findUsersWithFailedLoginAttempts(@Param("attempts") Integer attempts);
    
    /**
     * Find active users created within a date range
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findActiveUsersCreatedBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
}
