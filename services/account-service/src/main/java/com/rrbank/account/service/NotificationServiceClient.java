package com.rrbank.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.notification-url:http://localhost:8086}")
    private String notificationServiceUrl;

    public void sendAccountCreatedEmail(String userId, String email, String firstName,
                                        String accountType, String accountNumber) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("accountType", accountType);
            request.put("accountNumber", accountNumber);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/account-created")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Account created email sent for: {}", accountNumber),
                        error -> log.error("Failed to send account created email: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending account created email: {}", e.getMessage());
        }
    }

    public void sendLowBalanceAlert(String userId, String email, String firstName,
                                    String accountNumber, BigDecimal currentBalance,
                                    BigDecimal threshold) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("accountNumber", accountNumber);
            request.put("currentBalance", currentBalance);
            request.put("threshold", threshold);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/low-balance")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Low balance alert sent for account: {}", accountNumber),
                        error -> log.error("Failed to send low balance alert: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending low balance alert: {}", e.getMessage());
        }
    }
}
