package com.RRBank.banking.repository;

import com.RRBank.banking.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :hash " +
           "AND t.used = false AND t.expiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetToken> findValidToken(@Param("hash") String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true, t.usedAt = CURRENT_TIMESTAMP " +
           "WHERE t.userId = :userId AND t.used = false")
    int invalidateAllForUser(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.userId = :userId " +
           "AND t.createdAt > :since")
    long countRecentTokensForUser(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
}
