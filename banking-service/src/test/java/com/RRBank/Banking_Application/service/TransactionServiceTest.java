package com.RRBank.banking.service;

import com.RRBank.banking.dto.TransactionResponseDto;
import com.RRBank.banking.dto.TransferRequestDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Transaction;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.TransactionRepository;
import com.RRBank.banking.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TransactionService
 * Tests money transfer operations with edge cases
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventProducer eventProducer;

    @Mock
    private AccountCacheService cacheService;

    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private UUID userId;
    private TransferRequestDto transferRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC001")
                .customerId(userId)
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .overdraftLimit(BigDecimal.ZERO)
                .build();

        toAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC002")
                .customerId(UUID.randomUUID())
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(500))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .overdraftLimit(BigDecimal.ZERO)
                .build();

        transferRequest = new TransferRequestDto();
        transferRequest.setFromAccountId(fromAccount.getId());
        transferRequest.setToAccountId(toAccount.getId());
        transferRequest.setAmount(BigDecimal.valueOf(200));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    void testTransfer_Success() {
        // Arrange
        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionCompleted(any());
        doNothing().when(cacheService).updateCachedBalance(any(), any());

        // Act
        TransactionResponseDto result = transactionService.transfer(transferRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(800), fromAccount.getBalance());
        assertEquals(BigDecimal.valueOf(700), toAccount.getBalance());

        verify(accountRepository, times(2)).findByIdWithLock(any(UUID.class));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void testTransfer_InsufficientFunds_ThrowsException() {
        // Arrange
        transferRequest.setAmount(BigDecimal.valueOf(2000)); // More than balance

        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionFailed(any());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_SourceAccountNotFound_ThrowsException() {
        // Arrange
        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.empty());
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionFailed(any());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_DestinationAccountNotFound_ThrowsException() {
        // Arrange
        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.empty());
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionFailed(any());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_InactiveSourceAccount_ThrowsException() {
        // Arrange
        fromAccount.setStatus(Account.AccountStatus.FROZEN);

        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionFailed(any());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_InactiveDestinationAccount_ThrowsException() {
        // Arrange
        toAccount.setStatus(Account.AccountStatus.CLOSED);

        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionFailed(any());

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_SameAccount_ThrowsException() {
        // Arrange
        transferRequest.setToAccountId(fromAccount.getId());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(transferRequest, userId));
    }

    @Test
    void testTransfer_ExactBalance_Success() {
        // Arrange
        transferRequest.setAmount(BigDecimal.valueOf(1000)); // Exact balance

        when(referenceGenerator.generateTransactionReferenceWithType(anyString()))
                .thenReturn("TXN-TEST-001");
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(UUID.randomUUID());
                    }
                    return t;
                });
        when(accountRepository.findByIdWithLock(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventProducer).publishTransactionInitiated(any());
        doNothing().when(eventProducer).publishTransactionCompleted(any());
        doNothing().when(cacheService).updateCachedBalance(any(), any());

        // Act
        TransactionResponseDto result = transactionService.transfer(transferRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, fromAccount.getBalance());
        assertEquals(BigDecimal.valueOf(1500), toAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void testGetTransactionById_Success() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .transactionReference("TXN001")
                .amount(BigDecimal.valueOf(100))
                .status(Transaction.TransactionStatus.COMPLETED)
                .transactionType(Transaction.TransactionType.TRANSFER)
                .currency("USD")
                .build();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        // Act
        TransactionResponseDto result = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(result);

        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void testGetTransactionById_NotFound_ThrowsException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionById(transactionId));

        verify(transactionRepository).findById(transactionId);
    }
}
