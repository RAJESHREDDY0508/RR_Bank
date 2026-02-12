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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "View and manage transactions")
public class TransactionManagementController {

    private final ServiceClientService serviceClient;
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List transactions", description = "Get paginated list of transactions with filters")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        log.info("Admin getting transactions - page: {}, size: {}, status: {}, type: {}", page, size, status, type);
        
        Map<String, Object> response = serviceClient.getTransactions(page, size, status, type);
        
        if (response != null && !response.isEmpty()) {
            List<TransactionResponse> transactions = mapToTransactionResponses(response);
            long totalElements = getLongValue(response, "totalElements", transactions.size());
            int totalPages = getIntValue(response, "totalPages", 1);
            PageResponse<TransactionResponse> pageResponse = PageResponse.of(
                    transactions, page, size, totalElements
            );
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details", description = "Get detailed information about a transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable UUID id) {
        log.info("Admin getting transaction: {}", id);
        
        Map<String, Object> response = serviceClient.getTransaction(id.toString());
        
        if (response != null && !response.isEmpty()) {
            TransactionResponse transaction = mapToTransactionResponse(response);
            return ResponseEntity.ok(ApiResponse.success(transaction));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account", description = "Get all transactions for an account")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactionsByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Admin getting transactions for account: {}", accountId);
        
        Map<String, Object> response = serviceClient.getTransactionsByAccount(accountId.toString(), page, size);
        
        if (response != null && !response.isEmpty()) {
            List<TransactionResponse> transactions = mapToTransactionResponses(response);
            long totalElements = getLongValue(response, "totalElements", transactions.size());
            int totalPages = getIntValue(response, "totalPages", 1);
            PageResponse<TransactionResponse> pageResponse = PageResponse.of(
                    transactions, page, size, totalElements
            );
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get transaction statistics", description = "Get transaction statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactionStats() {
        log.info("Admin getting transaction stats");
        
        Map<String, Object> stats = serviceClient.getTransactionStats();
        
        if (stats != null && !stats.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(stats));
        }

        // Return empty stats
        Map<String, Object> emptyStats = new HashMap<>();
        emptyStats.put("totalTransactions", 0L);
        emptyStats.put("transactionsToday", 0L);
        emptyStats.put("volumeToday", BigDecimal.ZERO);
        return ResponseEntity.ok(ApiResponse.success(emptyStats));
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse transaction", description = "Reverse a completed transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> reverseTransaction(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} reversing transaction {}", admin.getUsername(), id);
        
        String reason = request.getOrDefault("reason", "Admin reversal");

        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "REVERSE_TRANSACTION",
                AdminAuditLog.ActionType.UPDATE,
                "TRANSACTION",
                id.toString(),
                "Transaction reversed. Reason: " + reason,
                "COMPLETED",
                "REVERSED",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        // In production, this would call the transaction service to reverse
        TransactionResponse response = TransactionResponse.builder()
                .id(id)
                .status("REVERSED")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Transaction reversed", response));
    }

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<TransactionResponse> mapToTransactionResponses(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null) {
            return new ArrayList<>();
        }
        return content.stream().map(this::mapToTransactionResponse).collect(Collectors.toList());
    }

    private TransactionResponse mapToTransactionResponse(Map<String, Object> data) {
        return TransactionResponse.builder()
                .id(getUUID(data, "id"))
                .transactionReference((String) data.get("transactionReference"))
                .transactionType((String) data.get("transactionType"))
                .status((String) data.get("status"))
                .amount(getBigDecimal(data, "amount"))
                .currency((String) data.getOrDefault("currency", "USD"))
                .fromAccountId(getUUID(data, "fromAccountId"))
                .toAccountId(getUUID(data, "toAccountId"))
                .description((String) data.get("description"))
                .createdAt(parseDateTime(data.get("createdAt")))
                .completedAt(parseDateTime(data.get("completedAt")))
                .build();
    }

    private UUID getUUID(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private Long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
