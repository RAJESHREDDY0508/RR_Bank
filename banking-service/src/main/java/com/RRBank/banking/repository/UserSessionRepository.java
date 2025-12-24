package com.RRBank.banking.repository;

import com.RRBank.banking.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for UserSession entity
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    /**
     * Find session by session token
     */
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    /**
     * Find all active sessions for a user
     */
    List<UserSession> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);
    
    /**
     * Find trusted devices for a user
     */
    List<UserSession> findByUserIdAndIsTrustedTrueOrderByCreatedAtDesc(String userId);
    
    /**
     * Find session by device ID
     */
    Optional<UserSession> findByUserIdAndDeviceIdAndIsActiveTrue(String userId, String deviceId);
    
    /**
     * Count active sessions for a user
     */
    long countByUserIdAndIsActiveTrue(String userId);
    
    /**
     * Find sessions by IP address
     */
    List<UserSession> findByUserIdAndIpAddress(String userId, String ipAddress);
    
    /**
     * Terminate all sessions for a user
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.terminatedAt = :now, " +
           "s.terminationReason = :reason WHERE s.userId = :userId AND s.isActive = true")
    int terminateAllSessionsForUser(
        @Param("userId") String userId, 
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );
    
    /**
     * Terminate all sessions except current
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.terminatedAt = :now, " +
           "s.terminationReason = :reason WHERE s.userId = :userId AND s.isActive = true " +
           "AND s.sessionToken != :currentToken")
    int terminateOtherSessions(
        @Param("userId") String userId,
        @Param("currentToken") String currentToken,
        @Param("now") LocalDateTime now,
        @Param("reason") String reason
    );
    
    /**
     * Delete expired sessions
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * Find inactive sessions (no activity for specified hours)
     */
    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.lastActivityAt < :since")
    List<UserSession> findInactiveSessions(@Param("since") LocalDateTime since);
    
    /**
     * Check if device is trusted
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSession s " +
           "WHERE s.userId = :userId AND s.deviceFingerprint = :fingerprint AND s.isTrusted = true")
    boolean isDeviceTrusted(@Param("userId") String userId, @Param("fingerprint") String fingerprint);
}
