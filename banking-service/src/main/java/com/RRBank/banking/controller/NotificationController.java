package com.RRBank.banking.controller;

import com.RRBank.banking.dto.NotificationResponseDto;
import com.RRBank.banking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Notification Controller
 * REST API endpoints for notification management
 * 
 * ✅ FIXED: Added /me convenience endpoints for frontend compatibility
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== /ME CONVENIENCE ENDPOINTS (for frontend) ====================

    /**
     * Get all notifications for the authenticated user
     * GET /api/notifications
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to get notifications for authenticated user: {}", userId);
        List<NotificationResponseDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for the authenticated user
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getMyUnreadNotifications(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to get unread notifications for authenticated user: {}", userId);
        List<NotificationResponseDto> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread count for the authenticated user
     * GET /api/notifications/unread/count
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Long> getMyUnreadCount(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to get unread count for authenticated user: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark all notifications as read for the authenticated user
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markAllMyNotificationsAsRead(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("REST request to mark all notifications as read for authenticated user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    // ==================== USER-ID BASED ENDPOINTS (for admin/backward compatibility) ====================

    /**
     * Get all notifications for a user
     * GET /api/notifications/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDto>> getUserNotifications(
            @PathVariable UUID userId,
            Authentication authentication) {
        // Ownership check: only allow if user is admin or accessing own notifications
        validateOwnership(userId, authentication);
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
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications(
            @PathVariable UUID userId,
            Authentication authentication) {
        validateOwnership(userId, authentication);
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
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        validateOwnership(userId, authentication);
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
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID userId,
            Authentication authentication) {
        validateOwnership(userId, authentication);
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
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable UUID userId,
            Authentication authentication) {
        validateOwnership(userId, authentication);
        log.info("REST request to mark all notifications as read for userId: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract user ID from authentication
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof com.RRBank.banking.security.CustomUserDetails userDetails) {
            return UUID.fromString(userDetails.getUserId());
        }
        
        if (principal instanceof String principalString) {
            return UUID.fromString(principalString);
        }
        
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }

    /**
     * Validate that the authenticated user can access notifications for the given userId
     */
    private void validateOwnership(UUID targetUserId, Authentication authentication) {
        UUID authenticatedUserId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && !authenticatedUserId.equals(targetUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to access notifications for this user");
        }
    }
}
