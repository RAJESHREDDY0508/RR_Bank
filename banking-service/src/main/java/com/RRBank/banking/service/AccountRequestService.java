package com.RRBank.banking.service;

import com.RRBank.banking.dto.AccountOpenRequest;
import com.RRBank.banking.dto.AccountRequestResponse;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.AccountRequest;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.exception.BusinessException;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.AccountRequestRepository;
import com.RRBank.banking.repository.CustomerRepository;
import com.RRBank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final UserRepository userRepository;
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
        
        // Get or create customer for this user
        Customer customer = getOrCreateCustomer(request.getUserId());
        
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
        log.info("Account created: {} for user {}", account.getAccountNumber(), request.getUserId());
        
        // Process initial deposit if any
        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            try {
                ledgerService.executeDeposit(account.getId(), request.getInitialDeposit(), null, "Initial deposit");
                log.info("Initial deposit of {} processed for account {}", request.getInitialDeposit(), account.getAccountNumber());
            } catch (Exception e) {
                log.warn("Failed to process initial deposit: {}. Account created without deposit.", e.getMessage());
                // Don't fail the approval - just log the warning
                // The account is still created, user can deposit later
            }
        }
        
        request.approve(adminId, notes, account.getId());
        AccountRequest savedRequest = requestRepository.save(request);
        
        log.info("Account request {} approved. Account {} created.", requestId, account.getAccountNumber());
        
        return AccountRequestResponse.fromEntity(savedRequest);
    }

    /**
     * Get existing customer or create a new one for the user
     */
    private Customer getOrCreateCustomer(String userId) {
        log.info("Looking up customer for userId: {}", userId);
        
        // Try to find existing customer
        Optional<Customer> existingCustomer = customerRepository.findByUserId(UUID.fromString(userId));
        
        if (existingCustomer.isPresent()) {
            log.info("Found existing customer: {}", existingCustomer.get().getId());
            return existingCustomer.get();
        }
        
        // Customer doesn't exist - create one from User data
        log.info("Customer not found for user {}. Creating new customer record.", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        Customer newCustomer = Customer.builder()
            .userId(UUID.fromString(userId))
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhoneNumber())
            .address(user.getAddress())
            .city(user.getCity())
            .state(user.getState())
            .zipCode(user.getPostalCode())
            .country(user.getCountry())
            .kycStatus(Customer.KycStatus.PENDING)
            .customerSegment(Customer.CustomerSegment.REGULAR)
            .build();
        
        newCustomer = customerRepository.save(newCustomer);
        log.info("Created new customer: {} for user {}", newCustomer.getId(), userId);
        
        return newCustomer;
    }

    @Transactional
    public AccountRequestResponse rejectRequest(UUID requestId, String adminId, String reason) {
        log.info("Admin {} rejecting request {} with reason: {}", adminId, requestId, reason);
        
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
        log.info("User {} cancelling request {}", userId, requestId);
        
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
