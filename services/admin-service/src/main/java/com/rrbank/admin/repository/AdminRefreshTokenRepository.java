package com.rrbank.admin.repository;

import com.rrbank.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {

    Optional<AdminRefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE AdminRefreshToken r SET r.revoked = true WHERE r.adminUserId = :adminUserId")
    void revokeAllByAdminUserId(@Param("adminUserId") UUID adminUserId);

    @Modifying
    @Query("DELETE FROM AdminRefreshToken r WHERE r.expiresAt < CURRENT_TIMESTAMP OR r.revoked = true")
    void deleteExpiredAndRevoked();

    long countByAdminUserIdAndRevokedFalse(UUID adminUserId);
}
