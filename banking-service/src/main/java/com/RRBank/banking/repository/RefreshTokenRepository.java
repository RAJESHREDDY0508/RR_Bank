package com.RRBank.banking.repository;

import com.RRBank.banking.entity.RefreshToken;
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
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :hash " +
           "AND t.revoked = false AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<RefreshToken> findValidToken(@Param("hash") String tokenHash);

    @Query("SELECT t FROM RefreshToken t WHERE t.userId = :userId " +
           "AND t.revoked = false AND t.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY t.createdAt DESC")
    List<RefreshToken> findActiveTokensForUser(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllForUser(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE t.tokenHash = :hash AND t.revoked = false")
    int revokeByHash(@Param("hash") String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.userId = :userId " +
           "AND t.revoked = false AND t.expiresAt > CURRENT_TIMESTAMP")
    long countActiveSessionsForUser(@Param("userId") String userId);
}
