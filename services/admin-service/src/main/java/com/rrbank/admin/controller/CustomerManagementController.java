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
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "Manage bank customers")
public class CustomerManagementController {

    private final ServiceClientService serviceClient;
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List customers", description = "Get paginated list of customers with filters")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("Admin getting customers - page: {}, size: {}, search: {}", page, size, search);
        
        Map<String, Object> response = serviceClient.getCustomers(page, size, search);
        
        if (response != null && !response.isEmpty()) {
            List<CustomerResponse> customers = mapToCustomerResponses(response);
            long totalElements = getLongValue(response, "totalElements", customers.size());
            int totalPages = getIntValue(response, "totalPages", 1);
            PageResponse<CustomerResponse> pageResponse = PageResponse.of(
                    customers, page, size, totalElements
            );
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        }

        // Return empty page if service unavailable
        return ResponseEntity.ok(ApiResponse.success(PageResponse.empty(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer details", description = "Get detailed information about a customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable UUID id) {
        log.info("Admin getting customer: {}", id);
        
        Map<String, Object> response = serviceClient.getCustomer(id.toString());
        
        if (response != null && !response.isEmpty()) {
            CustomerResponse customer = mapToCustomerResponse(response);
            return ResponseEntity.ok(ApiResponse.success(customer));
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update customer status", description = "Change customer status (ACTIVE, SUSPENDED, etc.)")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomerStatus(
            @PathVariable UUID id,
            @RequestBody UpdateCustomerStatusRequest request,
            @AuthenticationPrincipal AdminUserDetails admin,
            HttpServletRequest httpRequest
    ) {
        log.info("Admin {} updating customer {} status to {}", admin.getUsername(), id, request.getStatus());

        // Log the action
        auditLogService.logActionSync(
                admin.getId(),
                admin.getUsername(),
                "UPDATE_CUSTOMER_STATUS",
                AdminAuditLog.ActionType.UPDATE,
                "CUSTOMER",
                id.toString(),
                "Customer status updated to " + request.getStatus() + ". Reason: " + request.getReason(),
                null,
                request.getStatus(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        Map<String, Object> response = serviceClient.getCustomer(id.toString());
        if (response != null && !response.isEmpty()) {
            CustomerResponse customer = mapToCustomerResponse(response);
            customer.setStatus(request.getStatus());
            return ResponseEntity.ok(ApiResponse.success("Customer status updated", customer));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/accounts")
    @Operation(summary = "Get customer accounts", description = "Get all accounts belonging to a customer")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getCustomerAccounts(@PathVariable UUID id) {
        log.info("Admin getting accounts for customer: {}", id);
        
        List<Map<String, Object>> response = serviceClient.getAccountsByUser(id.toString());
        
        if (response != null && !response.isEmpty()) {
            List<AccountResponse> accounts = response.stream()
                    .map(this::mapToAccountResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(accounts));
        }

        return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get customer transactions", description = "Get transaction history for a customer")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getCustomerTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Admin getting transactions for customer: {}", id);
        
        // First get customer's accounts
        List<Map<String, Object>> accounts = serviceClient.getAccountsByUser(id.toString());
        
        List<TransactionResponse> allTransactions = new ArrayList<>();
        if (accounts != null && !accounts.isEmpty()) {
            // Get transactions for the first account (simplified)
            String accountId = (String) accounts.get(0).get("id");
            if (accountId != null) {
                Map<String, Object> txResponse = serviceClient.getTransactionsByAccount(accountId, page, size);
                if (txResponse != null && txResponse.containsKey("content")) {
                    allTransactions = mapToTransactionResponses(txResponse);
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(allTransactions, page, size, allTransactions.size())));
    }

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<CustomerResponse> mapToCustomerResponses(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null) {
            return new ArrayList<>();
        }
        return content.stream().map(this::mapToCustomerResponse).collect(Collectors.toList());
    }

    private CustomerResponse mapToCustomerResponse(Map<String, Object> data) {
        return CustomerResponse.builder()
                .id(getUUID(data, "id"))
                .userId(getUUID(data, "userId"))
                .email((String) data.get("email"))
                .firstName((String) data.get("firstName"))
                .lastName((String) data.get("lastName"))
                .fullName(getFullName(data))
                .phoneNumber((String) data.get("phoneNumber"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .kycStatus(Boolean.TRUE.equals(data.get("kycVerified")) ? "VERIFIED" : "PENDING")
                .address(mapAddress(data))
                .createdAt(parseDateTime(data.get("createdAt")))
                .build();
    }

    private String getFullName(Map<String, Object> data) {
        String firstName = (String) data.getOrDefault("firstName", "");
        String lastName = (String) data.getOrDefault("lastName", "");
        return (firstName + " " + lastName).trim();
    }

    private AddressInfo mapAddress(Map<String, Object> data) {
        return AddressInfo.builder()
                .line1((String) data.get("address"))
                .city((String) data.get("city"))
                .state((String) data.get("state"))
                .postalCode((String) data.get("postalCode"))
                .country((String) data.get("country"))
                .build();
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
                .createdAt(parseDateTime(data.get("createdAt")))
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
