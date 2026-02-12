package com.rrbank.admin.controller;

import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.dto.common.PageResponse;
import com.rrbank.admin.service.ServiceClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "Manage payments and payment methods")
public class PaymentManagementController {

    private final ServiceClientService serviceClient;

    @GetMapping
    @Operation(summary = "List payments", description = "Get paginated list of payments")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        log.info("Admin getting payments - page: {}, size: {}, status: {}", page, size, status);
        
        // Get transactions of type PAYMENT from transaction service
        Map<String, Object> response = serviceClient.getTransactions(page, size, status, "PAYMENT");
        
        List<PaymentResponse> payments = new ArrayList<>();
        if (response != null && response.containsKey("content")) {
            payments = mapToPaymentResponses(response);
        }

        long totalElements = response != null ? getLongValue(response, "totalElements", payments.size()) : 0;
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(payments, page, size, totalElements)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details", description = "Get detailed information about a payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        log.info("Admin getting payment: {}", id);
        
        Map<String, Object> response = serviceClient.getTransaction(id.toString());
        
        if (response != null && !response.isEmpty()) {
            PaymentResponse payment = mapToPaymentResponse(response);
            return ResponseEntity.ok(ApiResponse.success(payment));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get payment statistics", description = "Get payment statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStats() {
        log.info("Admin getting payment stats");
        
        Map<String, Object> txStats = serviceClient.getTransactionStats();
        
        Map<String, Object> stats = new HashMap<>();
        if (txStats != null) {
            stats.put("totalPayments", getLongValue(txStats, "totalTransactions", 0L));
            stats.put("completedPayments", getLongValue(txStats, "completedTransactions", 0L));
            stats.put("pendingPayments", getLongValue(txStats, "pendingTransactions", 0L));
            stats.put("failedPayments", getLongValue(txStats, "failedTransactions", 0L));
            stats.put("volumeToday", txStats.getOrDefault("volumeToday", BigDecimal.ZERO));
        } else {
            stats.put("totalPayments", 0L);
            stats.put("completedPayments", 0L);
            stats.put("pendingPayments", 0L);
            stats.put("failedPayments", 0L);
            stats.put("volumeToday", BigDecimal.ZERO);
        }
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<PaymentResponse> mapToPaymentResponses(Map<String, Object> response) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null) {
            return new ArrayList<>();
        }
        return content.stream().map(this::mapToPaymentResponse).collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Map<String, Object> data) {
        return PaymentResponse.builder()
                .id(getUUID(data, "id"))
                .paymentReference((String) data.get("transactionReference"))
                .paymentType((String) data.getOrDefault("transactionType", "PAYMENT"))
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

    // DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private UUID id;
        private String paymentReference;
        private String paymentType;
        private String status;
        private BigDecimal amount;
        private String currency;
        private UUID fromAccountId;
        private UUID toAccountId;
        private String description;
        private String payeeName;
        private String payeeAccount;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
}
