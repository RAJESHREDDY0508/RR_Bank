package com.RRBank.banking.repository;

import com.RRBank.banking.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for OtpCode entity
 */
@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {
    
    /**
     * Find valid OTP by user ID, type, and purpose
     */
    @Query("SELECT o FROM OtpCode o WHERE o.userId = :userId AND o.otpType = :otpType " +
           "AND o.purpose = :purpose AND o.verified = false AND o.expiresAt > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpCode> findValidOtp(
        @Param("userId") String userId, 
        @Param("otpType") OtpCode.OtpType otpType,
        @Param("purpose") OtpCode.OtpPurpose purpose,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Find OTP by code and user ID
     */
    Optional<OtpCode> findByCodeAndUserId(String code, String userId);
    
    /**
     * Find all valid OTPs for a user
     */
    @Query("SELECT o FROM OtpCode o WHERE o.userId = :userId AND o.verified = false " +
           "AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<OtpCode> findAllValidOtpsForUser(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    /**
     * Find recent OTPs for rate limiting
     */
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.userId = :userId AND o.otpType = :otpType " +
           "AND o.createdAt > :since")
    long countRecentOtps(
        @Param("userId") String userId, 
        @Param("otpType") OtpCode.OtpType otpType,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Delete expired OTPs
     */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    int deleteExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * Invalidate all OTPs for a user (except verified ones)
     */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.userId = :userId AND o.verified = false")
    int invalidateAllOtpsForUser(@Param("userId") String userId);
    
    /**
     * Find OTPs by purpose
     */
    List<OtpCode> findByUserIdAndPurposeOrderByCreatedAtDesc(String userId, OtpCode.OtpPurpose purpose);
}
