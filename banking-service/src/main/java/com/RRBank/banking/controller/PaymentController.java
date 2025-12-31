package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.service.OwnershipService;
import com.RRBank.banking.service.PaymentService;
import com.RRBank.banking.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payment Controller
 * REST API endpoints for payment management
 * 
 * Phase 2A.1: Account ownership enforcement
 * - Customer can only make payments from their own accounts
 * - Admin can process any payment
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OwnershipService ownershipService;

    /**
     * Process bill payment
     * POST /api/payments/bill
     * 
     * Phase 2A.1: Ownership enforcement - customer can only pay from their own accounts
     */
    @PostMapping("/bill")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> processBillPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BillPaymentRequestDto request,
            Authentication authentication) {
        
        // Ownership check: customer can only pay from their own accounts
        Account account = ownershipService.requireAccountOwnership(request.getAccountId(), authentication);
        log.info("REST request to process bill payment to: {} from account: {}", 
                request.getPayeeName(), request.getAccountId());
        
        UUID userId = SecurityUtil.requireUserId(authentication);
        
        PaymentResponseDto response = paymentService.processBillPayment(request, idempotencyKey, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Process merchant payment
     * POST /api/payments/merchant
     * 
     * Phase 2A.1: Ownership enforcement - customer can only pay from their own accounts
     */
    @PostMapping("/merchant")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> processMerchantPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MerchantPaymentRequestDto request,
            Authentication authentication) {
        
        // Ownership check: customer can only pay from their own accounts
        Account account = ownershipService.requireAccountOwnership(request.getAccountId(), authentication);
        log.info("REST request to process merchant payment to: {} from account: {}", 
                request.getMerchantName(), request.getAccountId());
        
        UUID userId = SecurityUtil.requireUserId(authentication);
        
        PaymentResponseDto response = paymentService.processMerchantPayment(request, idempotencyKey, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable UUID id,
            Authentication authentication) {
        log.info("REST request to get payment by ID: {}", id);
        
        PaymentResponseDto response = paymentService.getPaymentById(id);
        
        // Ownership check: verify user can access the account
        // PaymentResponseDto.getAccountId() returns UUID
        if (!SecurityUtil.isAdmin(authentication) && response.getAccountId() != null) {
            ownershipService.requireAccountOwnership(response.getAccountId(), authentication);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by reference
     * GET /api/payments/reference/{reference}
     */
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentByReference(
            @PathVariable String reference,
            Authentication authentication) {
        log.info("REST request to get payment by reference: {}", reference);
        
        PaymentResponseDto response = paymentService.getPaymentByReference(reference);
        
        // Ownership check - getAccountId() returns UUID
        if (!SecurityUtil.isAdmin(authentication) && response.getAccountId() != null) {
            ownershipService.requireAccountOwnership(response.getAccountId(), authentication);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for a customer
     * GET /api/payments/customer/{customerId}
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByCustomerId(
            @PathVariable UUID customerId,
            Authentication authentication) {
        
        // Ownership check: verify customer profile ownership
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get payments for customerId: {}", customerId);
        
        List<PaymentResponseDto> payments = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments for an account
     * GET /api/payments/account/{accountId}
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByAccountId(
            @PathVariable UUID accountId,
            Authentication authentication) {
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to get payments for accountId: {}", accountId);
        
        List<PaymentResponseDto> payments = paymentService.getPaymentsByAccountId(accountId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get recent payments for customer
     * GET /api/payments/customer/{customerId}/recent?limit=10
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/customer/{customerId}/recent")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getRecentPayments(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        // Ownership check
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get {} recent payments for customerId: {}", limit, customerId);
        
        List<PaymentResponseDto> payments = paymentService.getRecentPayments(customerId, limit);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get total payment amount for customer
     * GET /api/payments/customer/{customerId}/total
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/customer/{customerId}/total")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalPaymentAmount(
            @PathVariable UUID customerId,
            Authentication authentication) {
        
        // Ownership check
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get total payment amount for customerId: {}", customerId);
        
        BigDecimal total = paymentService.getTotalPaymentAmount(customerId);
        return ResponseEntity.ok(total);
    }

    /**
     * Get all payments (Admin only)
     * GET /api/payments
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        log.info("REST request to get all payments");
        List<PaymentResponseDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }
}
