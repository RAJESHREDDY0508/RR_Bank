package com.RRBank.banking.repository;

import com.RRBank.banking.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    List<LoginHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<LoginHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT h FROM LoginHistory h WHERE h.username = :username " +
           "AND h.success = false AND h.createdAt > :since " +
           "ORDER BY h.createdAt DESC")
    List<LoginHistory> findRecentFailedAttempts(
        @Param("username") String username,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(h) FROM LoginHistory h WHERE h.ipAddress = :ip " +
           "AND h.success = false AND h.createdAt > :since")
    long countFailedAttemptsFromIp(
        @Param("ip") String ipAddress,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT CASE WHEN COUNT(h) = 0 THEN true ELSE false END FROM LoginHistory h " +
           "WHERE h.userId = :userId AND h.success = true " +
           "AND (h.ipAddress = :ip OR h.deviceFingerprint = :device)")
    boolean isNewDevice(
        @Param("userId") String userId,
        @Param("ip") String ipAddress,
        @Param("device") String deviceFingerprint
    );

    @Query("SELECT h FROM LoginHistory h WHERE h.userId = :userId " +
           "AND h.success = true ORDER BY h.createdAt DESC")
    Page<LoginHistory> findSuccessfulLogins(@Param("userId") String userId, Pageable pageable);
}
