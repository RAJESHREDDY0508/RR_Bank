package com.RRBank.banking.repository;

import com.RRBank.banking.entity.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository interface for UserMfa entity
 */
@Repository
public interface UserMfaRepository extends JpaRepository<UserMfa, String> {
    
    /**
     * Find MFA settings by user ID
     */
    Optional<UserMfa> findByUserId(String userId);
    
    /**
     * Check if user has MFA enabled
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM UserMfa m " +
           "WHERE m.userId = :userId AND " +
           "((m.totpEnabled = true AND m.totpVerified = true) OR " +
           "(m.smsEnabled = true AND m.smsVerified = true) OR " +
           "(m.emailEnabled = true AND m.emailVerified = true))")
    boolean isMfaEnabledForUser(@Param("userId") String userId);
    
    /**
     * Find users with TOTP enabled
     */
    List<UserMfa> findByTotpEnabledTrueAndTotpVerifiedTrue();
    
    /**
     * Find users with SMS MFA enabled
     */
    List<UserMfa> findBySmsEnabledTrueAndSmsVerifiedTrue();
    
    /**
     * Find users with Email MFA enabled
     */
    List<UserMfa> findByEmailEnabledTrueAndEmailVerifiedTrue();
    
    /**
     * Delete MFA settings by user ID
     */
    void deleteByUserId(String userId);
}
