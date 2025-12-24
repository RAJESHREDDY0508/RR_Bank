package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Payment;
import com.RRBank.banking.event.*;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.PaymentRepository;
import com.RRBank.banking.util.PaymentReferenceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Service
 * Business logic for bill and merchant payments
 */
@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final PaymentGatewayService gatewayService;
    private final PaymentReferenceGenerator referenceGenerator;
    private final AccountService accountService;
    
    @Autowired(required = false)
    private PaymentEventProducer eventProducer;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                         AccountRepository accountRepository,
                         PaymentGatewayService gatewayService,
                         PaymentReferenceGenerator referenceGenerator,
                         AccountService accountService) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.gatewayService = gatewayService;
        this.referenceGenerator = referenceGenerator;
        this.accountService = accountService;
    }

    /**
     * Process bill payment
     */
    @Transactional
    public PaymentResponseDto processBillPayment(BillPaymentRequestDto request, UUID initiatedBy) {
        log.info("Processing bill payment for account: {}, payee: {}", 
                request.getAccountId(), request.getPayeeName());

        // Step 1: Validate account exists and is active
        Account account = validateAccount(request.getAccountId());

        // Step 2: Validate sufficient balance
        if (!account.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException("Insufficient balance for bill payment");
        }

        // Step 3: Create payment record
        String paymentRef = referenceGenerator.generatePaymentReferenceWithType("BILL");
        
        Payment.PaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? Payment.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase())
                : Payment.PaymentMethod.ACH;

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .accountId(request.getAccountId())
                .customerId(account.getCustomerId())
                .paymentType(Payment.PaymentType.BILL)
                .payeeName(request.getPayeeName())
                .payeeAccount(request.getPayeeAccount())
                .payeeReference(request.getPayeeReference())
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .description(request.getDescription())
                .scheduledDate(request.getScheduledDate() != null ? request.getScheduledDate() : LocalDate.now())
                .initiatedBy(initiatedBy)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Bill payment created with ID: {}, reference: {}", payment.getId(), paymentRef);

        // Step 4: Publish Payment Initiated Event
        publishPaymentInitiatedEvent(payment);

        // Step 5: Check if scheduled for future
        if (payment.isScheduledForFuture()) {
            payment.setStatus(Payment.PaymentStatus.SCHEDULED);
            payment = paymentRepository.save(payment);
            log.info("Bill payment scheduled for: {}", payment.getScheduledDate());
            return enrichPaymentResponse(payment);
        }

        // Step 6: Process payment immediately
        try {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            // Debit account
            accountService.debitAccount(account.getId(), request.getAmount(), payment.getId().toString());

            // Process through payment gateway
            PaymentGatewayService.GatewayResponse gatewayResponse = gatewayService.processBillPayment(
                    request.getPayeeName(),
                    request.getPayeeAccount(),
                    request.getAmount(),
                    paymentMethod.name()
            );

            if (gatewayResponse.isSuccess()) {
                // Mark payment as completed
                payment.markCompleted(gatewayResponse.getGatewayTransactionId());
                payment.setGatewayResponse(gatewayResponse.getResponseMessage());
                payment = paymentRepository.save(payment);
                
                log.info("Bill payment completed successfully: {}", payment.getId());
                
                // Publish Payment Completed Event
                publishPaymentCompletedEvent(payment);
                
                return enrichPaymentResponse(payment);
            } else {
                // Payment failed at gateway - refund account
                accountService.creditAccount(account.getId(), request.getAmount(), 
                        payment.getId().toString() + "-REFUND");
                
                payment.markFailed(gatewayResponse.getResponseMessage());
                payment.setGatewayResponse(gatewayResponse.getResponseMessage());
                payment = paymentRepository.save(payment);
                
                log.error("Bill payment failed at gateway: {}", payment.getId());
                
                // Publish Payment Failed Event
                publishPaymentFailedEvent(payment);
                
                throw new IllegalStateException("Payment failed: " + gatewayResponse.getResponseMessage());
            }
        } catch (Exception e) {
            log.error("Bill payment processing failed: {}", payment.getId(), e);
            
            // Attempt to refund if account was debited
            try {
                accountService.creditAccount(account.getId(), request.getAmount(), 
                        payment.getId().toString() + "-REFUND");
            } catch (Exception refundEx) {
                log.error("Failed to refund account after payment failure", refundEx);
            }
            
            payment.markFailed(e.getMessage());
            payment = paymentRepository.save(payment);
            
            publishPaymentFailedEvent(payment);
            
            throw new IllegalStateException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process merchant payment
     */
    @Transactional
    public PaymentResponseDto processMerchantPayment(MerchantPaymentRequestDto request, UUID initiatedBy) {
        log.info("Processing merchant payment for account: {}, merchant: {}", 
                request.getAccountId(), request.getMerchantName());

        // Step 1: Validate account
        Account account = validateAccount(request.getAccountId());

        // Step 2: Validate sufficient balance
        if (!account.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException("Insufficient balance for merchant payment");
        }

        // Step 3: Create payment record
        String paymentRef = referenceGenerator.generatePaymentReferenceWithType("MERCHANT");
        
        Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(
                request.getPaymentMethod().toUpperCase());

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .accountId(request.getAccountId())
                .customerId(account.getCustomerId())
                .paymentType(Payment.PaymentType.MERCHANT)
                .payeeName(request.getMerchantName())
                .payeeAccount(request.getMerchantId())
                .payeeReference(request.getOrderReference())
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .description(request.getDescription())
                .scheduledDate(LocalDate.now())
                .initiatedBy(initiatedBy)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Merchant payment created with ID: {}, reference: {}", payment.getId(), paymentRef);

        // Step 4: Publish Payment Initiated Event
        publishPaymentInitiatedEvent(payment);

        // Step 5: Process payment immediately (merchant payments are not scheduled)
        try {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            // Debit account
            accountService.debitAccount(account.getId(), request.getAmount(), payment.getId().toString());

            // Process through payment gateway
            PaymentGatewayService.GatewayResponse gatewayResponse = gatewayService.processMerchantPayment(
                    request.getMerchantName(),
                    request.getMerchantId(),
                    request.getAmount(),
                    paymentMethod.name()
            );

            if (gatewayResponse.isSuccess()) {
                payment.markCompleted(gatewayResponse.getGatewayTransactionId());
                payment.setGatewayResponse(gatewayResponse.getResponseMessage());
                payment = paymentRepository.save(payment);
                
                log.info("Merchant payment completed successfully: {}", payment.getId());
                
                publishPaymentCompletedEvent(payment);
                
                return enrichPaymentResponse(payment);
            } else {
                // Refund account
                accountService.creditAccount(account.getId(), request.getAmount(), 
                        payment.getId().toString() + "-REFUND");
                
                payment.markFailed(gatewayResponse.getResponseMessage());
                payment.setGatewayResponse(gatewayResponse.getResponseMessage());
                payment = paymentRepository.save(payment);
                
                log.error("Merchant payment failed at gateway: {}", payment.getId());
                
                publishPaymentFailedEvent(payment);
                
                throw new IllegalStateException("Payment failed: " + gatewayResponse.getResponseMessage());
            }
        } catch (Exception e) {
            log.error("Merchant payment processing failed: {}", payment.getId(), e);
            
            // Attempt to refund
            try {
                accountService.creditAccount(account.getId(), request.getAmount(), 
                        payment.getId().toString() + "-REFUND");
            } catch (Exception refundEx) {
                log.error("Failed to refund account after payment failure", refundEx);
            }
            
            payment.markFailed(e.getMessage());
            payment = paymentRepository.save(payment);
            
            publishPaymentFailedEvent(payment);
            
            throw new IllegalStateException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment by ID
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(UUID paymentId) {
        log.info("Fetching payment with ID: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        
        return enrichPaymentResponse(payment);
    }

    /**
     * Get payment by reference
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByReference(String reference) {
        log.info("Fetching payment with reference: {}", reference);
        
        Payment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + reference));
        
        return enrichPaymentResponse(payment);
    }

    /**
     * Get all payments for a customer
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByCustomerId(UUID customerId) {
        log.info("Fetching payments for customerId: {}", customerId);
        
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::enrichPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for an account
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByAccountId(UUID accountId) {
        log.info("Fetching payments for accountId: {}", accountId);
        
        return paymentRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::enrichPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent payments for customer
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getRecentPayments(UUID customerId, int limit) {
        log.info("Fetching {} recent payments for customerId: {}", limit, customerId);
        
        return paymentRepository.findRecentPaymentsByCustomer(customerId, limit).stream()
                .map(this::enrichPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments (Admin)
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        log.info("Fetching all payments");
        
        return paymentRepository.findAll().stream()
                .map(this::enrichPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get total payment amount for customer
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentAmount(UUID customerId) {
        log.info("Calculating total payment amount for customerId: {}", customerId);
        
        return paymentRepository.getTotalPaymentAmountByCustomer(customerId);
    }

    // ========== HELPER METHODS ==========

    private Account validateAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        
        if (!account.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        
        return account;
    }

    private PaymentResponseDto enrichPaymentResponse(Payment payment) {
        PaymentResponseDto dto = PaymentResponseDto.fromEntity(payment);
        
        // Enrich with account number
        accountRepository.findById(payment.getAccountId())
                .ifPresent(acc -> dto.setAccountNumber(acc.getAccountNumber()));
        
        return dto;
    }

    // ========== EVENT PUBLISHING ==========

    private void publishPaymentInitiatedEvent(Payment payment) {
        if (eventProducer == null) {
            log.debug("Kafka is disabled, skipping PaymentInitiatedEvent");
            return;
        }
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .accountId(payment.getAccountId())
                .customerId(payment.getCustomerId())
                .paymentType(payment.getPaymentType().name())
                .payeeName(payment.getPayeeName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .initiatedAt(payment.getCreatedAt())
                .build();
        
        eventProducer.publishPaymentInitiated(event);
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        if (eventProducer == null) {
            log.debug("Kafka is disabled, skipping PaymentCompletedEvent");
            return;
        }
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .accountId(payment.getAccountId())
                .customerId(payment.getCustomerId())
                .paymentType(payment.getPaymentType().name())
                .payeeName(payment.getPayeeName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .completedAt(payment.getProcessedAt())
                .build();
        
        eventProducer.publishPaymentCompleted(event);
    }

    private void publishPaymentFailedEvent(Payment payment) {
        if (eventProducer == null) {
            log.debug("Kafka is disabled, skipping PaymentFailedEvent");
            return;
        }
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .accountId(payment.getAccountId())
                .customerId(payment.getCustomerId())
                .paymentType(payment.getPaymentType().name())
                .payeeName(payment.getPayeeName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .failureReason(payment.getFailureReason())
                .failedAt(payment.getProcessedAt())
                .build();
        
        eventProducer.publishPaymentFailed(event);
    }
}
