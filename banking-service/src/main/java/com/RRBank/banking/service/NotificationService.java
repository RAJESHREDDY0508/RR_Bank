package com.RRBank.banking.service;

import com.RRBank.banking.dto.NotificationResponseDto;
import com.RRBank.banking.entity.Notification;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(notificationType)
                .channel(channel)
                .title(title)
                .message(message)
                .status(Notification.NotificationStatus.PENDING)
                .isRead(false)
                .retryCount(0)
                .referenceId(referenceId != null ? referenceId.toString() : null)
                .referenceType(referenceType)
                .build();

        notification = notificationRepository.save(notification);

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
                    String userEmail = getUserEmail(notification.getUserId());
                    success = emailService.sendEmail(
                            userEmail,
                            notification.getTitle(),
                            notification.getMessage()
                    );
                }
                case SMS -> {
                    String userPhone = getUserPhone(notification.getUserId());
                    success = smsService.sendSms(userPhone, notification.getMessage());
                }
                case PUSH -> {
                    String deviceToken = getDeviceToken(notification.getUserId());
                    success = pushNotificationService.sendPushNotification(
                            deviceToken,
                            notification.getTitle(),
                            notification.getMessage()
                    );
                }
                case IN_APP -> success = true;
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

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(UUID userId) {
        log.info("Fetching unread notifications for userId: {}", userId);

        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ✅ FIXED: Get recent notifications using Pageable
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecentNotifications(UUID userId, int limit) {
        log.info("Fetching {} recent notifications for userId: {}", limit, userId);

        Pageable pageable = PageRequest.of(0, limit);

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found with ID: " + notificationId)
                );

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

        List<Notification> notifications =
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }

    /**
     * Get unread notification count
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

        List<Notification> failedNotifications =
                notificationRepository.findFailedNotificationsForRetry(3);

        failedNotifications.forEach(this::sendNotification);
    }

    /* ------------------------------------------------------------------
       Mock helpers (replace with real User/Profile service later)
       ------------------------------------------------------------------ */

    private String getUserEmail(UUID userId) {
        return "user-" + userId.toString().substring(0, 8) + "@example.com";
    }

    private String getUserPhone(UUID userId) {
        return "+1234567890";
    }

    private String getDeviceToken(UUID userId) {
        return "device-token-" + userId.toString().substring(0, 8);
    }
}
