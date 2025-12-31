package com.RRBank.banking.service;

import com.RRBank.banking.dto.CreatePayeeRequestDto;
import com.RRBank.banking.dto.PayeeResponseDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Payee;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.PayeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payee Service
 * Phase 3: CRUD operations for payees/beneficiaries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayeeService {

    private final PayeeRepository payeeRepository;
    private final AccountRepository accountRepository;

    /**
     * Create a new payee
     */
    @Transactional
    public PayeeResponseDto createPayee(CreatePayeeRequestDto request) {
        log.info("Creating payee for customer {}: {}", request.getCustomerId(), request.getNickname());

        // Check for duplicate
        if (payeeRepository.existsByCustomerIdAndPayeeAccountNumber(
                request.getCustomerId(), request.getPayeeAccountNumber())) {
            throw new IllegalStateException("Payee with this account number already exists");
        }

        // Check if this is an internal account
        boolean isInternal = false;
        UUID internalAccountId = null;
        Account internalAccount = accountRepository.findByAccountNumber(request.getPayeeAccountNumber()).orElse(null);
        if (internalAccount != null) {
            isInternal = true;
            internalAccountId = internalAccount.getId();
        }

        Payee.PayeeType payeeType = request.getPayeeType() != null ?
                Payee.PayeeType.valueOf(request.getPayeeType().toUpperCase()) :
                Payee.PayeeType.INDIVIDUAL;

        Payee payee = Payee.builder()
                .customerId(request.getCustomerId())
                .nickname(request.getNickname())
                .payeeName(request.getPayeeName())
                .payeeAccountNumber(request.getPayeeAccountNumber())
                .payeeBankCode(request.getPayeeBankCode())
                .payeeBankName(request.getPayeeBankName())
                .payeeRoutingNumber(request.getPayeeRoutingNumber())
                .payeeType(payeeType)
                .transferLimit(request.getTransferLimit())
                .dailyLimit(request.getDailyLimit())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .isInternal(isInternal)
                .internalAccountId(internalAccountId)
                .status(isInternal ? Payee.PayeeStatus.ACTIVE : Payee.PayeeStatus.PENDING_VERIFICATION)
                .isVerified(isInternal)
                .build();

        payee = payeeRepository.save(payee);
        log.info("Payee created: {}", payee.getId());

        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Get payee by ID
     */
    @Transactional(readOnly = true)
    public PayeeResponseDto getPayeeById(UUID payeeId) {
        Payee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee not found: " + payeeId));
        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Get all payees for customer
     */
    @Transactional(readOnly = true)
    public List<PayeeResponseDto> getPayeesByCustomer(UUID customerId) {
        return payeeRepository.findByCustomerIdOrderByNicknameAsc(customerId).stream()
                .map(PayeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active payees for customer
     */
    @Transactional(readOnly = true)
    public List<PayeeResponseDto> getActivePayees(UUID customerId) {
        return payeeRepository.findActivePayeesByCustomer(customerId).stream()
                .map(PayeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get verified payees for customer
     */
    @Transactional(readOnly = true)
    public List<PayeeResponseDto> getVerifiedPayees(UUID customerId) {
        return payeeRepository.findVerifiedPayeesByCustomer(customerId).stream()
                .map(PayeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update payee
     */
    @Transactional
    public PayeeResponseDto updatePayee(UUID payeeId, CreatePayeeRequestDto request) {
        log.info("Updating payee: {}", payeeId);

        Payee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee not found: " + payeeId));

        payee.setNickname(request.getNickname());
        payee.setPayeeName(request.getPayeeName());
        payee.setEmail(request.getEmail());
        payee.setPhone(request.getPhone());
        payee.setTransferLimit(request.getTransferLimit());
        payee.setDailyLimit(request.getDailyLimit());

        if (request.getNotes() != null) {
            payee.setNotes(request.getNotes());
        }

        payee = payeeRepository.save(payee);
        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Verify payee (admin action)
     */
    @Transactional
    public PayeeResponseDto verifyPayee(UUID payeeId, UUID verifiedBy) {
        log.info("Verifying payee: {} by {}", payeeId, verifiedBy);

        Payee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee not found: " + payeeId));

        payee.verify(verifiedBy);
        payee = payeeRepository.save(payee);

        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Deactivate payee
     */
    @Transactional
    public PayeeResponseDto deactivatePayee(UUID payeeId) {
        log.info("Deactivating payee: {}", payeeId);

        Payee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee not found: " + payeeId));

        payee.deactivate();
        payee = payeeRepository.save(payee);

        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Block payee
     */
    @Transactional
    public PayeeResponseDto blockPayee(UUID payeeId) {
        log.info("Blocking payee: {}", payeeId);

        Payee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payee not found: " + payeeId));

        payee.block();
        payee = payeeRepository.save(payee);

        return PayeeResponseDto.fromEntity(payee);
    }

    /**
     * Delete payee
     */
    @Transactional
    public void deletePayee(UUID payeeId) {
        log.info("Deleting payee: {}", payeeId);

        if (!payeeRepository.existsById(payeeId)) {
            throw new ResourceNotFoundException("Payee not found: " + payeeId);
        }

        payeeRepository.deleteById(payeeId);
    }

    /**
     * Search payees
     */
    @Transactional(readOnly = true)
    public List<PayeeResponseDto> searchPayees(UUID customerId, String query) {
        return payeeRepository.searchByCustomer(customerId, query).stream()
                .map(PayeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if transfer requires verified payee
     */
    @Transactional(readOnly = true)
    public boolean requiresVerifiedPayee(UUID payeeId, BigDecimal amount) {
        Payee payee = payeeRepository.findById(payeeId).orElse(null);
        if (payee == null) return true;
        return payee.requiresVerificationForAmount(amount);
    }
}
