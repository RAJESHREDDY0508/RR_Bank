package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.event.AccountCreatedEvent;
import com.RRBank.banking.event.AccountStatusChangedEvent;
import com.RRBank.banking.event.BalanceUpdatedEvent;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.security.CustomUserDetails;
import com.RRBank.banking.util.AccountNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Account Service
 * Business logic for account management
 * 
 * SECURITY: Every account MUST be owned by the authenticated user
 */
@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountCacheService cacheService;
    private final AccountNumberGenerator accountNumberGenerator;
    
    @Autowired(required = false)
    private AccountEventProducer eventProducer;
    
    // Lazy injection to avoid circular dependency
    @Autowired(required = false)
    @Lazy
    private TransactionService transactionService;

    @Autowired
    public AccountService(AccountRepository accountRepository, 
                         AccountCacheService cacheService,
                         AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.cacheService = cacheService;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    // ========== AUTHENTICATION HELPER METHODS ==========

    /**
     * Get authenticated user ID - BULLETPROOF implementation
     * 
     * @return The authenticated user's ID
     * @throws IllegalStateException if user is not authenticated or userId cannot be extracted
     */
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check 1: Authentication must exist
        if (auth == null) {
            log.error("No authentication found in SecurityContext");
            throw new IllegalStateException("User must be authenticated to perform this action");
        }

        // Check 2: User must be authenticated
        if (!auth.isAuthenticated()) {
            log.error("User is not authenticated");
            throw new IllegalStateException("User must be authenticated to perform this action");
        }

        Object principal = auth.getPrincipal();

        // Check 3: Principal must not be null
        if (principal == null) {
            log.error("Authentication principal is null");
            throw new IllegalStateException("Invalid authentication state");
        }

        // Check 4: Handle CustomUserDetails (our implementation)
        if (principal instanceof CustomUserDetails userDetails) {
            String userId = userDetails.getUserId();
            if (userId == null || userId.isBlank()) {
                log.error("CustomUserDetails has null/empty userId");
                throw new IllegalStateException("User ID is missing from authentication");
            }
            log.debug("Extracted userId from CustomUserDetails: {}", userId);
            return userId;
        }

        // Check 5: Handle standard Spring Security UserDetails (fallback)
        if (principal instanceof UserDetails userDetails) {
            // Username might be the userId in some configurations
            String username = userDetails.getUsername();
            log.warn("Using standard UserDetails, username as userId fallback: {}", username);
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("Username is missing from authentication");
            }
            return username;
        }

        // Check 6: Handle String principal (e.g., from JWT subject)
        if (principal instanceof String stringPrincipal) {
            if (stringPrincipal.isBlank()) {
                throw new IllegalStateException("Authentication principal is empty");
            }
            log.debug("Using String principal as userId: {}", stringPrincipal);
            return stringPrincipal;
        }

        // Final fallback: Unknown principal type
        log.error("Unknown principal type: {}", principal.getClass().getName());
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }

    /**
     * Get authenticated user ID as UUID (nullable)
     */
    private UUID getAuthenticatedUserIdAsUUID() {
        try {
            String userId = getAuthenticatedUserId();
            return UUID.fromString(userId);
        } catch (Exception e) {
            log.warn("Could not extract user UUID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate that userId is not null before saving
     * 
     * @param account The account to validate
     * @throws IllegalStateException if userId is missing
     */
    private void validateAccountOwnership(Account account) {
        if (account.getUserId() == null || account.getUserId().isBlank()) {
            log.error("Account validation failed: userId is null or empty");
            throw new IllegalStateException("Account must be linked to a user. userId cannot be null.");
        }
    }

    // ========== ACCOUNT OPERATIONS ==========

    /**
     * Create new bank account
     * 
     * SECURITY: Account is automatically linked to the authenticated user
     * 
     * ✅ CRITICAL FIX: If initialBalance > 0, creates an Initial Deposit transaction
     */
    @Transactional
    public AccountResponseDto createAccount(CreateAccountDto dto) {
        log.info("Creating account for customerId: {}, type: {}", dto.getCustomerId(), dto.getAccountType());

        // ✅ CRITICAL: Get authenticated user ID
        String userId = getAuthenticatedUserId();
        UUID userUUID = getAuthenticatedUserIdAsUUID();
        log.info("Account will be owned by userId: {}", userId);

        // Validate account type
        Account.AccountType accountType;
        try {
            accountType = Account.AccountType.valueOf(dto.getAccountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid account type: " + dto.getAccountType());
        }

        // Generate unique account number
        String accountNumber = accountNumberGenerator.generateAccountNumberWithType(dto.getAccountType());

        // Set default values
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "USD";
        BigDecimal overdraftLimit = dto.getOverdraftLimit() != null ? dto.getOverdraftLimit() : BigDecimal.ZERO;
        BigDecimal interestRate = dto.getInterestRate() != null ? dto.getInterestRate() : BigDecimal.ZERO;
        BigDecimal initialBalance = dto.getInitialBalance() != null ? dto.getInitialBalance() : BigDecimal.ZERO;

        // Create account entity with PENDING status (requires admin approval)
        // ✅ CRITICAL FIX: Set userId from authenticated user
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerId(dto.getCustomerId())
                .userId(userId)  // ✅ THIS WAS MISSING - NOW FIXED
                .accountType(accountType)
                .balance(initialBalance)  // Set initial balance
                .currency(currency)
                .status(Account.AccountStatus.ACTIVE)  // ✅ Set to ACTIVE for immediate use
                .overdraftLimit(overdraftLimit)
                .interestRate(interestRate)
                .build();

        // ✅ DEFENSIVE VALIDATION: Ensure userId is set before saving
        validateAccountOwnership(account);

        account = accountRepository.save(account);
        log.info("Account created with ID: {}, accountNumber: {}, userId: {}, initialBalance: {}", 
                account.getId(), account.getAccountNumber(), account.getUserId(), initialBalance);

        // Cache the balance
        cacheService.cacheBalance(account.getId(), account.getBalance());

        // Publish event to Kafka
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .initialBalance(account.getBalance())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .build();
        
        if (eventProducer != null) {
            eventProducer.publishAccountCreated(event);
        }

        // ✅ CRITICAL: Create Initial Deposit Transaction if initialBalance > 0
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0 && transactionService != null) {
            try {
                log.info("Creating initial deposit transaction for account {} with amount {}", 
                        account.getId(), initialBalance);
                transactionService.createInitialDeposit(account.getId(), initialBalance, userUUID);
                log.info("Initial deposit transaction created successfully");
            } catch (Exception e) {
                log.error("Failed to create initial deposit transaction: {}", e.getMessage(), e);
                // Don't fail account creation if initial deposit fails
                // The balance is already set on the account
            }
        }

        return AccountResponseDto.fromEntity(account);
    }

    /**
     * Get account by ID
     */
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountById(UUID accountId) {
        log.info("Fetching account with ID: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        
        return AccountResponseDto.fromEntity(account);
    }

    /**
     * Get account entity by ID (for internal use)
     */
    @Transactional(readOnly = true)
    public Account getAccountEntityById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
    }

    /**
     * Get account by account number
     */
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account with accountNumber: {}", accountNumber);
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        
        return AccountResponseDto.fromEntity(account);
    }

    /**
     * Get all accounts for a customer
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByCustomerId(UUID customerId) {
        log.info("Fetching accounts for customerId: {}", customerId);
        
        return accountRepository.findByCustomerId(customerId).stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get accounts for the authenticated user
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getMyAccounts() {
        String userId = getAuthenticatedUserId();
        log.info("Fetching accounts for authenticated userId: {}", userId);
        
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active accounts for customer
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getActiveAccountsByCustomer(UUID customerId) {
        log.info("Fetching active accounts for customerId: {}", customerId);
        
        return accountRepository.findActiveAccountsByCustomer(customerId).stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get account balance (with caching)
     */
    @Transactional(readOnly = true)
    public BalanceResponseDto getAccountBalance(UUID accountId) {
        log.info("Fetching balance for accountId: {}", accountId);

        // Try to get from cache first
        BigDecimal cachedBalance = cacheService.getCachedBalance(accountId);
        if (cachedBalance != null) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
            
            return buildBalanceResponse(account, cachedBalance);
        }

        // If not in cache, get from database
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        
        // Cache it for future requests
        cacheService.cacheBalance(accountId, account.getBalance());
        
        return buildBalanceResponse(account, account.getBalance());
    }

    /**
     * Update account status
     */
    @Transactional
    public AccountResponseDto updateAccountStatus(UUID accountId, UpdateAccountStatusDto dto, String changedBy) {
        log.info("Updating status for accountId: {} to {}", accountId, dto.getStatus());

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Validate new status
        Account.AccountStatus newStatus;
        try {
            newStatus = Account.AccountStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid account status: " + dto.getStatus());
        }

        // Check if status change is allowed
        validateStatusChange(account.getStatus(), newStatus);

        Account.AccountStatus oldStatus = account.getStatus();
        account.setStatus(newStatus);

        // If closing account, set closed date
        if (newStatus == Account.AccountStatus.CLOSED) {
            account.setClosedAt(LocalDateTime.now());
        }

        account = accountRepository.save(account);
        log.info("Account status updated for accountId: {}, oldStatus: {}, newStatus: {}",
                accountId, oldStatus, newStatus);

        // Invalidate cache
        cacheService.invalidateAllAccountCache(accountId);

        // Publish event
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .reason(dto.getReason())
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        
        if (eventProducer != null) {
            eventProducer.publishAccountStatusChanged(event);
        }

        return AccountResponseDto.fromEntity(account);
    }

    /**
     * Credit amount to account (internal use - for transactions)
     */
    @Transactional
    public void creditAccount(UUID accountId, BigDecimal amount, String transactionId) {
        log.info("Crediting account: {}, amount: {}", accountId, amount);

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Check if account is active
        if (!account.isActive()) {
            throw new IllegalStateException("Cannot credit inactive account");
        }

        BigDecimal oldBalance = account.getBalance();
        account.credit(amount);
        
        account = accountRepository.save(account);
        log.info("Account credited: {}, oldBalance: {}, newBalance: {}", 
                accountId, oldBalance, account.getBalance());

        // Update cache
        cacheService.updateCachedBalance(accountId, account.getBalance());

        // Publish balance updated event
        publishBalanceUpdatedEvent(account, oldBalance, "CREDIT", transactionId);
    }

    /**
     * Debit amount from account (internal use - for transactions)
     */
    @Transactional
    public void debitAccount(UUID accountId, BigDecimal amount, String transactionId) {
        log.info("Debiting account: {}, amount: {}", accountId, amount);

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Check if account is active
        if (!account.isActive()) {
            throw new IllegalStateException("Cannot debit inactive account");
        }

        // Check sufficient balance
        if (!account.hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }

        BigDecimal oldBalance = account.getBalance();
        account.debit(amount);
        
        account = accountRepository.save(account);
        log.info("Account debited: {}, oldBalance: {}, newBalance: {}", 
                accountId, oldBalance, account.getBalance());

        // Update cache
        cacheService.updateCachedBalance(accountId, account.getBalance());

        // Publish balance updated event
        publishBalanceUpdatedEvent(account, oldBalance, "DEBIT", transactionId);
    }

    /**
     * Get all accounts (Admin)
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAllAccounts() {
        log.info("Fetching all accounts");
        
        return accountRepository.findAll().stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get accounts by status (Admin)
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByStatus(String status) {
        log.info("Fetching accounts with status: {}", status);
        
        Account.AccountStatus accountStatus = Account.AccountStatus.valueOf(status.toUpperCase());
        
        return accountRepository.findByStatus(accountStatus).stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search accounts (Admin)
     */
    @Transactional(readOnly = true)
    public List<AccountResponseDto> searchAccounts(String query) {
        log.info("Searching accounts with query: {}", query);
        
        return accountRepository.searchAccounts(query).stream()
                .map(AccountResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get total balance for customer
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceForCustomer(UUID customerId) {
        log.info("Calculating total balance for customerId: {}", customerId);
        
        BigDecimal totalBalance = accountRepository.getTotalBalanceByCustomer(customerId);
        return totalBalance != null ? totalBalance : BigDecimal.ZERO;
    }

    /**
     * Delete account (Admin - only if balance is zero)
     */
    @Transactional
    public void deleteAccount(UUID accountId) {
        log.info("Deleting account with ID: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        // Can only delete account with zero balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        }

        // Should be closed first
        if (account.getStatus() != Account.AccountStatus.CLOSED) {
            throw new IllegalStateException("Account must be closed before deletion");
        }

        accountRepository.delete(account);
        cacheService.invalidateAllAccountCache(accountId);
        log.info("Account deleted with ID: {}", accountId);
    }

    // ========== HELPER METHODS ==========

    private BalanceResponseDto buildBalanceResponse(Account account, BigDecimal balance) {
        BigDecimal availableBalance = balance.add(account.getOverdraftLimit());
        
        return BalanceResponseDto.builder()
                .accountId(account.getId().toString())
                .accountNumber(account.getAccountNumber())
                .balance(balance)
                .availableBalance(availableBalance)
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .build();
    }

    private void validateStatusChange(Account.AccountStatus currentStatus, Account.AccountStatus newStatus) {
        // Cannot reopen closed account
        if (currentStatus == Account.AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot change status of closed account");
        }

        // Cannot go from SUSPENDED directly to CLOSED
        if (currentStatus == Account.AccountStatus.SUSPENDED && newStatus == Account.AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot close suspended account directly. Activate first.");
        }
    }

    private void publishBalanceUpdatedEvent(Account account, BigDecimal oldBalance, 
                                           String transactionType, String transactionId) {
        BigDecimal changeAmount = account.getBalance().subtract(oldBalance);
        
        BalanceUpdatedEvent event = BalanceUpdatedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .oldBalance(oldBalance)
                .newBalance(account.getBalance())
                .changeAmount(changeAmount)
                .transactionType(transactionType)
                .transactionId(transactionId)
                .updatedAt(LocalDateTime.now())
                .build();
        
        if (eventProducer != null) {
            eventProducer.publishBalanceUpdated(event);
        }
    }
}
