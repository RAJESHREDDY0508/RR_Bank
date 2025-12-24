package com.RRBank.banking.service;

import com.RRBank.banking.dto.NotificationResponseDto;
import com.RRBank.banking.entity.Notification;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notification Service
 * Business logic for managing notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;

    /**
     * Create and send notification
     */
    @Async
    @Transactional
    public void createAndSendNotification(
            UUID userId,
            Notification.NotificationType notificationType,
            Notification.NotificationChannel channel,
            String title,
            String message,
            UUID referenceId,
            String referenceType) {
        
        log.info("Creating notification for user: {}, type: {}, channel: {}", 
                userId, notificationType, channel);

        // Create notification record
        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(notificationType)
                .channel(channel)
                .title(title)
                .message(message)
                .status(Notification.NotificationStatus.PENDING)
                .isRead(false)
                .retryCount(0)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notification = notificationRepository.save(notification);

        // Send notification
        sendNotification(notification);
    }

    /**
     * Send notification through appropriate channel
     */
    @Async
    public void sendNotification(Notification notification) {
        log.info("Sending notification: {}, channel: {}", notification.getId(), notification.getChannel());

        boolean success = false;

        try {
            switch (notification.getChannel()) {
                case EMAIL -> {
                    // Get user email - in production, fetch from user service
                    String userEmail = getUserEmail(notification.getUserId());
                    success = emailService.sendEmail(userEmail, notification.getTitle(), notification.getMessage());
                }
                case SMS -> {
                    // Get user phone - in production, fetch from user service
                    String userPhone = getUserPhone(notification.getUserId());
                    success = smsService.sendSms(userPhone, notification.getMessage());
                }
                case PUSH -> {
                    // Get device token - in production, fetch from user preferences
                    String deviceToken = getDeviceToken(notification.getUserId());
                    success = pushNotificationService.sendPushNotification(
                            deviceToken, notification.getTitle(), notification.getMessage());
                }
                case IN_APP -> {
                    // In-app notifications are just stored in DB
                    success = true;
                }
            }

            if (success) {
                notification.markAsSent();
                log.info("Notification sent successfully: {}", notification.getId());
            } else {
                notification.markAsFailed("Failed to send notification");
                log.error("Notification failed: {}", notification.getId());
            }

        } catch (Exception e) {
            log.error("Error sending notification: {}", notification.getId(), e);
            notification.markAsFailed(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUserNotifications(UUID userId) {
        log.info("Fetching notifications for userId: {}", userId);
        
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(UUID userId) {
        log.info("Fetching unread notifications for userId: {}", userId);
        
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get recent notifications
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecentNotifications(UUID userId, int limit) {
        log.info("Fetching {} recent notifications for userId: {}", limit, userId);
        
        return notificationRepository.findRecentNotificationsByUser(userId, limit).stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public NotificationResponseDto markAsRead(UUID notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with ID: " + notificationId));
        
        notification.markAsRead();
        notification = notificationRepository.save(notification);
        
        return NotificationResponseDto.fromEntity(notification);
    }

    /**
     * Mark all notifications as read for user
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for userId: {}", userId);
        
        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }

    /**
     * Get unread count for user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Retry failed notifications
     */
    @Transactional
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications");
        
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(3); // Max 3 retries
        
        failedNotifications.forEach(this::sendNotification);
    }

    // ========== HELPER METHODS ==========

    /**
     * Get user email - Mock implementation
     * In production, fetch from Customer/User service
     */
    private String getUserEmail(UUID userId) {
        // Mock email - in production, call user service
        return "user-" + userId.toString().substring(0, 8) + "@example.com";
    }

    /**
     * Get user phone - Mock implementation
     */
    private String getUserPhone(UUID userId) {
        // Mock phone - in production, call customer service
        return "+1234567890";
    }

    /**
     * Get device token - Mock implementation
     */
    private String getDeviceToken(UUID userId) {
        // Mock device token - in production, fetch from user preferences
        return "device-token-" + userId.toString().substring(0, 8);
    }
}
