package com.RRBank.banking.repository;

import com.RRBank.banking.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Notification Repository
 * Data access layer for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a user
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find unread notifications for a user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Find notifications by user and type
     */
    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            UUID userId, Notification.NotificationType notificationType);

    /**
     * Find notifications by status
     */
    List<Notification> findByStatusOrderByCreatedAtDesc(Notification.NotificationStatus status);

    /**
     * Find pending notifications (not sent yet)
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();

    /**
     * Find failed notifications for retry
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries " +
           "ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Find notifications by reference
     */
    List<Notification> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType);

    /**
     * Find recent notifications for user
     */
    @Query(value = "SELECT n FROM Notification n WHERE n.userId = :userId " +
                   "ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotificationsByUser(@Param("userId") UUID userId, 
                                                     @Param("limit") int limit);

    /**
     * Count unread notifications for user
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Count notifications by status
     */
    long countByStatus(Notification.NotificationStatus status);

    /**
     * Find notifications by date range
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find notifications by user and date range
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Delete old read notifications
     */
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
