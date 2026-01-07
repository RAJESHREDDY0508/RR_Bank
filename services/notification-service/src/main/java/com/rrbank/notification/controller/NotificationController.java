package com.rrbank.notification.controller;

import com.rrbank.notification.dto.NotificationDTOs.*;
import com.rrbank.notification.entity.Notification;
import com.rrbank.notification.service.EmailService;
import com.rrbank.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(@RequestHeader("X-User-Id") String userId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(UUID.fromString(userId), pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(UUID.fromString(userId)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") String userId) {
        long count = notificationService.getUnreadCount(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Id") String userId) {
        notificationService.markAllAsRead(UUID.fromString(userId));
        return ResponseEntity.ok().build();
    }

    // ============ Email Notification Endpoints (Internal API) ============

    @PostMapping("/email/welcome")
    public ResponseEntity<Void> sendWelcomeEmail(@Valid @RequestBody WelcomeEmailRequest request) {
        log.info("Sending welcome email to: {}", request.getEmail());
        emailService.sendWelcomeEmail(request.getEmail(), request.getFirstName());
        
        // Also create in-app notification
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            "Welcome to RR Bank! 🎉",
            "Your account has been created successfully. Start exploring your dashboard!",
            Notification.NotificationType.ACCOUNT
        );
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/transaction")
    public ResponseEntity<Void> sendTransactionEmail(@Valid @RequestBody TransactionEmailRequest request) {
        log.info("Sending transaction email to: {} for type: {}", request.getEmail(), request.getTransactionType());
        
        emailService.sendTransactionEmail(
            request.getEmail(),
            request.getFirstName(),
            request.getTransactionType(),
            request.getAmount(),
            request.getAccountNumber(),
            request.getDescription(),
            request.getNewBalance(),
            request.getTransactionRef()
        );
        
        // Create in-app notification
        String title = getTransactionNotificationTitle(request.getTransactionType(), request.getAmount());
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            title,
            request.getDescription() + " - Ref: " + request.getTransactionRef(),
            Notification.NotificationType.TRANSACTION
        );
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/transfer-received")
    public ResponseEntity<Void> sendTransferReceivedEmail(@Valid @RequestBody TransferReceivedEmailRequest request) {
        log.info("Sending transfer received email to: {}", request.getEmail());
        
        emailService.sendTransferReceivedEmail(
            request.getEmail(),
            request.getFirstName(),
            request.getAmount(),
            request.getFromAccount(),
            request.getToAccountNumber(),
            request.getNewBalance(),
            request.getTransactionRef()
        );
        
        // Create in-app notification
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            "💰 Money Received: $" + request.getAmount(),
            "Transfer from " + (request.getFromAccount() != null ? request.getFromAccount() : "external account"),
            Notification.NotificationType.TRANSACTION
        );
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/security-alert")
    public ResponseEntity<Void> sendSecurityAlert(@Valid @RequestBody SecurityAlertRequest request) {
        log.info("Sending security alert to: {} for type: {}", request.getEmail(), request.getAlertType());
        
        emailService.sendSecurityAlert(
            request.getEmail(),
            request.getFirstName(),
            request.getAlertType(),
            request.getDeviceInfo(),
            request.getIpAddress(),
            request.getLocation()
        );
        
        // Create in-app notification
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            "🔐 Security Alert: " + request.getAlertType(),
            "From " + request.getIpAddress() + " - " + (request.getLocation() != null ? request.getLocation() : "Unknown location"),
            Notification.NotificationType.SECURITY
        );
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/password-reset")
    public ResponseEntity<Void> sendPasswordResetEmail(@Valid @RequestBody PasswordResetEmailRequest request) {
        log.info("Sending password reset email to: {}", request.getEmail());
        emailService.sendPasswordResetEmail(request.getEmail(), request.getFirstName(), request.getResetToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/account-created")
    public ResponseEntity<Void> sendAccountCreatedEmail(@Valid @RequestBody AccountCreatedEmailRequest request) {
        log.info("Sending account created email to: {}", request.getEmail());
        
        emailService.sendAccountCreatedEmail(
            request.getEmail(),
            request.getFirstName(),
            request.getAccountType(),
            request.getAccountNumber()
        );
        
        // Create in-app notification
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            "🎊 New " + request.getAccountType() + " Account Created!",
            "Account number: " + request.getAccountNumber(),
            Notification.NotificationType.ACCOUNT
        );
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/low-balance")
    public ResponseEntity<Void> sendLowBalanceAlert(@Valid @RequestBody LowBalanceAlertRequest request) {
        log.info("Sending low balance alert to: {}", request.getEmail());
        
        emailService.sendLowBalanceAlert(
            request.getEmail(),
            request.getFirstName(),
            request.getAccountNumber(),
            request.getCurrentBalance(),
            request.getThreshold()
        );
        
        // Create in-app notification
        notificationService.createNotification(
            UUID.fromString(request.getUserId()),
            "⚠️ Low Balance Alert",
            "Your account " + request.getAccountNumber() + " balance is $" + request.getCurrentBalance(),
            Notification.NotificationType.ALERT
        );
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }

    private String getTransactionNotificationTitle(String type, java.math.BigDecimal amount) {
        return switch (type.toUpperCase()) {
            case "DEPOSIT" -> "✅ Deposit: +$" + amount;
            case "WITHDRAWAL" -> "💸 Withdrawal: -$" + amount;
            case "TRANSFER" -> "↗️ Transfer: -$" + amount;
            default -> "Transaction: $" + amount;
        };
    }
}
