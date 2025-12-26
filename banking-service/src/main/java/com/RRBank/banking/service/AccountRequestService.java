package com.RRBank.banking.service;

import com.RRBank.banking.dto.AccountOpenRequest;
import com.RRBank.banking.dto.AccountRequestResponse;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.AccountRequest;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.exception.BusinessException;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.AccountRequestRepository;
import com.RRBank.banking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Account Request Service - Bank-style account approval workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountRequestService {

    private final AccountRequestRepository requestRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LedgerService ledgerService;

    @Transactional
    public AccountRequestResponse submitRequest(String userId, AccountOpenRequest request) {
        log.info("User {} requesting {} account", userId, request.getAccountType());
        
        if (requestRepository.hasPendingRequest(userId, request.getAccountType())) {
            throw new BusinessException("You already have a pending request for " + request.getAccountType());
        }
        
        AccountRequest accountRequest = AccountRequest.builder()
            .userId(userId)
            .accountType(request.getAccountType())
            .initialDeposit(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO)
            .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
            .status(AccountRequest.RequestStatus.PENDING)
            .requestNotes(request.getNotes())
            .build();
        
        return AccountRequestResponse.fromEntity(requestRepository.save(accountRequest));
    }

    @Transactional(readOnly = true)
    public List<AccountRequestResponse> getUserRequests(String userId) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(AccountRequestResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AccountRequestResponse> getPendingRequests(Pageable pageable) {
        return requestRepository.findPendingRequests(pageable).map(AccountRequestResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long countPendingRequests() {
        return requestRepository.countPendingRequests();
    }

    @Transactional
    public AccountRequestResponse approveRequest(UUID requestId, String adminId, String notes) {
        log.info("Admin {} approving request {}", adminId, requestId);
        
        AccountRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        
        if (!request.isPending()) {
            throw new BusinessException("Request is not pending. Status: " + request.getStatus());
        }
        
        Customer customer = customerRepository.findByUserId(UUID.fromString(request.getUserId()))
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found for user: " + request.getUserId()));
        
        Account account = Account.builder()
            .accountNumber(generateAccountNumber())
            .customerId(customer.getId())
            .userId(request.getUserId())
            .accountType(request.getAccountType())
            .balance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .currency(request.getCurrency())
            .status(Account.AccountStatus.ACTIVE)
            .openedAt(LocalDateTime.now())
            .build();
        
        account = accountRepository.save(account);
        
        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.executeDeposit(account.getId(), request.getInitialDeposit(), null, "Initial deposit");
        }
        
        request.approve(adminId, notes, account.getId());
        return AccountRequestResponse.fromEntity(requestRepository.save(request));
    }

    @Transactional
    public AccountRequestResponse rejectRequest(UUID requestId, String adminId, String reason) {
        AccountRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        
        if (!request.isPending()) {
            throw new BusinessException("Request is not pending");
        }
        
        request.reject(adminId, reason);
        return AccountRequestResponse.fromEntity(requestRepository.save(request));
    }

    @Transactional
    public AccountRequestResponse cancelRequest(UUID requestId, String userId) {
        AccountRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        
        if (!request.getUserId().equals(userId)) {
            throw new BusinessException("You can only cancel your own requests");
        }
        
        if (!request.isPending()) {
            throw new BusinessException("Can only cancel pending requests");
        }
        
        request.cancel();
        return AccountRequestResponse.fromEntity(requestRepository.save(request));
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("10");
        for (int i = 0; i < 10; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
