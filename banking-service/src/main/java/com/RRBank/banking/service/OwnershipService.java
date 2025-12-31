package com.RRBank.banking.service;

import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.CustomerRepository;
import com.RRBank.banking.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ownership Service
 * Centralized ownership and authorization checks for banking operations
 * 
 * Phase 2A.1: Account ownership enforcement
 * 
 * Rule: If admin → allow, else customer can only access their own accounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnershipService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    /**
     * Verify that the authenticated user owns the account or is an admin
     * 
     * @param accountId The account ID to check
     * @param auth Spring Security Authentication
     * @return The Account entity if authorized
     * @throws AccessDeniedException if user doesn't own the account and isn't admin
     * @throws ResourceNotFoundException if account doesn't exist
     */
    @Transactional(readOnly = true)
    public Account requireAccountOwnership(UUID accountId, Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }

        // Admin can access any account
        if (SecurityUtil.isAdmin(auth)) {
            return accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        }

        // Get account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        // Get authenticated user ID as string
        String authUserId = SecurityUtil.requireUserIdString(auth);

        // Check if user owns the account directly (account.userId is String)
        if (account.getUserId() != null && account.getUserId().equals(authUserId)) {
            return account;
        }

        // Alternative: check via customer linkage
        if (account.getCustomerId() != null) {
            Customer customer = customerRepository.findById(account.getCustomerId()).orElse(null);
            if (customer != null && customer.getUserId() != null) {
                // Customer.userId is UUID, convert to string for comparison
                if (customer.getUserId().toString().equals(authUserId)) {
                    return account;
                }
            }
        }

        log.warn("Access denied: User {} attempted to access account {} owned by user {}", 
                authUserId, accountId, account.getUserId());
        throw new AccessDeniedException("You do not have permission to access this account");
    }

    /**
     * Verify that the authenticated user owns the account (by account number) or is an admin
     */
    @Transactional(readOnly = true)
    public Account requireAccountOwnershipByNumber(String accountNumber, Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        // Admin can access any account
        if (SecurityUtil.isAdmin(auth)) {
            return account;
        }

        // Get authenticated user ID
        String authUserId = SecurityUtil.requireUserIdString(auth);

        // Check ownership
        if (account.getUserId() != null && account.getUserId().equals(authUserId)) {
            return account;
        }

        // Check via customer
        if (account.getCustomerId() != null) {
            Customer customer = customerRepository.findById(account.getCustomerId()).orElse(null);
            if (customer != null && customer.getUserId() != null) {
                if (customer.getUserId().toString().equals(authUserId)) {
                    return account;
                }
            }
        }

        log.warn("Access denied: User {} attempted to access account {}", authUserId, accountNumber);
        throw new AccessDeniedException("You do not have permission to access this account");
    }

    /**
     * Verify that the authenticated user owns the customer profile or is an admin
     */
    @Transactional(readOnly = true)
    public Customer requireCustomerOwnership(UUID customerId, Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        // Admin can access any customer
        if (SecurityUtil.isAdmin(auth)) {
            return customer;
        }

        // Get authenticated user ID
        String authUserId = SecurityUtil.requireUserIdString(auth);

        // Check if user owns the customer profile
        if (customer.getUserId() != null && customer.getUserId().toString().equals(authUserId)) {
            return customer;
        }

        log.warn("Access denied: User {} attempted to access customer {} owned by user {}", 
                authUserId, customerId, customer.getUserId());
        throw new AccessDeniedException("You do not have permission to access this customer profile");
    }

    /**
     * Check if authenticated user is an admin (non-throwing version)
     */
    public boolean isAdmin(Authentication auth) {
        return SecurityUtil.isAdmin(auth);
    }

    /**
     * Get the authenticated user's customer profile
     */
    @Transactional(readOnly = true)
    public Customer getAuthenticatedCustomer(Authentication auth) {
        String userId = SecurityUtil.requireUserIdString(auth);
        try {
            UUID userIdUUID = UUID.fromString(userId);
            return customerRepository.findByUserId(userIdUUID).orElse(null);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", userId);
            return null;
        }
    }

    /**
     * Check if the authenticated user can perform operations on the given account
     * Returns false instead of throwing an exception
     */
    @Transactional(readOnly = true)
    public boolean canAccessAccount(UUID accountId, Authentication auth) {
        if (auth == null) {
            return false;
        }

        if (SecurityUtil.isAdmin(auth)) {
            return true;
        }

        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return false;
            }

            String authUserId = SecurityUtil.requireUserIdString(auth);

            // Direct ownership
            if (account.getUserId() != null && account.getUserId().equals(authUserId)) {
                return true;
            }

            // Via customer
            if (account.getCustomerId() != null) {
                Customer customer = customerRepository.findById(account.getCustomerId()).orElse(null);
                if (customer != null && customer.getUserId() != null) {
                    if (customer.getUserId().toString().equals(authUserId)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Error checking account access: {}", e.getMessage());
            return false;
        }
    }
}
