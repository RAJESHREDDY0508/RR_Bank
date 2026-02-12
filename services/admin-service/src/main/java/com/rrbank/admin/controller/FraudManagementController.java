package com.rrbank.admin.controller;

import com.rrbank.admin.dto.ManagementDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.dto.common.PageResponse;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.security.AdminUserDetails;
import com.rrbank.admin.service.AuditLogService;
import com.rrbank.admin.service.ServiceClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/fraud")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Management", description = "Manage fraud alerts and suspicious activities")
public class FraudManagementController {

    private final ServiceClientService serviceClient;
    private final AuditLogService auditLogService;

    @GetMapping("/alerts")
    @Operation(summary = "List fraud alerts", description = "Get paginated list of fraud alerts")
    public ResponseEntity<ApiResponse<PageResponse<FraudAlertResponse>>> getFraudAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity
    ) {
        log.info("Admin getting fraud alerts - page: {}, size: {}, status: {}", page, size, status);
        
        Map<String, Object> stats = serviceClient.getFraudStats();
        
        // For now, return mock alerts based on stats
        List<FraudAlertResponse> alerts = generateAlertsFromStats(stats);
        
        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            alerts = alerts.stream()
                    .filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .toList();
        }
        
        // Filter by severity if provided
        if (severity != null && !severity.isEmpty()) {
            alerts = alerts.stream()
                    .filter(a -> a.getSeverity().equalsIgnoreCase(severity))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(alerts, page, size, alerts.size())));
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Get fraud alert details", description = "Get detailed information about a fraud alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> getFraudAlert(@PathVariable UUID id) {
        log.info("Admin getting fraud alert: {}", id);
        
        FraudAlertResponse alert = FraudAlertResponse.builder()
                .id(id)
                .alertType("SUSPICIOUS_TRANSACTION")
                .severity("MEDIUM")
                .status("PENDING")
                .description("Suspicious transaction pattern detected")
                .accountId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .riskScore(65)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    @PostMapping("/alerts/{id}/review")
    @Operation(summary = "Review fraud alert", description = "Mark a fraud alert as reviewed")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> reviewAlert(
            @PathVariable UUID id,
            @RequestBody ReviewAlertRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} reviewing fraud alert {}: {}", admin.getUsername(), id, request.getDecision());

        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "REVIEW_FRAUD_ALERT",
                request.getDecision().equalsIgnoreCase("APPROVE") ? 
                        AdminAuditLog.ActionType.APPROVE : AdminAuditLog.ActionType.REJECT,
                "FRAUD_ALERT",
                id.toString(),
                "Fraud alert reviewed: " + request.getDecision() + ". Notes: " + request.getNotes(),
                "PENDING",
                request.getDecision().toUpperCase(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        FraudAlertResponse response = FraudAlertResponse.builder()
                .id(id)
                .status(request.getDecision().equalsIgnoreCase("DISMISS") ? "DISMISSED" : "REVIEWED")
                .reviewedBy(admin.getUsername())
                .reviewedAt(LocalDateTime.now())
                .reviewNotes(request.getNotes())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Alert reviewed", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get fraud statistics", description = "Get fraud detection statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFraudStats() {
        log.info("Admin getting fraud stats");
        
        Map<String, Object> stats = serviceClient.getFraudStats();
        
        if (stats == null || stats.isEmpty()) {
            stats = new HashMap<>();
            stats.put("pendingAlerts", 0L);
            stats.put("reviewRequired", 0L);
            stats.put("blockedTransactions", 0L);
            stats.put("alertsToday", 0L);
            stats.put("alertsThisWeek", 0L);
        }
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/user/{userId}/limits")
    @Operation(summary = "Get user fraud limits", description = "Get fraud limits for a user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserLimits(@PathVariable UUID userId) {
        log.info("Admin getting fraud limits for user: {}", userId);
        
        Map<String, Object> limits = serviceClient.getUserFraudLimits(userId.toString());
        
        if (limits == null || limits.isEmpty()) {
            limits = new HashMap<>();
            limits.put("userId", userId.toString());
            limits.put("dailyLimit", new BigDecimal("10000.00"));
            limits.put("dailyUsed", BigDecimal.ZERO);
            limits.put("remainingDaily", new BigDecimal("10000.00"));
            limits.put("maxWithdrawalsPerHour", 5);
            limits.put("withdrawalsThisHour", 0);
        }
        
        return ResponseEntity.ok(ApiResponse.success(limits));
    }

    // Helper methods
    private List<FraudAlertResponse> generateAlertsFromStats(Map<String, Object> stats) {
        List<FraudAlertResponse> alerts = new ArrayList<>();
        
        long pendingAlerts = stats != null ? getLongValue(stats, "pendingAlerts", 0L) : 0L;
        
        // Generate sample alerts based on stats
        for (int i = 0; i < Math.min(pendingAlerts, 10); i++) {
            alerts.add(FraudAlertResponse.builder()
                    .id(UUID.randomUUID())
                    .alertType(getAlertType(i))
                    .severity(getSeverity(i))
                    .status("PENDING")
                    .description(getAlertDescription(i))
                    .accountId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .riskScore(50 + (i * 5))
                    .createdAt(LocalDateTime.now().minusHours(i + 1))
                    .build());
        }
        
        return alerts;
    }

    private String getAlertType(int index) {
        String[] types = {"SUSPICIOUS_TRANSACTION", "VELOCITY_BREACH", "HIGH_RISK_AMOUNT", "UNUSUAL_PATTERN", "GEOGRAPHIC_ANOMALY"};
        return types[index % types.length];
    }

    private String getSeverity(int index) {
        String[] severities = {"HIGH", "MEDIUM", "LOW"};
        return severities[index % severities.length];
    }

    private String getAlertDescription(int index) {
        String[] descriptions = {
                "Multiple transactions detected in short time",
                "Transaction amount exceeds normal pattern",
                "Login from unusual location",
                "Rapid account activity detected",
                "Transaction velocity limit approached"
        };
        return descriptions[index % descriptions.length];
    }

    private Long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FraudAlertResponse {
        private UUID id;
        private String alertType;
        private String severity;
        private String status;
        private String description;
        private UUID accountId;
        private UUID userId;
        private int riskScore;
        private LocalDateTime createdAt;
        private String reviewedBy;
        private LocalDateTime reviewedAt;
        private String reviewNotes;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReviewAlertRequest {
        private String decision; // APPROVE, DISMISS, ESCALATE
        private String notes;
    }
}
