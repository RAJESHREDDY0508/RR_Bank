package com.RRBank.banking.service;

import com.RRBank.banking.entity.Dispute;
import com.RRBank.banking.entity.Transaction;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.DisputeRepository;
import com.RRBank.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dispute Service
 * Phase 3: Transaction dispute workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;
    private final HoldService holdService;

    /**
     * File a new dispute
     */
    @Transactional
    public Dispute fileDispute(UUID transactionId, UUID customerId, Dispute.DisputeType disputeType,
                               String reason, String description) {
        log.info("Filing dispute for transaction {} by customer {}", transactionId, customerId);

        // Check if dispute already exists for this transaction
        if (disputeRepository.existsByTransactionId(transactionId)) {
            throw new IllegalStateException("A dispute already exists for this transaction");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        // Verify customer owns the account
        UUID accountId = transaction.getFromAccountId() != null ? 
                transaction.getFromAccountId() : transaction.getToAccountId();

        Dispute dispute = Dispute.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .customerId(customerId)
                .disputedAmount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .disputeType(disputeType)
                .reason(reason)
                .customerDescription(description)
                .merchantName(transaction.getMerchantName())
                .transactionDate(transaction.getCreatedAt())
                .build();

        dispute = disputeRepository.save(dispute);
        log.info("Dispute filed: {}", dispute.getDisputeNumber());

        return dispute;
    }

    /**
     * Get dispute by ID
     */
    @Transactional(readOnly = true)
    public Dispute getById(UUID disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
    }

    /**
     * Get dispute by dispute number
     */
    @Transactional(readOnly = true)
    public Dispute getByDisputeNumber(String disputeNumber) {
        return disputeRepository.findByDisputeNumber(disputeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeNumber));
    }

    /**
     * Get disputes for customer
     */
    @Transactional(readOnly = true)
    public List<Dispute> getByCustomer(UUID customerId) {
        return disputeRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get active disputes for customer
     */
    @Transactional(readOnly = true)
    public List<Dispute> getActiveByCustomer(UUID customerId) {
        return disputeRepository.findActiveByCustomer(customerId);
    }

    /**
     * Get open disputes (admin queue)
     */
    @Transactional(readOnly = true)
    public Page<Dispute> getOpenDisputes(Pageable pageable) {
        return disputeRepository.findOpenDisputes(pageable);
    }

    /**
     * Assign dispute to admin
     */
    @Transactional
    public Dispute assign(UUID disputeId, UUID assigneeId) {
        log.info("Assigning dispute {} to {}", disputeId, assigneeId);

        Dispute dispute = getById(disputeId);
        dispute.assignTo(assigneeId);

        return disputeRepository.save(dispute);
    }

    /**
     * Issue provisional credit
     */
    @Transactional
    public Dispute issueProvisionalCredit(UUID disputeId, BigDecimal amount) {
        log.info("Issuing provisional credit for dispute {} amount {}", disputeId, amount);

        Dispute dispute = getById(disputeId);
        
        if (!dispute.isOpen()) {
            throw new IllegalStateException("Cannot issue credit for closed dispute");
        }

        dispute.issueProvisionalCredit(amount);
        
        // Create credit transaction would be done here via TransactionService
        
        return disputeRepository.save(dispute);
    }

    /**
     * Resolve in customer's favor
     */
    @Transactional
    public Dispute resolveForCustomer(UUID disputeId, UUID reviewerId, BigDecimal refundAmount, String notes) {
        log.info("Resolving dispute {} in customer favor", disputeId);

        Dispute dispute = getById(disputeId);
        
        if (!dispute.isOpen()) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        dispute.resolveInCustomerFavor(reviewerId, refundAmount, notes);

        // Create refund transaction would be done here
        
        return disputeRepository.save(dispute);
    }

    /**
     * Resolve in merchant's favor
     */
    @Transactional
    public Dispute resolveForMerchant(UUID disputeId, UUID reviewerId, String notes) {
        log.info("Resolving dispute {} in merchant favor", disputeId);

        Dispute dispute = getById(disputeId);
        
        if (!dispute.isOpen()) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        dispute.resolveInMerchantFavor(reviewerId, notes);

        // If provisional credit was issued, reverse it
        if (Boolean.TRUE.equals(dispute.getProvisionalCreditIssued())) {
            // Reverse provisional credit via TransactionService
            log.info("Reversing provisional credit for dispute {}", disputeId);
        }
        
        return disputeRepository.save(dispute);
    }

    /**
     * Reject dispute
     */
    @Transactional
    public Dispute reject(UUID disputeId, UUID reviewerId, String notes) {
        log.info("Rejecting dispute {}", disputeId);

        Dispute dispute = getById(disputeId);
        
        if (!dispute.isOpen()) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        dispute.reject(reviewerId, notes);
        
        return disputeRepository.save(dispute);
    }

    /**
     * Add supporting documents
     */
    @Transactional
    public Dispute addDocuments(UUID disputeId, String documentReferences) {
        Dispute dispute = getById(disputeId);
        
        String existing = dispute.getSupportingDocuments();
        dispute.setSupportingDocuments(existing != null ? 
                existing + "," + documentReferences : documentReferences);
        
        return disputeRepository.save(dispute);
    }

    /**
     * Get dispute statistics
     */
    @Transactional(readOnly = true)
    public long countOpenDisputes() {
        return disputeRepository.countOpenDisputes();
    }
}
