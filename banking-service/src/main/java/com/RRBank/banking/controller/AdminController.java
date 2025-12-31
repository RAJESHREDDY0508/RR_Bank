package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.entity.Transaction;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.*;
import com.RRBank.banking.security.CustomUserDetails;
import com.RRBank.banking.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Controller
 * REST API endpoints for admin operations
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AccountRequestService accountRequestService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionLimitService transactionLimitService;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ==================== DASHBOARD ====================

    /**
     * ✅ FIX: Added /api/admin/dashboard alias for frontend compatibility
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return getDashboardMetrics();
    }

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<Map<String, Object>> getDashboardMetrics() {
        log.info("REST request to get dashboard metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Total counts
        metrics.put("totalCustomers", customerRepository.count());
        metrics.put("totalAccounts", accountRepository.count());
        metrics.put("totalTransactions", transactionRepository.count());
        
        // Active accounts
        metrics.put("activeAccounts", accountRepository.countByStatus(Account.AccountStatus.ACTIVE));
        
        // Pending requests
        metrics.put("pendingAccountRequests", accountRequestService.countPendingRequests());
        
        // Today's transactions
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        metrics.put("todayTransactions", transactionRepository.countByCreatedAtAfter(startOfDay));
        
        // Total balance across all accounts
        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.put("totalBalance", totalBalance);
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/dashboard/transaction-stats")
    public ResponseEntity<Map<String, Object>> getTransactionStats(
            @RequestParam(defaultValue = "today") String period) {
        log.info("REST request to get transaction stats for period: {}", period);
        
        LocalDateTime startDate = switch (period) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            default -> LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        };
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("period", period);
        stats.put("startDate", startDate);
        stats.put("transactionCount", transactionRepository.countByCreatedAtAfter(startDate));
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get {} recent activities", limit);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Transaction> transactions = transactionRepository.findAll(pageable).getContent();
        
        List<Map<String, Object>> activities = transactions.stream()
                .map(t -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", t.getId());
                    activity.put("type", t.getTransactionType().name());
                    activity.put("amount", t.getAmount());
                    activity.put("status", t.getStatus().name());
                    activity.put("createdAt", t.getCreatedAt());
                    return activity;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(activities);
    }

    // ==================== ACCOUNT REQUESTS ====================

    @GetMapping("/account-requests")
    public ResponseEntity<Page<AccountRequestResponse>> getPendingAccountRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to get pending account requests - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AccountRequestResponse> requests = accountRequestService.getPendingRequests(pageable);
        
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/account-requests/count")
    public ResponseEntity<Map<String, Long>> getPendingRequestsCount() {
        long count = accountRequestService.countPendingRequests();
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    @PostMapping("/account-requests/{requestId}/approve")
    public ResponseEntity<AccountRequestResponse> approveAccountRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String adminId = extractUserIdString(authentication);
        String notes = body != null ? body.get("notes") : null;
        
        log.info("REST request to approve account request {} by admin {}", requestId, adminId);
        
        AccountRequestResponse response = accountRequestService.approveRequest(requestId, adminId, notes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account-requests/{requestId}/reject")
    public ResponseEntity<AccountRequestResponse> rejectAccountRequest(
            @PathVariable UUID requestId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String adminId = extractUserIdString(authentication);
        String notes = body.get("notes");
        
        log.info("REST request to reject account request {} by admin {}", requestId, adminId);
        
        AccountRequestResponse response = accountRequestService.rejectRequest(requestId, adminId, notes);
        return ResponseEntity.ok(response);
    }

    // ==================== CUSTOMERS ====================

    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerDto>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus) {
        log.info("REST request to get all customers");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Customer> customers = customerRepository.findAll(pageable);
        
        Page<CustomerDto> customerDtos = customers.map(this::toCustomerDto);
        return ResponseEntity.ok(customerDtos);
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable UUID customerId) {
        log.info("REST request to get customer: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        
        return ResponseEntity.ok(toCustomerDto(customer));
    }

    @PatchMapping("/customers/{customerId}/kyc-status")
    public ResponseEntity<CustomerDto> updateKycStatus(
            @PathVariable UUID customerId,
            @RequestBody Map<String, String> body) {
        log.info("REST request to update KYC status for customer: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        
        String kycStatusValue = body.get("kycStatus");
        customer.setKycStatus(Customer.KycStatus.valueOf(kycStatusValue));
        
        if ("VERIFIED".equals(kycStatusValue)) {
            customer.setKycVerifiedAt(LocalDateTime.now());
        }
        
        customer = customerRepository.save(customer);
        return ResponseEntity.ok(toCustomerDto(customer));
    }

    @PostMapping("/customers/{customerId}/kyc/approve")
    public ResponseEntity<CustomerDto> approveKyc(
            @PathVariable UUID customerId,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("REST request to approve KYC for customer: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        
        customer.setKycStatus(Customer.KycStatus.VERIFIED);
        customer.setKycVerifiedAt(LocalDateTime.now());
        
        customer = customerRepository.save(customer);
        return ResponseEntity.ok(toCustomerDto(customer));
    }

    @PostMapping("/customers/{customerId}/kyc/reject")
    public ResponseEntity<CustomerDto> rejectKyc(
            @PathVariable UUID customerId,
            @RequestBody Map<String, String> body) {
        log.info("REST request to reject KYC for customer: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        
        customer.setKycStatus(Customer.KycStatus.REJECTED);
        
        customer = customerRepository.save(customer);
        return ResponseEntity.ok(toCustomerDto(customer));
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<AccountResponseDto>> getCustomerAccounts(@PathVariable UUID customerId) {
        log.info("REST request to get accounts for customer: {}", customerId);
        
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    // ==================== ACCOUNTS ====================

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountResponseDto>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status) {
        log.info("REST request to get all accounts");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Account> accounts = accountRepository.findAll(pageable);
        
        // Use the static fromEntity method for consistent conversion
        Page<AccountResponseDto> accountDtos = accounts.map(AccountResponseDto::fromEntity);
        return ResponseEntity.ok(accountDtos);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponseDto> getAccount(@PathVariable UUID accountId) {
        log.info("REST request to get account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<AccountResponseDto> updateAccountStatus(
            @PathVariable UUID accountId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        log.info("REST request to update account status: {}", accountId);
        
        String statusValue = body.get("status");
        String reason = body.get("reason");
        String changedBy = extractUserIdString(authentication);
        
        UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
        dto.setStatus(statusValue);
        dto.setReason(reason);
        
        AccountResponseDto response = accountService.updateAccountStatus(accountId, dto, changedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/{accountId}/freeze")
    public ResponseEntity<AccountResponseDto> freezeAccount(
            @PathVariable UUID accountId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        log.info("REST request to freeze account: {}", accountId);
        
        String reason = body.get("reason");
        String changedBy = extractUserIdString(authentication);
        
        UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
        dto.setStatus("FROZEN");
        dto.setReason(reason);
        
        AccountResponseDto response = accountService.updateAccountStatus(accountId, dto, changedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/{accountId}/unfreeze")
    public ResponseEntity<AccountResponseDto> unfreezeAccount(
            @PathVariable UUID accountId,
            Authentication authentication) {
        log.info("REST request to unfreeze account: {}", accountId);
        
        String changedBy = extractUserIdString(authentication);
        
        UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
        dto.setStatus("ACTIVE");
        dto.setReason("Account unfrozen by admin");
        
        AccountResponseDto response = accountService.updateAccountStatus(accountId, dto, changedBy);
        return ResponseEntity.ok(response);
    }

    // ==================== TRANSACTIONS ====================

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponseDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String status) {
        log.info("REST request to get all transactions");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponseDto> transactions = transactionService.getAllTransactions(pageable);
        
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getTransaction(@PathVariable UUID transactionId) {
        log.info("REST request to get transaction: {}", transactionId);
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    @PostMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<TransactionResponseDto> approveTransaction(
            @PathVariable UUID transactionId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        log.info("REST request to approve transaction: {}", transactionId);
        
        // For now, just return the transaction as-is
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    @PostMapping("/transactions/{transactionId}/reject")
    public ResponseEntity<TransactionResponseDto> rejectTransaction(
            @PathVariable UUID transactionId,
            @RequestBody Map<String, String> body) {
        log.info("REST request to reject transaction: {}", transactionId);
        
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    // ==================== USER LIMITS ====================

    @GetMapping("/users/{userId}/limits")
    public ResponseEntity<List<TransactionLimitResponse>> getUserLimits(@PathVariable String userId) {
        log.info("REST request to get limits for user: {}", userId);
        
        List<TransactionLimitResponse> limits = transactionLimitService.getLimitsForUser(userId);
        return ResponseEntity.ok(limits);
    }

    @PutMapping("/users/{userId}/limits")
    public ResponseEntity<TransactionLimitResponse> updateUserLimits(
            @PathVariable String userId,
            @RequestParam String limitType,
            @RequestParam(required = false) BigDecimal dailyLimit,
            @RequestParam(required = false) BigDecimal perTransactionLimit,
            @RequestParam(required = false) BigDecimal monthlyLimit) {
        log.info("REST request to update limits for user: {}", userId);
        
        TransactionLimitResponse response = transactionLimitService.updateLimit(
                userId,
                com.RRBank.banking.entity.TransactionLimit.LimitType.valueOf(limitType),
                dailyLimit,
                perTransactionLimit,
                monthlyLimit
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== FRAUD ====================

    @GetMapping("/fraud/queue")
    public ResponseEntity<Map<String, Object>> getFraudQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to get fraud queue");
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of());
        result.put("totalPages", 0);
        result.put("totalElements", 0);
        result.put("number", page);
        result.put("size", size);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fraud/stats")
    public ResponseEntity<Map<String, Object>> getFraudStats() {
        log.info("REST request to get fraud stats");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", 0);
        stats.put("pendingAlerts", 0);
        stats.put("resolvedAlerts", 0);
        stats.put("highRiskAlerts", 0);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/fraud/{eventId}/approve")
    public ResponseEntity<Map<String, String>> approveFraudEvent(
            @PathVariable UUID eventId,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("REST request to approve fraud event: {}", eventId);
        return ResponseEntity.ok(Map.of("status", "approved", "eventId", eventId.toString()));
    }

    @PostMapping("/fraud/{eventId}/reject")
    public ResponseEntity<Map<String, String>> rejectFraudEvent(
            @PathVariable UUID eventId,
            @RequestBody Map<String, String> body) {
        log.info("REST request to reject fraud event: {}", eventId);
        return ResponseEntity.ok(Map.of("status", "rejected", "eventId", eventId.toString()));
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to get audit logs");
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of());
        result.put("totalPages", 0);
        result.put("totalElements", 0);
        result.put("number", page);
        result.put("size", size);
        
        return ResponseEntity.ok(result);
    }

    // ==================== REPORTS ====================

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to get reports");
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", List.of());
        result.put("totalPages", 0);
        result.put("totalElements", 0);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, Object> params) {
        log.info("REST request to generate report with params: {}", params);
        
        Map<String, Object> report = new HashMap<>();
        report.put("id", UUID.randomUUID().toString());
        report.put("status", "GENERATING");
        report.put("createdAt", LocalDateTime.now());
        
        return ResponseEntity.ok(report);
    }

    // ==================== HELPER METHODS ====================

    private String extractUserIdString(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
            return authentication.getName();
        }
        return null;
    }

    private CustomerDto toCustomerDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setUserId(customer.getUserId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setZipCode(customer.getZipCode());
        dto.setCountry(customer.getCountry());
        dto.setKycStatus(customer.getKycStatus() != null ? customer.getKycStatus().name() : "PENDING");
        dto.setCustomerSegment(customer.getCustomerSegment() != null ? customer.getCustomerSegment().name() : "REGULAR");
        dto.setCreatedAt(customer.getCreatedAt());
        
        // Get email from user if available
        if (customer.getUserId() != null) {
            userRepository.findById(customer.getUserId().toString())
                    .ifPresent(user -> dto.setEmail(user.getEmail()));
        }
        
        return dto;
    }
}
