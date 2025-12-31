package com.RRBank.banking.service;

import com.RRBank.banking.entity.ScheduledPayment;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled Payment Service
 * Phase 3: Handles recurring and scheduled transfers/payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final TransactionService transactionService;
    private final PaymentService paymentService;

    /**
     * Create a scheduled payment
     */
    @Transactional
    public ScheduledPayment createScheduledPayment(UUID customerId, UUID accountId,
                                                   ScheduledPayment.PaymentType paymentType,
                                                   BigDecimal amount, String currency,
                                                   ScheduledPayment.Frequency frequency,
                                                   LocalDate startDate, LocalDate endDate,
                                                   Integer maxExecutions, String description,
                                                   UUID payeeId, UUID toAccountId,
                                                   String toAccountNumber, String payeeName,
                                                   UUID createdBy) {
        log.info("Creating scheduled payment for customer {} account {}", customerId, accountId);

        ScheduledPayment scheduled = ScheduledPayment.builder()
                .customerId(customerId)
                .accountId(accountId)
                .paymentType(paymentType)
                .amount(amount)
                .currency(currency != null ? currency : "USD")
                .frequency(frequency)
                .startDate(startDate)
                .endDate(endDate)
                .nextExecutionDate(startDate)
                .maxExecutions(maxExecutions)
                .description(description)
                .payeeId(payeeId)
                .toAccountId(toAccountId)
                .toAccountNumber(toAccountNumber)
                .payeeName(payeeName)
                .createdBy(createdBy)
                .build();

        scheduled = scheduledPaymentRepository.save(scheduled);
        log.info("Scheduled payment created: {}", scheduled.getScheduleReference());

        return scheduled;
    }

    /**
     * Get scheduled payment by ID
     */
    @Transactional(readOnly = true)
    public ScheduledPayment getById(UUID id) {
        return scheduledPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled payment not found: " + id));
    }

    /**
     * Get scheduled payments for customer
     */
    @Transactional(readOnly = true)
    public List<ScheduledPayment> getByCustomer(UUID customerId) {
        return scheduledPaymentRepository.findByCustomerIdOrderByNextExecutionDateAsc(customerId);
    }

    /**
     * Get active scheduled payments for customer
     */
    @Transactional(readOnly = true)
    public List<ScheduledPayment> getActiveByCustomer(UUID customerId) {
        return scheduledPaymentRepository.findActiveByCustomer(customerId);
    }

    /**
     * Pause scheduled payment
     */
    @Transactional
    public ScheduledPayment pause(UUID id) {
        ScheduledPayment scheduled = getById(id);
        scheduled.pause();
        return scheduledPaymentRepository.save(scheduled);
    }

    /**
     * Resume scheduled payment
     */
    @Transactional
    public ScheduledPayment resume(UUID id) {
        ScheduledPayment scheduled = getById(id);
        scheduled.resume();
        return scheduledPaymentRepository.save(scheduled);
    }

    /**
     * Cancel scheduled payment
     */
    @Transactional
    public ScheduledPayment cancel(UUID id) {
        ScheduledPayment scheduled = getById(id);
        scheduled.cancel();
        return scheduledPaymentRepository.save(scheduled);
    }

    /**
     * Update scheduled payment amount
     */
    @Transactional
    public ScheduledPayment updateAmount(UUID id, BigDecimal newAmount) {
        ScheduledPayment scheduled = getById(id);
        scheduled.setAmount(newAmount);
        return scheduledPaymentRepository.save(scheduled);
    }

    /**
     * Execute due scheduled payments
     * Runs daily at 6 AM
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void executeDuePayments() {
        log.info("Executing due scheduled payments");

        List<ScheduledPayment> duePayments = scheduledPaymentRepository.findDueForExecution(LocalDate.now());
        log.info("Found {} due scheduled payments", duePayments.size());

        for (ScheduledPayment scheduled : duePayments) {
            try {
                executePayment(scheduled);
            } catch (Exception e) {
                log.error("Failed to execute scheduled payment {}: {}", 
                        scheduled.getScheduleReference(), e.getMessage());
                scheduled.recordExecution(false, e.getMessage());
                scheduledPaymentRepository.save(scheduled);
            }
        }
    }

    /**
     * Execute a single scheduled payment
     */
    @Transactional
    public void executePayment(ScheduledPayment scheduled) {
        log.info("Executing scheduled payment: {}", scheduled.getScheduleReference());

        try {
            switch (scheduled.getPaymentType()) {
                case TRANSFER -> executeTransfer(scheduled);
                case BILL_PAYMENT -> executeBillPayment(scheduled);
                case MERCHANT_PAYMENT -> executeMerchantPayment(scheduled);
            }

            scheduled.recordExecution(true, null);
            log.info("Scheduled payment executed successfully: {}", scheduled.getScheduleReference());

        } catch (Exception e) {
            log.error("Scheduled payment execution failed: {}", e.getMessage());
            scheduled.recordExecution(false, e.getMessage());
            throw e;
        }

        scheduledPaymentRepository.save(scheduled);
    }

    private void executeTransfer(ScheduledPayment scheduled) {
        if (scheduled.getToAccountId() == null) {
            throw new IllegalStateException("Transfer requires toAccountId");
        }

        com.RRBank.banking.dto.TransferRequestDto request = new com.RRBank.banking.dto.TransferRequestDto();
        request.setFromAccountId(scheduled.getAccountId());
        request.setToAccountId(scheduled.getToAccountId());
        request.setAmount(scheduled.getAmount());
        request.setDescription("Scheduled: " + scheduled.getDescription());
        request.setIdempotencyKey("SCH-" + scheduled.getId() + "-" + scheduled.getExecutionCount());

        transactionService.transfer(request, scheduled.getCreatedBy());
    }

    private void executeBillPayment(ScheduledPayment scheduled) {
        com.RRBank.banking.dto.BillPaymentRequestDto request = new com.RRBank.banking.dto.BillPaymentRequestDto();
        request.setAccountId(scheduled.getAccountId());
        request.setAmount(scheduled.getAmount());
        request.setPayeeName(scheduled.getPayeeName());
        request.setPayeeAccount(scheduled.getToAccountNumber());
        request.setDescription("Scheduled: " + scheduled.getDescription());

        String idempotencyKey = "SCH-" + scheduled.getId() + "-" + scheduled.getExecutionCount();
        paymentService.processBillPayment(request, idempotencyKey, scheduled.getCreatedBy());
    }

    private void executeMerchantPayment(ScheduledPayment scheduled) {
        com.RRBank.banking.dto.MerchantPaymentRequestDto request = new com.RRBank.banking.dto.MerchantPaymentRequestDto();
        request.setAccountId(scheduled.getAccountId());
        request.setAmount(scheduled.getAmount());
        request.setMerchantName(scheduled.getPayeeName());
        request.setMerchantId(scheduled.getToAccountNumber());
        request.setDescription("Scheduled: " + scheduled.getDescription());
        request.setPaymentMethod("ACH");

        String idempotencyKey = "SCH-" + scheduled.getId() + "-" + scheduled.getExecutionCount();
        paymentService.processMerchantPayment(request, idempotencyKey, scheduled.getCreatedBy());
    }
}
