package com.rrbank.admin.controller;

import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.security.AdminUserDetails;
import com.rrbank.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "System settings and configuration")
public class SettingsController {

    private final AuditLogService auditLogService;

    // In-memory settings (in production, these would be in a database)
    private static final Map<String, Object> systemSettings = new HashMap<>();
    
    static {
        // Initialize default settings
        systemSettings.put("transactionLimits.dailyLimit", new BigDecimal("10000.00"));
        systemSettings.put("transactionLimits.singleTransactionLimit", new BigDecimal("5000.00"));
        systemSettings.put("transactionLimits.monthlyLimit", new BigDecimal("100000.00"));
        systemSettings.put("security.sessionTimeout", 30);
        systemSettings.put("security.maxLoginAttempts", 5);
        systemSettings.put("security.lockoutDuration", 30);
        systemSettings.put("security.passwordExpiryDays", 90);
        systemSettings.put("notifications.emailEnabled", true);
        systemSettings.put("notifications.smsEnabled", false);
        systemSettings.put("notifications.pushEnabled", true);
        systemSettings.put("maintenance.maintenanceMode", false);
        systemSettings.put("maintenance.maintenanceMessage", "System is under maintenance. Please try again later.");
    }

    @GetMapping
    @Operation(summary = "Get all settings", description = "Get all system settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllSettings() {
        log.info("Admin getting all settings");
        return ResponseEntity.ok(ApiResponse.success(new HashMap<>(systemSettings)));
    }

    @GetMapping("/{category}")
    @Operation(summary = "Get settings by category", description = "Get settings for a specific category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettingsByCategory(
            @PathVariable String category
    ) {
        log.info("Admin getting settings for category: {}", category);
        
        Map<String, Object> categorySettings = new HashMap<>();
        String prefix = category + ".";
        
        for (Map.Entry<String, Object> entry : systemSettings.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String key = entry.getKey().substring(prefix.length());
                categorySettings.put(key, entry.getValue());
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(categorySettings));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update settings", description = "Update system settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @RequestBody Map<String, Object> newSettings,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} updating settings", admin.getUsername());
        
        String oldValues = systemSettings.toString();
        
