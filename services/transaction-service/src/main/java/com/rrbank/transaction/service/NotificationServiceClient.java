package com.rrbank.transaction.service;

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

    public void sendTransactionNotification(String userId, String email, String firstName,
                                            String transactionType, BigDecimal amount,
                                            String accountNumber, String description,
                                            BigDecimal newBalance, String transactionRef) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("transactionType", transactionType);
            request.put("amount", amount);
            request.put("accountNumber", accountNumber);
            request.put("description", description);
            request.put("newBalance", newBalance);
            request.put("transactionRef", transactionRef);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/transaction")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Transaction notification sent for: {}", transactionRef),
                        error -> log.error("Failed to send transaction notification: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending transaction notification: {}", e.getMessage());
        }
    }

    public void sendTransferReceivedNotification(String userId, String email, String firstName,
                                                  BigDecimal amount, String fromAccount,
                                                  String toAccountNumber, BigDecimal newBalance,
                                                  String transactionRef) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("firstName", firstName);
            request.put("amount", amount);
            request.put("fromAccount", fromAccount);
            request.put("toAccountNumber", toAccountNumber);
            request.put("newBalance", newBalance);
            request.put("transactionRef", transactionRef);

            webClientBuilder.baseUrl(notificationServiceUrl).build()
                    .post()
                    .uri("/api/notifications/email/transfer-received")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                        result -> log.info("Transfer received notification sent for: {}", transactionRef),
                        error -> log.error("Failed to send transfer received notification: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending transfer received notification: {}", e.getMessage());
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
            request.put("deviceInfo", deviceInfo);
            request.put("ipAddress", ipAddress);
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
}
