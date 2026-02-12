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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/statements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statement Management", description = "Manage account statements")
public class StatementManagementController {

    private final ServiceClientService serviceClient;

    @GetMapping
    @Operation(summary = "List statements", description = "Get paginated list of generated statements")
    public ResponseEntity<ApiResponse<PageResponse<StatementResponse>>> getStatements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String period
    ) {
        log.info("Admin getting statements - page: {}, size: {}", page, size);
        
        // Get accounts and generate statement list
        Map<String, Object> accountsResponse = serviceClient.getAccounts(0, 100, null, null, null);
        
        List<StatementResponse> statements = generateStatementsFromAccounts(accountsResponse);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(statements, page, size, statements.size())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get statement details", description = "Get detailed information about a statement")
    public ResponseEntity<ApiResponse<StatementResponse>> getStatement(@PathVariable UUID id) {
        log.info("Admin getting statement: {}", id);
        
        StatementResponse statement = StatementResponse.builder()
                .id(id)
                .accountId(UUID.randomUUID())
                .accountNumber("ACC-001")
                .period("December 2025")
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .openingBalance(new BigDecimal("5000.00"))
                .closingBalance(new BigDecimal("5500.00"))
                .totalDeposits(new BigDecimal("2000.00"))
                .totalWithdrawals(new BigDecimal("1500.00"))
                .transactionCount(25)
                .generatedAt(LocalDateTime.now().minusDays(1))
                .status("AVAILABLE")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(statement));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate statement", description = "Generate a new statement for an account")
    public ResponseEntity<ApiResponse<StatementResponse>> generateStatement(
            @RequestBody GenerateStatementRequest request
    ) {
        log.info("Admin generating statement for account: {}", request.getAccountId());
        
        StatementResponse statement = StatementResponse.builder()
                .id(UUID.randomUUID())
                .accountId(request.getAccountId())
                .accountNumber("ACC-" + request.getAccountId().toString().substring(0, 8))
                .period(request.getPeriod())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .openingBalance(new BigDecimal("5000.00"))
                .closingBalance(new BigDecimal("5500.00"))
                .totalDeposits(new BigDecimal("2000.00"))
                .totalWithdrawals(new BigDecimal("1500.00"))
                .transactionCount(25)
                .generatedAt(LocalDateTime.now())
                .status("GENERATING")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Statement generation started", statement));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get statements by account", description = "Get all statements for an account")
    public ResponseEntity<ApiResponse<List<StatementResponse>>> getStatementsByAccount(
            @PathVariable UUID accountId
    ) {
        log.info("Admin getting statements for account: {}", accountId);
        
        List<StatementResponse> statements = new ArrayList<>();
        
        // Generate sample statements for the last 6 months
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            statements.add(StatementResponse.builder()
                    .id(UUID.randomUUID())
                    .accountId(accountId)
                    .accountNumber("ACC-" + accountId.toString().substring(0, 8))
                    .period(monthStart.getMonth().name() + " " + monthStart.getYear())
                    .startDate(monthStart)
                    .endDate(monthEnd)
                    .openingBalance(new BigDecimal("5000.00").add(BigDecimal.valueOf(i * 100)))
                    .closingBalance(new BigDecimal("5500.00").add(BigDecimal.valueOf(i * 100)))
                    .totalDeposits(new BigDecimal("2000.00"))
                    .totalWithdrawals(new BigDecimal("1500.00"))
                    .transactionCount(20 + i)
                    .generatedAt(monthEnd.plusDays(1).atStartOfDay())
                    .status("AVAILABLE")
                    .build());
        }
        
        return ResponseEntity.ok(ApiResponse.success(statements));
    }

    // Helper methods
    @SuppressWarnings("unchecked")
    private List<StatementResponse> generateStatementsFromAccounts(Map<String, Object> accountsResponse) {
        List<StatementResponse> statements = new ArrayList<>();
        
        if (accountsResponse != null && accountsResponse.containsKey("content")) {
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) accountsResponse.get("content");
            
            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1).minusMonths(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            for (Map<String, Object> account : accounts) {
                UUID accountId = getUUID(account, "id");
                if (accountId != null) {
                    statements.add(StatementResponse.builder()
                            .id(UUID.randomUUID())
                            .accountId(accountId)
                            .accountNumber((String) account.get("accountNumber"))
                            .period(monthStart.getMonth().name() + " " + monthStart.getYear())
                            .startDate(monthStart)
                            .endDate(monthEnd)
                            .openingBalance(getBigDecimal(account, "balance"))
                            .closingBalance(getBigDecimal(account, "balance"))
                            .totalDeposits(BigDecimal.ZERO)
                            .totalWithdrawals(BigDecimal.ZERO)
                            .transactionCount(0)
                            .generatedAt(LocalDateTime.now())
                            .status("AVAILABLE")
                            .build());
                }
            }
        }
        
        return statements;
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

    // DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementResponse {
        private UUID id;
        private UUID accountId;
        private String accountNumber;
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal openingBalance;
        private BigDecimal closingBalance;
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private int transactionCount;
        private LocalDateTime generatedAt;
        private String status;
        private String downloadUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateStatementRequest {
        private UUID accountId;
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