        for (Map.Entry<String, Object> entry : newSettings.entrySet()) {
            if (systemSettings.containsKey(entry.getKey())) {
                systemSettings.put(entry.getKey(), entry.getValue());
            }
        }
        
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "UPDATE_SETTINGS",
                AdminAuditLog.ActionType.UPDATE,
                "SETTINGS",
                "SYSTEM",
                "Updated system settings",
                oldValues,
                systemSettings.toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", new HashMap<>(systemSettings)));
    }

    @GetMapping("/transaction-limits")
    @Operation(summary = "Get transaction limits", description = "Get current transaction limits")
    public ResponseEntity<ApiResponse<TransactionLimitsResponse>> getTransactionLimits() {
        log.info("Admin getting transaction limits");
        
        TransactionLimitsResponse limits = TransactionLimitsResponse.builder()
                .dailyLimit((BigDecimal) systemSettings.get("transactionLimits.dailyLimit"))
                .singleTransactionLimit((BigDecimal) systemSettings.get("transactionLimits.singleTransactionLimit"))
                .monthlyLimit((BigDecimal) systemSettings.get("transactionLimits.monthlyLimit"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(limits));
    }

    @PutMapping("/transaction-limits")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update transaction limits", description = "Update transaction limits")
    public ResponseEntity<ApiResponse<TransactionLimitsResponse>> updateTransactionLimits(
            @RequestBody TransactionLimitsRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} updating transaction limits", admin.getUsername());
        
        if (request.getDailyLimit() != null) {
            systemSettings.put("transactionLimits.dailyLimit", request.getDailyLimit());
        }
        if (request.getSingleTransactionLimit() != null) {
            systemSettings.put("transactionLimits.singleTransactionLimit", request.getSingleTransactionLimit());
        }
        if (request.getMonthlyLimit() != null) {
            systemSettings.put("transactionLimits.monthlyLimit", request.getMonthlyLimit());
        }
        
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "UPDATE_TRANSACTION_LIMITS",
                AdminAuditLog.ActionType.UPDATE,
                "SETTINGS",
                "TRANSACTION_LIMITS",
                "Updated transaction limits",
                null,
                request.toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        TransactionLimitsResponse limits = TransactionLimitsResponse.builder()
                .dailyLimit((BigDecimal) systemSettings.get("transactionLimits.dailyLimit"))
                .singleTransactionLimit((BigDecimal) systemSettings.get("transactionLimits.singleTransactionLimit"))
                .monthlyLimit((BigDecimal) systemSettings.get("transactionLimits.monthlyLimit"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Transaction limits updated", limits));
    }

    @GetMapping("/security")
    @Operation(summary = "Get security settings", description = "Get security configuration")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> getSecuritySettings() {
        log.info("Admin getting security settings");
        
        SecuritySettingsResponse settings = SecuritySettingsResponse.builder()
                .sessionTimeout((Integer) systemSettings.get("security.sessionTimeout"))
                .maxLoginAttempts((Integer) systemSettings.get("security.maxLoginAttempts"))
                .lockoutDuration((Integer) systemSettings.get("security.lockoutDuration"))
                .passwordExpiryDays((Integer) systemSettings.get("security.passwordExpiryDays"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/security")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update security settings", description = "Update security configuration")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> updateSecuritySettings(
            @RequestBody SecuritySettingsRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} updating security settings", admin.getUsername());
        
        if (request.getSessionTimeout() != null) {
            systemSettings.put("security.sessionTimeout", request.getSessionTimeout());
        }
        if (request.getMaxLoginAttempts() != null) {
            systemSettings.put("security.maxLoginAttempts", request.getMaxLoginAttempts());
        }
        if (request.getLockoutDuration() != null) {
            systemSettings.put("security.lockoutDuration", request.getLockoutDuration());
        }
        if (request.getPasswordExpiryDays() != null) {
            systemSettings.put("security.passwordExpiryDays", request.getPasswordExpiryDays());
        }
        
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "UPDATE_SECURITY_SETTINGS",
                AdminAuditLog.ActionType.UPDATE,
                "SETTINGS",
                "SECURITY",
                "Updated security settings",
                null,
                request.toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        SecuritySettingsResponse settings = SecuritySettingsResponse.builder()
                .sessionTimeout((Integer) systemSettings.get("security.sessionTimeout"))
                .maxLoginAttempts((Integer) systemSettings.get("security.maxLoginAttempts"))
                .lockoutDuration((Integer) systemSettings.get("security.lockoutDuration"))
                .passwordExpiryDays((Integer) systemSettings.get("security.passwordExpiryDays"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Security settings updated", settings));
    }

    @GetMapping("/maintenance")
    @Operation(summary = "Get maintenance status", description = "Get maintenance mode status")
    public ResponseEntity<ApiResponse<MaintenanceStatusResponse>> getMaintenanceStatus() {
        log.info("Admin getting maintenance status");
        
        MaintenanceStatusResponse status = MaintenanceStatusResponse.builder()
                .maintenanceMode((Boolean) systemSettings.get("maintenance.maintenanceMode"))
                .maintenanceMessage((String) systemSettings.get("maintenance.maintenanceMessage"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PutMapping("/maintenance")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Toggle maintenance mode", description = "Enable or disable maintenance mode")
    public ResponseEntity<ApiResponse<MaintenanceStatusResponse>> setMaintenanceMode(
            @RequestBody MaintenanceModeRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} setting maintenance mode to {}", admin.getUsername(), request.getEnabled());
        
        systemSettings.put("maintenance.maintenanceMode", request.getEnabled());
        if (request.getMessage() != null) {
            systemSettings.put("maintenance.maintenanceMessage", request.getMessage());
        }
        
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                request.getEnabled() ? "ENABLE_MAINTENANCE_MODE" : "DISABLE_MAINTENANCE_MODE",
                AdminAuditLog.ActionType.UPDATE,
                "SETTINGS",
                "MAINTENANCE",
                "Maintenance mode " + (request.getEnabled() ? "enabled" : "disabled"),
                null,
                request.toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
        
        MaintenanceStatusResponse status = MaintenanceStatusResponse.builder()
                .maintenanceMode((Boolean) systemSettings.get("maintenance.maintenanceMode"))
                .maintenanceMessage((String) systemSettings.get("maintenance.maintenanceMessage"))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Maintenance mode updated", status));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionLimitsResponse {
        private BigDecimal dailyLimit;
        private BigDecimal singleTransactionLimit;
        private BigDecimal monthlyLimit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionLimitsRequest {
        private BigDecimal dailyLimit;
        private BigDecimal singleTransactionLimit;
        private BigDecimal monthlyLimit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySettingsResponse {
        private Integer sessionTimeout;
        private Integer maxLoginAttempts;
        private Integer lockoutDuration;
        private Integer passwordExpiryDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySettingsRequest {
        private Integer sessionTimeout;
        private Integer maxLoginAttempts;
        private Integer lockoutDuration;
        private Integer passwordExpiryDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceStatusResponse {
        private Boolean maintenanceMode;
        private String maintenanceMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceModeRequest {
        private Boolean enabled;
        private String message;
    }
}
