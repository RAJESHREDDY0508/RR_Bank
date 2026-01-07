package com.rrbank.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.notification-url:http://localhost:8086}")
    private String notificationServiceUrl;

    public void sendWelcomeEmail(String userId, String email, String firstName) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/welcome")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Welcome email sent for user: {}", userId),
                        error -> log.error("Failed to send welcome email: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending welcome email: {}", e.getMessage());
        }
    }

    public void sendSecurityAlert(String userId, String email, String firstName,
                                  String alertType, String deviceInfo,
                                  String ipAddress, String location) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("alertType", alertType);
            request.put("deviceInfo", deviceInfo != null ? deviceInfo : "Unknown");
            request.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
            request.put("location", location);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/security-alert")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Security alert sent for user: {}", userId),
                        error -> log.error("Failed to send security alert: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending security alert: {}", e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String email, String firstName, String resetToken) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("resetToken", resetToken);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/password-reset")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Password reset email sent to: {}", email),
                        error -> log.error("Failed to send password reset email: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
        }
    }
}
