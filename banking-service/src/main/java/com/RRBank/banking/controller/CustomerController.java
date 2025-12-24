package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.service.CustomerService;
import com.RRBank.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Customer Controller
 * REST API endpoints for customer management
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    /**
     * Create new customer profile
     * POST /api/customers
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CreateCustomerDto dto) {
        log.info("REST request to create customer for userId: {}", dto.getUserId());
        CustomerResponseDto response = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get customer by ID
     * GET /api/customers/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable UUID id) {
        log.info("REST request to get customer by ID: {}", id);
        CustomerResponseDto response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get customer by user ID
     * GET /api/customers/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> getCustomerByUserId(@PathVariable UUID userId) {
        log.info("REST request to get customer by userId: {}", userId);
        CustomerResponseDto response = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update customer profile
     * PUT /api/customers/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerDto dto) {
        log.info("REST request to update customer with ID: {}", id);
        CustomerResponseDto response = customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit KYC for verification
     * POST /api/customers/kyc
     */
    @PostMapping("/kyc")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> submitKyc(@Valid @RequestBody KycRequestDto dto) {
        log.info("REST request to submit KYC for customer ID: {}", dto.getCustomerId());
        CustomerResponseDto response = customerService.submitKyc(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify KYC (Admin only)
     * PUT /api/customers/{id}/kyc/verify
     */
    @PutMapping("/{id}/kyc/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> verifyKyc(
            @PathVariable UUID id,
            @RequestParam boolean approved,
            @RequestParam(required = false, defaultValue = "SYSTEM") String verifiedBy) {
        log.info("REST request to verify KYC for customer ID: {}, approved: {}", id, approved);
        CustomerResponseDto response = customerService.verifyKyc(id, approved, verifiedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all customers (Admin only)
     * GET /api/customers
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        log.info("REST request to get all customers");
        List<CustomerResponseDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * Search customers by name
     * GET /api/customers/search?query=...
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponseDto>> searchCustomers(@RequestParam String query) {
        log.info("REST request to search customers with query: {}", query);
        List<CustomerResponseDto> customers = customerService.searchCustomers(query);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get customers by KYC status
     * GET /api/customers/kyc/status/{status}
     */
    @GetMapping("/kyc/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponseDto>> getCustomersByKycStatus(@PathVariable String status) {
        log.info("REST request to get customers by KYC status: {}", status);
        List<CustomerResponseDto> customers = customerService.getCustomersByKycStatus(status);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get pending KYC customers
     * GET /api/customers/kyc/pending
     */
    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponseDto>> getPendingKycCustomers() {
        log.info("REST request to get pending KYC customers");
        List<CustomerResponseDto> customers = customerService.getPendingKycCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * Delete customer (Admin only)
     * DELETE /api/customers/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        log.info("REST request to delete customer with ID: {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get customer's accounts
     * GET /api/customers/{id}/accounts
     */
    @GetMapping("/{id}/accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getCustomerAccounts(@PathVariable UUID id) {
        log.info("REST request to get accounts for customer ID: {}", id);
        // Delegate to Account Service
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomerId(id);
        return ResponseEntity.ok(accounts);
    }
}
