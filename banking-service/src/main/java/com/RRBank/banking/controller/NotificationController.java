package com.RRBank.banking.controller;

import com.RRBank.banking.dto.NotificationResponseDto;
import com.RRBank.banking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Notification Controller
 * REST API endpoints for notification management
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for a user
     * GET /api/notifications/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getUserNotifications(@PathVariable UUID userId) {
        log.info("REST request to get notifications for userId: {}", userId);
        List<NotificationResponseDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for a user
     * GET /api/notifications/user/{userId}/unread
     */
    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications(@PathVariable UUID userId) {
        log.info("REST request to get unread notifications for userId: {}", userId);
        List<NotificationResponseDto> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get recent notifications for a user
     * GET /api/notifications/user/{userId}/recent?limit=10
     */
    @GetMapping("/user/{userId}/recent")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getRecentNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get {} recent notifications for userId: {}", limit, userId);
        List<NotificationResponseDto> notifications = notificationService.getRecentNotifications(userId, limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread count for a user
     * GET /api/notifications/user/{userId}/unread/count
     */
    @GetMapping("/user/{userId}/unread/count")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getUnreadCount(@PathVariable UUID userId) {
        log.info("REST request to get unread count for userId: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable UUID id) {
        log.info("REST request to mark notification as read: {}", id);
        NotificationResponseDto notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark all notifications as read for a user
     * PUT /api/notifications/user/{userId}/read-all
     */
    @PutMapping("/user/{userId}/read-all")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
        log.info("REST request to mark all notifications as read for userId: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
