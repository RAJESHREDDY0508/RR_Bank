package com.rrbank.admin.controller;

import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.dto.common.PageResponse;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.security.AdminUserDetails;
import com.rrbank.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/account-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Requests", description = "Manage account opening requests")
public class AccountRequestsController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List account requests", description = "Get paginated list of account opening requests")
    public ResponseEntity<ApiResponse<PageResponse<AccountRequestResponse>>> getAccountRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        log.info("Fetching account requests - page: {}, size: {}, status: {}", page, size, status);
        
        List<AccountRequestResponse> mockRequests = getMockAccountRequests();
        
        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            mockRequests = mockRequests.stream()
                    .filter(r -> r.getStatus().equalsIgnoreCase(status))
                    .toList();
        }
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(mockRequests, page, size, mockRequests.size())));
    }

    @GetMapping("/count")
    @Operation(summary = "Get pending count", description = "Get count of pending account requests")
    public ResponseEntity<ApiResponse<CountResponse>> getPendingCount() {
        log.info("Fetching pending account requests count");
        // Return mock count - in production this would query the database
        return ResponseEntity.ok(ApiResponse.success(new CountResponse(3)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account request details", description = "Get details of a specific account request")
    public ResponseEntity<ApiResponse<AccountRequestResponse>> getAccountRequest(@PathVariable UUID id) {
        log.info("Fetching account request: {}", id);
        AccountRequestResponse mock = getMockAccountRequest(id);
        return ResponseEntity.ok(ApiResponse.success(mock));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve account request", description = "Approve an account opening request")
    public ResponseEntity<ApiResponse<AccountRequestResponse>> approveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} approving account request {}", admin.getUsername(), id);

        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "APPROVE_ACCOUNT_REQUEST",
                AdminAuditLog.ActionType.APPROVE,
                "ACCOUNT_REQUEST",
                id.toString(),
                "Account request approved",
                "PENDING",
                "APPROVED",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        AccountRequestResponse response = getMockAccountRequest(id);
        response.setStatus("APPROVED");
        response.setReviewedBy(admin.getUsername());
        response.setReviewedAt(LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success("Account request approved successfully", response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject account request", description = "Reject an account opening request")
    public ResponseEntity<ApiResponse<AccountRequestResponse>> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} rejecting account request {} with reason: {}", admin.getUsername(), id, request.getReason());

        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "REJECT_ACCOUNT_REQUEST",
                AdminAuditLog.ActionType.REJECT,
                "ACCOUNT_REQUEST",
                id.toString(),
                "Account request rejected. Reason: " + request.getReason(),
                "PENDING",
                "REJECTED",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        AccountRequestResponse response = getMockAccountRequest(id);
        response.setStatus("REJECTED");
        response.setRejectionReason(request.getReason());
        response.setReviewedBy(admin.getUsername());
        response.setReviewedAt(LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success("Account request rejected", response));
    }

    // DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountRequestResponse {
        private UUID id;
        private String requestNumber;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String accountType;
        private String status;
        private String idType;
        private String idNumber;
        private String address;
        private LocalDateTime createdAt;
        private LocalDateTime reviewedAt;
        private String reviewedBy;
        private String rejectionReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountResponse {
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }

    // Mock data methods
    private List<AccountRequestResponse> getMockAccountRequests() {
        return new ArrayList<>(List.of(
                AccountRequestResponse.builder()
                        .id(UUID.randomUUID())
                        .requestNumber("REQ-2024-001")
                        .customerName("Alice Johnson")
                        .customerEmail("alice.johnson@email.com")
                        .customerPhone("+1-555-0101")
                        .accountType("CHECKING")
                        .status("PENDING")
                        .idType("PASSPORT")
                        .idNumber("P12345678")
                        .address("123 Main St, New York, NY 10001")
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build(),
                AccountRequestResponse.builder()
                        .id(UUID.randomUUID())
                        .requestNumber("REQ-2024-002")
                        .customerName("Bob Williams")
                        .customerEmail("bob.williams@email.com")
                        .customerPhone("+1-555-0102")
                        .accountType("SAVINGS")
                        .status("PENDING")
                        .idType("DRIVER_LICENSE")
                        .idNumber("DL987654321")
                        .address("456 Oak Ave, Los Angeles, CA 90001")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                AccountRequestResponse.builder()
                        .id(UUID.randomUUID())
                        .requestNumber("REQ-2024-003")
                        .customerName("Carol Davis")
                        .customerEmail("carol.davis@email.com")
                        .customerPhone("+1-555-0103")
                        .accountType("BUSINESS")
                        .status("PENDING")
                        .idType("NATIONAL_ID")
                        .idNumber("NID123456789")
                        .address("789 Business Blvd, Chicago, IL 60601")
                        .createdAt(LocalDateTime.now().minusHours(12))
                        .build(),
                AccountRequestResponse.builder()
                        .id(UUID.randomUUID())
                        .requestNumber("REQ-2024-000")
                        .customerName("David Brown")
                        .customerEmail("david.brown@email.com")
                        .customerPhone("+1-555-0100")
                        .accountType("CHECKING")
                        .status("APPROVED")
                        .idType("PASSPORT")
                        .idNumber("P87654321")
                        .address("321 Elm St, Houston, TX 77001")
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .reviewedAt(LocalDateTime.now().minusDays(4))
                        .reviewedBy("admin")
                        .build()
        ));
    }

    private AccountRequestResponse getMockAccountRequest(UUID id) {
        return AccountRequestResponse.builder()
                .id(id)
                .requestNumber("REQ-2024-001")
                .customerName("Alice Johnson")
                .customerEmail("alice.johnson@email.com")
                .customerPhone("+1-555-0101")
                .accountType("CHECKING")
                .status("PENDING")
                .idType("PASSPORT")
                .idNumber("P12345678")
                .address("123 Main St, New York, NY 10001")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
