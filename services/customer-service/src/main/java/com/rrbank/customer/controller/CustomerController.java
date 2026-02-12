package com.rrbank.customer.controller;

import com.rrbank.customer.dto.CustomerDTOs.*;
import com.rrbank.customer.entity.Customer;
import com.rrbank.customer.entity.Customer.KycStatus;
import com.rrbank.customer.repository.CustomerRepository;
import com.rrbank.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CreateCustomerRequest request) {
        log.info("=== CREATE CUSTOMER REQUEST ===");
        log.info("Received: userId={}, email={}, firstName={}, lastName={}", 
                request.getUserId(), request.getEmail(), request.getFirstName(), request.getLastName());
        
        try {
            // Check if customer already exists for this user
            if (request.getUserId() != null) {
                UUID userId = UUID.fromString(request.getUserId());
                if (customerRepository.existsByUserId(userId)) {
                    log.info("Customer already exists for userId: {}", userId);
                    Customer existing = customerRepository.findByUserId(userId).get();
                    return ResponseEntity.ok(existing);
                }
            }
            
            // Build customer entity from request
            Customer customer = Customer.builder()
                    .userId(request.getUserId() != null ? UUID.fromString(request.getUserId()) : null)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .postalCode(request.getPostalCode())
                    .country(request.getCountry())
                    .dateOfBirth(request.getDateOfBirth())
                    .kycVerified(request.getKycVerified() != null ? request.getKycVerified() : false)
                    .build();
            
            Customer saved = customerService.createCustomer(customer);
            log.info("=== CUSTOMER CREATED: id={} ===", saved.getId());
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            log.error("=== CUSTOMER CREATION FAILED: {} ===", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Page<Customer>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET all customers - page: {}, size: {}, search: {}", page, size, search);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Customer> customers;
        if (search != null && !search.isEmpty()) {
            customers = customerRepository.searchCustomers(search, pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        log.info("GET customer stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total customers
            long totalCustomers = customerRepository.count();
            stats.put("totalCustomers", totalCustomers);
            log.info("Total customers in database: {}", totalCustomers);
            
            // Verified customers (KYC completed)
            long verifiedCustomers = customerRepository.countByKycVerified(true);
            stats.put("verifiedCustomers", verifiedCustomers);
            
            // Pending KYC
            long pendingKyc = customerRepository.countByKycVerified(false);
            stats.put("pendingKyc", pendingKyc);
            
            // New customers today
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            long newCustomersToday = customerRepository.countByCreatedAtAfter(startOfDay);
            stats.put("newCustomersToday", newCustomersToday);
            
            // New customers this week
            LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
            long newCustomersThisWeek = customerRepository.countByCreatedAtAfter(startOfWeek);
            stats.put("newCustomersThisWeek", newCustomersThisWeek);
            
            // New customers this month
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            long newCustomersThisMonth = customerRepository.countByCreatedAtAfter(startOfMonth);
            stats.put("newCustomersThisMonth", newCustomersThisMonth);
            
            log.info("Customer stats: {}", stats);
        } catch (Exception e) {
            log.error("Error getting customer stats: {}", e.getMessage(), e);
            stats.put("totalCustomers", 0L);
            stats.put("verifiedCustomers", 0L);
            stats.put("pendingKyc", 0L);
            stats.put("newCustomersToday", 0L);
            stats.put("newCustomersThisWeek", 0L);
            stats.put("newCustomersThisMonth", 0L);
        }
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getCustomerByUserId(@PathVariable UUID userId) {
        try {
            Customer customer = customerService.getCustomerByUserId(userId);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.warn("Customer not found for userId: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-User-Id header required"));
        }
        try {
            Customer customer = customerService.getCustomerByUserId(UUID.fromString(userId));
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.warn("Customer not found for userId: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable UUID customerId,
            @RequestBody UpdateCustomerRequest request) {
        
        Customer updates = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .dateOfBirth(request.getDateOfBirth())
                .build();
        
        return ResponseEntity.ok(customerService.updateCustomer(customerId, updates));
    }

    @PostMapping("/{customerId}/verify-kyc")
    public ResponseEntity<Customer> verifyKyc(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.verifyKyc(customerId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Customer Service is healthy");
    }
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        long count = customerRepository.count();
        log.info("Customer count: {}", count);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ==================== KYC STATUS ENDPOINTS ====================

    @GetMapping("/kyc/pending")
    public ResponseEntity<Page<Customer>> getPendingKycCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET pending KYC customers - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Customer> customers = customerService.getCustomersByKycStatus(KycStatus.PENDING, pageable);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/kyc/status/{status}")
    public ResponseEntity<Page<Customer>> getCustomersByKycStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET customers by KYC status: {} - page: {}, size: {}", status, page, size);
        try {
            KycStatus kycStatus = KycStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Customer> customers = customerService.getCustomersByKycStatus(kycStatus, pageable);
            return ResponseEntity.ok(customers);
        } catch (IllegalArgumentException e) {
            log.error("Invalid KYC status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/kyc/stats")
    public ResponseEntity<Map<String, Object>> getKycStats() {
        log.info("GET KYC stats");
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", customerService.countByKycStatus(KycStatus.PENDING));
        stats.put("approved", customerService.countByKycStatus(KycStatus.APPROVED));
        stats.put("rejected", customerService.countByKycStatus(KycStatus.REJECTED));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{userId}/kyc-status")
    public ResponseEntity<Map<String, String>> getKycStatusByUserId(@PathVariable UUID userId) {
        log.info("GET KYC status for user: {}", userId);
        try {
            KycStatus status = customerService.getKycStatusByUserId(userId);
            Customer customer = customerService.getCustomerByUserId(userId);
            Map<String, String> response = new HashMap<>();
            response.put("kycStatus", status.name());
            response.put("rejectionReason", customer.getKycRejectionReason());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Customer not found for userId: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{customerId}/kyc/approve")
    public ResponseEntity<Customer> approveKyc(@PathVariable UUID customerId) {
        log.info("POST approve KYC for customer: {}", customerId);
        try {
            Customer customer = customerService.approveKyc(customerId);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Failed to approve KYC for customer: {}", customerId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/user/{userId}/kyc/approve")
    public ResponseEntity<Customer> approveKycByUserId(@PathVariable UUID userId) {
        log.info("POST approve KYC for user: {}", userId);
        try {
            Customer customer = customerService.approveKycByUserId(userId);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Failed to approve KYC for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{customerId}/kyc/reject")
    public ResponseEntity<Customer> rejectKyc(
            @PathVariable UUID customerId,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("POST reject KYC for customer: {}", customerId);
        try {
            String reason = body != null ? body.get("reason") : null;
            Customer customer = customerService.rejectKyc(customerId, reason);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Failed to reject KYC for customer: {}", customerId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/user/{userId}/kyc/reject")
    public ResponseEntity<Customer> rejectKycByUserId(
            @PathVariable UUID userId,
            @RequestBody(required = false) Map<String, String> body) {
        log.info("POST reject KYC for user: {}", userId);
        try {
            String reason = body != null ? body.get("reason") : null;
            Customer customer = customerService.rejectKycByUserId(userId, reason);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Failed to reject KYC for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
