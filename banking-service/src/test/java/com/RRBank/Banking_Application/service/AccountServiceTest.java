package com.RRBank.banking.service;

import com.RRBank.banking.dto.AccountResponseDto;
import com.RRBank.banking.dto.BalanceResponseDto;
import com.RRBank.banking.dto.CreateAccountDto;
import com.RRBank.banking.dto.UpdateAccountStatusDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AccountService
 * Tests account creation, balance operations, and status management
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEventProducer eventProducer;

    @Mock
    private AccountCacheService cacheService;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testAccount = Account.builder()
                .id(accountId)
                .accountNumber("SAV1234567890")
                .customerId(customerId)
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .overdraftLimit(BigDecimal.ZERO)
                .interestRate(BigDecimal.valueOf(2.5))
                .openedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== CREATE ACCOUNT TESTS ==========
    @Nested
    @DisplayName("Create Account Tests")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create savings account successfully")
        void testCreateAccount_Savings_Success() {
            // Arrange
            CreateAccountDto dto = new CreateAccountDto();
            dto.setCustomerId(customerId);
            dto.setAccountType("SAVINGS");
            dto.setInitialBalance(BigDecimal.valueOf(500));
            dto.setCurrency("USD");

            when(accountNumberGenerator.generateAccountNumberWithType("SAVINGS"))
                    .thenReturn("SAV1234567890");
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> {
                        Account account = invocation.getArgument(0);
                        account.setId(UUID.randomUUID());
                        account.setCreatedAt(LocalDateTime.now());
                        return account;
                    });
            doNothing().when(cacheService).cacheBalance(any(UUID.class), any(BigDecimal.class));
            doNothing().when(eventProducer).publishAccountCreated(any());

            // Act
            AccountResponseDto result = accountService.createAccount(dto);

            // Assert
            assertNotNull(result);
            assertEquals("SAV1234567890", result.getAccountNumber());
            assertEquals("SAVINGS", result.getAccountType());
            assertEquals(BigDecimal.valueOf(500), result.getBalance());
            assertEquals("USD", result.getCurrency());
            assertEquals("ACTIVE", result.getStatus());

            verify(accountRepository).save(any(Account.class));
            verify(cacheService).cacheBalance(any(UUID.class), eq(BigDecimal.valueOf(500)));
            verify(eventProducer).publishAccountCreated(any());
        }

        @Test
        @DisplayName("Should create checking account successfully")
        void testCreateAccount_Checking_Success() {
            // Arrange
            CreateAccountDto dto = new CreateAccountDto();
            dto.setCustomerId(customerId);
            dto.setAccountType("CHECKING");
            dto.setInitialBalance(BigDecimal.ZERO);

            when(accountNumberGenerator.generateAccountNumberWithType("CHECKING"))
                    .thenReturn("CHK9876543210");
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> {
                        Account account = invocation.getArgument(0);
                        account.setId(UUID.randomUUID());
                        account.setCreatedAt(LocalDateTime.now());
                        return account;
                    });
            doNothing().when(cacheService).cacheBalance(any(UUID.class), any(BigDecimal.class));
            doNothing().when(eventProducer).publishAccountCreated(any());

            // Act
            AccountResponseDto result = accountService.createAccount(dto);

            // Assert
            assertNotNull(result);
            assertEquals("CHECKING", result.getAccountType());
            assertEquals(BigDecimal.ZERO, result.getBalance());
        }

        @Test
        @DisplayName("Should throw exception for invalid account type")
        void testCreateAccount_InvalidType_ThrowsException() {
            // Arrange
            CreateAccountDto dto = new CreateAccountDto();
            dto.setCustomerId(customerId);
            dto.setAccountType("INVALID_TYPE");
            dto.setInitialBalance(BigDecimal.valueOf(100));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> accountService.createAccount(dto));

            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    // ========== GET ACCOUNT TESTS ==========
    @Nested
    @DisplayName("Get Account Tests")
    class GetAccountTests {

        @Test
        @DisplayName("Should get account by ID successfully")
        void testGetAccountById_Success() {
            // Arrange
            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act
            AccountResponseDto result = accountService.getAccountById(accountId);

            // Assert
            assertNotNull(result);
            assertEquals(testAccount.getAccountNumber(), result.getAccountNumber());
            assertEquals(testAccount.getBalance(), result.getBalance());
            assertEquals(testAccount.getAccountType().name(), result.getAccountType());

            verify(accountRepository).findById(accountId);
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void testGetAccountById_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(accountRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> accountService.getAccountById(nonExistentId)
            );

            assertTrue(exception.getMessage().contains("Account not found"));
            verify(accountRepository).findById(nonExistentId);
        }

        @Test
        @DisplayName("Should get account by account number successfully")
        void testGetAccountByAccountNumber_Success() {
            // Arrange
            String accountNumber = "SAV1234567890";
            when(accountRepository.findByAccountNumber(accountNumber))
                    .thenReturn(Optional.of(testAccount));

            // Act
            AccountResponseDto result = accountService.getAccountByAccountNumber(accountNumber);

            // Assert
            assertNotNull(result);
            assertEquals(accountNumber, result.getAccountNumber());
            verify(accountRepository).findByAccountNumber(accountNumber);
        }

        @Test
        @DisplayName("Should get all accounts for customer")
        void testGetAccountsByCustomerId_Success() {
            // Arrange
            Account checkingAccount = Account.builder()
                    .id(UUID.randomUUID())
                    .accountNumber("CHK9876543210")
                    .customerId(customerId)
                    .accountType(Account.AccountType.CHECKING)
                    .balance(BigDecimal.valueOf(500))
                    .currency("USD")
                    .status(Account.AccountStatus.ACTIVE)
                    .build();

            when(accountRepository.findByCustomerId(customerId))
                    .thenReturn(Arrays.asList(testAccount, checkingAccount));

            // Act
            List<AccountResponseDto> results = accountService.getAccountsByCustomerId(customerId);

            // Assert
            assertNotNull(results);
            assertEquals(2, results.size());
            verify(accountRepository).findByCustomerId(customerId);
        }
    }

    // ========== BALANCE TESTS ==========
    @Nested
    @DisplayName("Balance Tests")
    class BalanceTests {

        @Test
        @DisplayName("Should get balance from cache if available")
        void testGetBalance_FromCache_Success() {
            // Arrange
            BigDecimal cachedBalance = BigDecimal.valueOf(1500);
            when(cacheService.getCachedBalance(accountId)).thenReturn(cachedBalance);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

            // Act
            BalanceResponseDto result = accountService.getAccountBalance(accountId);

            // Assert
            assertNotNull(result);
            assertEquals(cachedBalance, result.getBalance());
            assertEquals(testAccount.getAccountNumber(), result.getAccountNumber());
        }

        @Test
        @DisplayName("Should get balance from database and cache it")
        void testGetBalance_FromDatabase_Success() {
            // Arrange
            when(cacheService.getCachedBalance(accountId)).thenReturn(null);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            doNothing().when(cacheService).cacheBalance(any(UUID.class), any(BigDecimal.class));

            // Act
            BalanceResponseDto result = accountService.getAccountBalance(accountId);

            // Assert
            assertNotNull(result);
            assertEquals(testAccount.getBalance(), result.getBalance());
            verify(cacheService).cacheBalance(accountId, testAccount.getBalance());
        }

        @Test
        @DisplayName("Should calculate total balance for customer")
        void testGetTotalBalanceForCustomer_Success() {
            // Arrange
            BigDecimal totalBalance = BigDecimal.valueOf(5000);
            when(accountRepository.getTotalBalanceByCustomer(customerId))
                    .thenReturn(totalBalance);

            // Act
            BigDecimal result = accountService.getTotalBalanceForCustomer(customerId);

            // Assert
            assertEquals(totalBalance, result);
            verify(accountRepository).getTotalBalanceByCustomer(customerId);
        }

        @Test
        @DisplayName("Should return zero when no accounts exist")
        void testGetTotalBalanceForCustomer_NoAccounts_ReturnsZero() {
            // Arrange
            when(accountRepository.getTotalBalanceByCustomer(customerId))
                    .thenReturn(null);

            // Act
            BigDecimal result = accountService.getTotalBalanceForCustomer(customerId);

            // Assert
            assertEquals(BigDecimal.ZERO, result);
        }
    }

    // ========== CREDIT/DEBIT TESTS ==========
    @Nested
    @DisplayName("Credit/Debit Tests")
    class CreditDebitTests {

        @Test
        @DisplayName("Should credit account successfully")
        void testCreditAccount_Success() {
            // Arrange
            BigDecimal creditAmount = BigDecimal.valueOf(500);
            String transactionId = "TXN123";

            when(accountRepository.findByIdWithLock(accountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(cacheService).updateCachedBalance(any(), any());
            doNothing().when(eventProducer).publishBalanceUpdated(any());

            // Act
            accountService.creditAccount(accountId, creditAmount, transactionId);

            // Assert
            assertEquals(BigDecimal.valueOf(1500), testAccount.getBalance());
            verify(accountRepository).save(testAccount);
            verify(cacheService).updateCachedBalance(accountId, BigDecimal.valueOf(1500));
        }

        @Test
        @DisplayName("Should throw exception when crediting inactive account")
        void testCreditAccount_InactiveAccount_ThrowsException() {
            // Arrange
            testAccount.setStatus(Account.AccountStatus.FROZEN);
            when(accountRepository.findByIdWithLock(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> accountService.creditAccount(accountId, BigDecimal.valueOf(100), "TXN123"));

            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Should debit account successfully")
        void testDebitAccount_Success() {
            // Arrange
            BigDecimal debitAmount = BigDecimal.valueOf(300);
            String transactionId = "TXN456";

            when(accountRepository.findByIdWithLock(accountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(cacheService).updateCachedBalance(any(), any());
            doNothing().when(eventProducer).publishBalanceUpdated(any());

            // Act
            accountService.debitAccount(accountId, debitAmount, transactionId);

            // Assert
            assertEquals(BigDecimal.valueOf(700), testAccount.getBalance());
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("Should throw exception for insufficient balance")
        void testDebitAccount_InsufficientBalance_ThrowsException() {
            // Arrange
            BigDecimal debitAmount = BigDecimal.valueOf(2000); // More than balance
            when(accountRepository.findByIdWithLock(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> accountService.debitAccount(accountId, debitAmount, "TXN789"));

            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    // ========== STATUS UPDATE TESTS ==========
    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("Should freeze account successfully")
        void testUpdateAccountStatus_Freeze_Success() {
            // Arrange
            UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
            dto.setStatus("FROZEN");
            dto.setReason("Suspicious activity");

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(cacheService).invalidateAllAccountCache(any());
            doNothing().when(eventProducer).publishAccountStatusChanged(any());

            // Act
            AccountResponseDto result = accountService.updateAccountStatus(accountId, dto, "admin");

            // Assert
            assertEquals("FROZEN", result.getStatus());
            verify(accountRepository).save(testAccount);
            verify(cacheService).invalidateAllAccountCache(accountId);
        }

        @Test
        @DisplayName("Should close account and set closed date")
        void testUpdateAccountStatus_Close_Success() {
            // Arrange
            UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
            dto.setStatus("CLOSED");
            dto.setReason("Customer request");

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(cacheService).invalidateAllAccountCache(any());
            doNothing().when(eventProducer).publishAccountStatusChanged(any());

            // Act
            AccountResponseDto result = accountService.updateAccountStatus(accountId, dto, "admin");

            // Assert
            assertEquals("CLOSED", result.getStatus());
            assertNotNull(testAccount.getClosedAt());
        }

        @Test
        @DisplayName("Should throw exception for invalid status")
        void testUpdateAccountStatus_InvalidStatus_ThrowsException() {
            // Arrange
            UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
            dto.setStatus("INVALID_STATUS");

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> accountService.updateAccountStatus(accountId, dto, "admin"));
        }

        @Test
        @DisplayName("Should throw exception when changing status of closed account")
        void testUpdateAccountStatus_ClosedAccount_ThrowsException() {
            // Arrange
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            UpdateAccountStatusDto dto = new UpdateAccountStatusDto();
            dto.setStatus("ACTIVE");

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> accountService.updateAccountStatus(accountId, dto, "admin"));
        }
    }

    // ========== DELETE ACCOUNT TESTS ==========
    @Nested
    @DisplayName("Delete Account Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should delete closed account with zero balance")
        void testDeleteAccount_Success() {
            // Arrange
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            testAccount.setBalance(BigDecimal.ZERO);

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));
            doNothing().when(accountRepository).delete(testAccount);
            doNothing().when(cacheService).invalidateAllAccountCache(accountId);

            // Act
            accountService.deleteAccount(accountId);

            // Assert
            verify(accountRepository).delete(testAccount);
            verify(cacheService).invalidateAllAccountCache(accountId);
        }

        @Test
        @DisplayName("Should throw exception when deleting account with balance")
        void testDeleteAccount_WithBalance_ThrowsException() {
            // Arrange
            testAccount.setStatus(Account.AccountStatus.CLOSED);
            testAccount.setBalance(BigDecimal.valueOf(100));

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> accountService.deleteAccount(accountId));

            verify(accountRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting non-closed account")
        void testDeleteAccount_NotClosed_ThrowsException() {
            // Arrange
            testAccount.setStatus(Account.AccountStatus.ACTIVE);
            testAccount.setBalance(BigDecimal.ZERO);

            when(accountRepository.findById(accountId))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> accountService.deleteAccount(accountId));

            verify(accountRepository, never()).delete(any());
        }
    }
}
