package com.rrbank.admin.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "KYC Management", description = "Manage customer KYC verification requests")
public class KycManagementController {

    private final ServiceClientService serviceClient;
    private final AuditLogService auditLogService;

    @GetMapping("/pending")
    @Operation(summary = "Get pending KYC requests")
    public ResponseEntity<ApiResponse<PageResponse<KycCustomerResponse>>> getPendingKycRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Admin getting pending KYC requests - page: {}, size: {}", page, size);
        
        try {
            Map<String, Object> response = serviceClient.getPendingKycCustomers(page, size);
            
            // Handle empty or null response gracefully
            if (response == null || response.isEmpty()) {
                log.warn("Received empty response from customer service for pending KYC requests");
                return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
            }
            
            List<KycCustomerResponse> customers = mapToKycCustomerResponses(response);
            long totalElements = getLongValue(response, "totalElements", customers.size());
            return ResponseEntity.ok(ApiResponse.success(PageResponse.of(customers, page, size, totalElements)));
            
        } catch (Exception e) {
            log.error("Error getting pending KYC requests", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to fetch KYC requests. Please try again later."));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get KYC statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKycStats() {
        log.info("Admin getting KYC stats");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", 0L);
        stats.put("approved", 0L);
        stats.put("rejected", 0L);
        
        try {
            Map<String, Object> fetchedStats = serviceClient.getKycStats();
            
            // Handle empty or null response gracefully
            if (fetchedStats != null && !fetchedStats.isEmpty()) {
                stats.put("pending", getLongValue(fetchedStats, "pending", 0L));
                stats.put("approved", getLongValue(fetchedStats, "approved", 0L));
                stats.put("rejected", getLongValue(fetchedStats, "rejected", 0L));
                log.info("Successfully fetched KYC stats: {}", stats);
            } else {
                log.warn("Received empty response from customer service for KYC stats, returning defaults");
            }
        } catch (Exception e) {
            log.error("Error getting KYC stats, returning defaults", e);
        }
        
        // Always return 200 with default stats if service is unavailable
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get customers by KYC status")
    public ResponseEntity<ApiResponse<PageResponse<KycCustomerResponse>>> getCustomersByKycStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Admin getting customers by KYC status: {}", status);
        
        try {
            Map<String, Object> response = serviceClient.getCustomersByKycStatus(status.toUpperCase(), page, size);
            
            // Handle empty or null response gracefully
            if (response == null || response.isEmpty()) {
                log.warn("Received empty response from customer service for KYC status: {}", status);
                return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
            }
            
            List<KycCustomerResponse> customers = mapToKycCustomerResponses(response);
            long totalElements = getLongValue(response, "totalElements", customers.size());
            return ResponseEntity.ok(ApiResponse.success(PageResponse.of(customers, page, size, totalElements)));
            
        } catch (Exception e) {
            log.error("Error getting customers by KYC status: {}", status, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to fetch customers. Please try again later."));
        }
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer KYC details")
    public ResponseEntity<ApiResponse<KycCustomerResponse>> getCustomerKycDetails(@PathVariable String customerId) {
        log.info("Admin getting KYC details for customer: {}", customerId);
        
        try {
            Map<String, Object> response = serviceClient.getCustomer(customerId);
            
            if (response != null && !response.isEmpty()) {
                KycCustomerResponse customer = mapToKycCustomerResponse(response);
                return ResponseEntity.ok(ApiResponse.success(customer));
            }
            
            log.warn("Customer not found: {}", customerId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found"));
                    
        } catch (Exception e) {
            log.error("Error getting customer KYC details: {}", customerId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to fetch customer details. Please try again later."));
        }
    }

    @PostMapping("/{customerId}/approve")
    @Operation(summary = "Approve KYC")
    public ResponseEntity<ApiResponse<KycCustomerResponse>> approveKyc(
            @PathVariable String customerId,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} approving KYC for customer: {}", admin.getUsername(), customerId);
        
        try {
            Map<String, Object> response = serviceClient.approveKyc(customerId);
            
            if (response != null && !response.isEmpty()) {
                auditLogService.logActionSync(
                        admin.getId(), admin.getUsername(), "APPROVE_KYC",
                        AdminAuditLog.ActionType.UPDATE, "CUSTOMER", customerId,
                        "KYC approved for customer", "PENDING", "APPROVED",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent")
                );
                
                KycCustomerResponse customer = mapToKycCustomerResponse(response);
                return ResponseEntity.ok(ApiResponse.success("KYC approved successfully", customer));
            }
            
            log.warn("Failed to approve KYC - empty response from customer service");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to approve KYC. Please try again later."));
                    
        } catch (Exception e) {
            log.error("Error approving KYC for customer: {}", customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to approve KYC: " + e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/approve")
    @Operation(summary = "Approve KYC by User ID")
    public ResponseEntity<ApiResponse<KycCustomerResponse>> approveKycByUserId(
            @PathVariable String userId,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} approving KYC for user: {}", admin.getUsername(), userId);
        
        try {
            Map<String, Object> response = serviceClient.approveKycByUserId(userId);
            
            if (response != null && !response.isEmpty()) {
                auditLogService.logActionSync(
                        admin.getId(), admin.getUsername(), "APPROVE_KYC",
                        AdminAuditLog.ActionType.UPDATE, "USER", userId,
                        "KYC approved for user", "PENDING", "APPROVED",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent")
                );
                
                KycCustomerResponse customer = mapToKycCustomerResponse(response);
                return ResponseEntity.ok(ApiResponse.success("KYC approved successfully", customer));
            }
            
            log.warn("Failed to approve KYC - empty response from customer service");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to approve KYC. Please try again later."));
                    
        } catch (Exception e) {
            log.error("Error approving KYC for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to approve KYC: " + e.getMessage()));
        }
    }

    @PostMapping("/{customerId}/reject")
    @Operation(summary = "Reject KYC")
    public ResponseEntity<ApiResponse<KycCustomerResponse>> rejectKyc(
            @PathVariable String customerId,
            @RequestBody(required = false) RejectKycRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} rejecting KYC for customer: {}", admin.getUsername(), customerId);
        
        try {
            String reason = request != null ? request.reason() : null;
            Map<String, Object> response = serviceClient.rejectKyc(customerId, reason);
            
            if (response != null && !response.isEmpty()) {
                auditLogService.logActionSync(
                        admin.getId(), admin.getUsername(), "REJECT_KYC",
                        AdminAuditLog.ActionType.UPDATE, "CUSTOMER", customerId,
                        "KYC rejected. Reason: " + (reason != null ? reason : "Not specified"),
                        "PENDING", "REJECTED",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent")
                );
                
                KycCustomerResponse customer = mapToKycCustomerResponse(response);
                return ResponseEntity.ok(ApiResponse.success("KYC rejected", customer));
            }
            
            log.warn("Failed to reject KYC - empty response from customer service");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to reject KYC. Please try again later."));
                    
        } catch (Exception e) {
            log.error("Error rejecting KYC for customer: {}", customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reject KYC: " + e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/reject")
    @Operation(summary = "Reject KYC by User ID")
    public ResponseEntity<ApiResponse<KycCustomerResponse>> rejectKycByUserId(
            @PathVariable String userId,
            @RequestBody(required = false) RejectKycRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} rejecting KYC for user: {}", admin.getUsername(), userId);
        
        try {
            String reason = request != null ? request.reason() : null;
            Map<String, Object> response = serviceClient.rejectKycByUserId(userId, reason);
            
            if (response != null && !response.isEmpty()) {
                auditLogService.logActionSync(
                        admin.getId(), admin.getUsername(), "REJECT_KYC",
                        AdminAuditLog.ActionType.UPDATE, "USER", userId,
                        "KYC rejected. Reason: " + (reason != null ? reason : "Not specified"),
                        "PENDING", "REJECTED",
                        getClientIp(httpRequest), httpRequest.getHeader("User-Agent")
                );
                
                KycCustomerResponse customer = mapToKycCustomerResponse(response);
                return ResponseEntity.ok(ApiResponse.success("KYC rejected", customer));
            }
            
            log.warn("Failed to reject KYC - empty response from customer service");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Unable to reject KYC. Please try again later."));
                    
        } catch (Exception e) {
            log.error("Error rejecting KYC for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reject KYC: " + e.getMessage()));
        }
    }

    // DTOs
    public record RejectKycRequest(String reason) {}

    public record KycCustomerResponse(
            String id,
            String userId,
            String email,
            String firstName,
            String lastName,
            String fullName,
            String phoneNumber,
            String kycStatus,
            String kycRejectionReason,
            String kycVerifiedAt,
            String createdAt
    ) {}

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<KycCustomerResponse> mapToKycCustomerResponses(Map<String, Object> response) {
        try {
            Object content = response.get("content");
            if (content instanceof List) {
                return ((List<Map<String, Object>>) content).stream()
                        .filter(Objects::nonNull) // Filter out null entries
                        .map(this::mapToKycCustomerResponse)
                        .filter(Objects::nonNull) // Filter out mapping failures
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error mapping KYC responses", e);
        }
        return new ArrayList<>();
    }

    private KycCustomerResponse mapToKycCustomerResponse(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            log.warn("Attempted to map null or empty customer data");
            return null;
        }
        
        try {
            String firstName = getString(data, "firstName");
            String lastName = getString(data, "lastName");
            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            
            return new KycCustomerResponse(
                    getString(data, "id"),
                    getString(data, "userId"),
                    getString(data, "email"),
                    firstName,
                    lastName,
                    fullName.isEmpty() ? null : fullName,
                    getString(data, "phoneNumber"),
                    getKycStatus(data),
                    getString(data, "kycRejectionReason"),
                    getString(data, "kycVerifiedAt"),
                    getString(data, "createdAt")
            );
        } catch (Exception e) {
            log.error("Error mapping individual customer response: {}", e.getMessage());
            return null;
        }
    }

    private String getString(Map<String, Object> data, String key) {
        try {
            Object value = data.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Error getting string value for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    private String getKycStatus(Map<String, Object> data) {
        try {
            Object kycStatus = data.get("kycStatus");
            if (kycStatus != null) return kycStatus.toString();
            
            Object kycVerified = data.get("kycVerified");
            if (kycVerified instanceof Boolean) {
                return Boolean.TRUE.equals(kycVerified) ? "APPROVED" : "PENDING";
            }
            return "PENDING";
        } catch (Exception e) {
            log.warn("Error getting KYC status: {}", e.getMessage());
            return "PENDING";
        }
    }

    private Long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        try {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
            log.warn("Error getting long value for key {}: {}", key, e.getMessage());
        }
        return defaultValue;
    }

    private String getClientIp(HttpServletRequest request) {
        try {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Error getting client IP: {}", e.getMessage());
            return "unknown";
        }
    }
}
