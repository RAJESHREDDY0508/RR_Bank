package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payment Controller
 * REST API endpoints for payment management
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process bill payment
     * POST /api/payments/bill
     */
    @PostMapping("/bill")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> processBillPayment(
            @Valid @RequestBody BillPaymentRequestDto request,
            Authentication authentication) {
        log.info("REST request to process bill payment to: {}", request.getPayeeName());
        
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        PaymentResponseDto response = paymentService.processBillPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Process merchant payment
     * POST /api/payments/merchant
     */
    @PostMapping("/merchant")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> processMerchantPayment(
            @Valid @RequestBody MerchantPaymentRequestDto request,
            Authentication authentication) {
        log.info("REST request to process merchant payment to: {}", request.getMerchantName());
        
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        PaymentResponseDto response = paymentService.processMerchantPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable UUID id) {
        log.info("REST request to get payment by ID: {}", id);
        PaymentResponseDto response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by reference
     * GET /api/payments/reference/{reference}
     */
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentByReference(@PathVariable String reference) {
        log.info("REST request to get payment by reference: {}", reference);
        PaymentResponseDto response = paymentService.getPaymentByReference(reference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for a customer
     * GET /api/payments/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByCustomerId(@PathVariable UUID customerId) {
        log.info("REST request to get payments for customerId: {}", customerId);
        List<PaymentResponseDto> payments = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments for an account
     * GET /api/payments/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByAccountId(@PathVariable UUID accountId) {
        log.info("REST request to get payments for accountId: {}", accountId);
        List<PaymentResponseDto> payments = paymentService.getPaymentsByAccountId(accountId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get recent payments for customer
     * GET /api/payments/customer/{customerId}/recent?limit=10
     */
    @GetMapping("/customer/{customerId}/recent")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getRecentPayments(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get {} recent payments for customerId: {}", limit, customerId);
        List<PaymentResponseDto> payments = paymentService.getRecentPayments(customerId, limit);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get total payment amount for customer
     * GET /api/payments/customer/{customerId}/total
     */
    @GetMapping("/customer/{customerId}/total")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalPaymentAmount(@PathVariable UUID customerId) {
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

    // ========== HELPER METHODS ==========

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse userId from authentication: {}", authentication.getName());
                return UUID.randomUUID(); // Placeholder
            }
        }
        return UUID.randomUUID(); // Placeholder
    }
}
