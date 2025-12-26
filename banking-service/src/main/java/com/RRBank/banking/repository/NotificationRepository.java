package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            UUID userId,
            Notification.NotificationType notificationType
    );

    List<Notification> findByStatusOrderByCreatedAtDesc(
            Notification.NotificationStatus status
    );

    @Query("""
           SELECT n FROM Notification n
           WHERE n.status = 'PENDING'
           ORDER BY n.createdAt ASC
           """)
    List<Notification> findPendingNotifications();

    @Query("""
           SELECT n FROM Notification n
           WHERE n.status = 'FAILED'
           AND n.retryCount < :maxRetries
           ORDER BY n.createdAt ASC
           """)
    List<Notification> findFailedNotificationsForRetry(
            @Param("maxRetries") int maxRetries
    );

    List<Notification> findByReferenceIdAndReferenceType(
            String referenceId,
            String referenceType
    );

    // ✅ THIS replaces findRecentNotificationsByUser
    List<Notification> findByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );

    long countByUserIdAndIsReadFalse(UUID userId);

    long countByStatus(Notification.NotificationStatus status);

    @Query("""
           SELECT n FROM Notification n
           WHERE n.createdAt BETWEEN :startDate AND :endDate
           ORDER BY n.createdAt DESC
           """)
    List<Notification> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
           SELECT n FROM Notification n
           WHERE n.userId = :userId
           AND n.createdAt BETWEEN :startDate AND :endDate
           ORDER BY n.createdAt DESC
           """)
    List<Notification> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
           DELETE FROM Notification n
           WHERE n.isRead = true
           AND n.createdAt < :cutoffDate
           """)
    void deleteOldReadNotifications(
            @Param("cutoffDate") LocalDateTime cutoffDate
    );
}
