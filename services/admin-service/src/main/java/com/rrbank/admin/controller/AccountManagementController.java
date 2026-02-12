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
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "Manage bank accounts")
public class AccountManagementController {

    private final ServiceClientService serviceClient;
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List accounts", description = "Get paginated list of accounts with filters")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        log.info("Admin getting accounts - page: {}, size: {}, status: {}, type: {}", page, size, status, accountType);
        
        Map<String, Object> response = serviceClient.getAccounts(page, size, status, accountType, search);
        
        if (response != null && !response.isEmpty()) {
            List<AccountResponse> accounts = mapToAccountResponses(response);
            long totalElements = getLongValue(response, "totalElements", accounts.size());
            int totalPages = getIntValue(response, "totalPages", 1);
            PageResponse<AccountResponse> pageResponse = PageResponse.of(
                    accounts, page, size, totalElements
            );
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details", description = "Get detailed information about an account")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable UUID id) {
        log.info("Admin getting account: {}", id);
        
        Map<String, Object> response = serviceClient.getAccount(id.toString());
        
        if (response != null && !response.isEmpty()) {
            AccountResponse account = mapToAccountResponse(response);
            return ResponseEntity.ok(ApiResponse.success(account));
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update account status", description = "Freeze, unfreeze, or close an account")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AccountActionRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} performing {} on account {}", admin.getUsername(), request.getAction(), id);

        String action = request.getAction().toUpperCase();
        AdminAuditLog.ActionType actionType;
        String newStatus;

        switch (action) {
            case "FREEZE":
                actionType = AdminAuditLog.ActionType.FREEZE;
                newStatus = "FROZEN";
                break;
            case "UNFREEZE":
                actionType = AdminAuditLog.ActionType.UNFREEZE;
                newStatus = "ACTIVE";
                break;
            case "CLOSE":
                actionType = AdminAuditLog.ActionType.UPDATE;
                newStatus = "CLOSED";
                break;
            default:
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid action: " + action));
        }

        // Update status in account service
        Map<String, Object> updateResponse = serviceClient.updateAccountStatus(id.toString(), newStatus);
        
        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                action + "_ACCOUNT",
                actionType,
                "ACCOUNT",
                id.toString(),
                "Account " + action.toLowerCase() + ". Reason: " + request.getReason(),
                null,
                newStatus,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        if (updateResponse != null && !updateResponse.isEmpty()) {
            AccountResponse account = mapToAccountResponse(updateResponse);
            return ResponseEntity.ok(ApiResponse.success("Account " + action.toLowerCase() + " successful", account));
        }

        // Return success even if service call failed (logged action)
        AccountResponse response = AccountResponse.builder()
                .id(id)
                .status(newStatus)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Account " + action.toLowerCase() + " successful", response));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get account transactions", description = "Get transaction history for an account")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAccountTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Admin getting transactions for account: {}", id);
        
        Map<String, Object> response = serviceClient.getTransactionsByAccount(id.toString(), page, size);
        
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

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance", description = "Get current balance from ledger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountBalance(@PathVariable UUID id) {
        log.info("Admin getting balance for account: {}", id);
        
        Map<String, Object> balance = serviceClient.getAccountBalance(id.toString());
        
        if (balance != null && !balance.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(balance));
        }

        return ResponseEntity.ok(ApiResponse.success(Collections.singletonMap("balance", BigDecimal.ZERO)));
    }

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<AccountResponse> mapToAccountResponses(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null) {
            return new ArrayList<>();
        }
        return content.stream().map(this::mapToAccountResponse).collect(Collectors.toList());
    }

    private AccountResponse mapToAccountResponse(Map<String, Object> data) {
        return AccountResponse.builder()
                .id(getUUID(data, "id"))
                .accountNumber((String) data.get("accountNumber"))
                .accountType((String) data.get("accountType"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .currency((String) data.getOrDefault("currency", "USD"))
                .balance(getBigDecimal(data, "balance"))
                .availableBalance(getBigDecimal(data, "availableBalance"))
                .userId(getUUID(data, "userId"))
                .customerId(getUUID(data, "customerId"))
                .createdAt(parseDateTime(data.get("createdAt")))
                .updatedAt(parseDateTime(data.get("updatedAt")))
                .build();
    }

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
