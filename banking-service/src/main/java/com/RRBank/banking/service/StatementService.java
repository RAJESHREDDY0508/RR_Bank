package com.RRBank.banking.service;

import com.RRBank.banking.dto.GenerateStatementRequestDto;
import com.RRBank.banking.dto.StatementResponseDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Statement;
import com.RRBank.banking.entity.Transaction;
import com.RRBank.banking.event.StatementGeneratedEvent;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.StatementRepository;
import com.RRBank.banking.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Statement Service
 * Generates and manages account statements
 * 
 * Phase 2C.3: Statement generation is SYNCHRONOUS (no @Async)
 * - generatedBy is correctly extracted from authentication in controller
 * - No random UUID fallback
 */
@Service
@Slf4j
public class StatementService {

    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PdfGenerationService pdfGenerationService;
    private final CsvGenerationService csvGenerationService;
    private final S3Service s3Service;
    
    @Autowired(required = false)
    private StatementEventProducer eventProducer;

    @Autowired
    public StatementService(StatementRepository statementRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           PdfGenerationService pdfGenerationService,
                           CsvGenerationService csvGenerationService,
                           S3Service s3Service) {
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.csvGenerationService = csvGenerationService;
        this.s3Service = s3Service;
    }

    /**
     * Generate statement for account
     * 
     * Phase 2C.3: This method is SYNCHRONOUS (no @Async annotation)
     * - Statement generation returns immediately with the result
     * - generatedBy must be a valid user ID (no random UUID fallback)
     */
    @Transactional
    public StatementResponseDto generateStatement(GenerateStatementRequestDto request, UUID generatedBy) {
        log.info("Generating statement for account: {}, period: {} to {}, generatedBy: {}", 
                request.getAccountId(), request.getStartDate(), request.getEndDate(), generatedBy);

        // Validate generatedBy is provided (Phase 2C.3 - no fallback)
        if (generatedBy == null) {
            throw new IllegalArgumentException("generatedBy user ID is required for statement generation");
        }

        // Validate account
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        // Calculate statement period
        String period = calculatePeriod(request.getStartDate(), request.getEndDate());

        // Check if statement already exists
        if (statementRepository.existsByAccountIdAndStatementPeriod(account.getId(), period)) {
            log.warn("Statement already exists for account {} period {}", account.getId(), period);
            return StatementResponseDto.fromEntity(
                    statementRepository.findByAccountIdAndStatementPeriod(account.getId(), period).get());
        }

        // Create statement record
        Statement statement = Statement.builder()
                .accountId(account.getId())
                .customerId(account.getCustomerId())
                .statementPeriod(period)
                .periodStartDate(request.getStartDate())
                .periodEndDate(request.getEndDate())
                .statementType(request.getStatementType() != null ? 
                        Statement.StatementType.valueOf(request.getStatementType()) : 
                        Statement.StatementType.ON_DEMAND)
                .status(Statement.StatementStatus.GENERATING)
                .generatedBy(generatedBy)
                .build();

        statement = statementRepository.save(statement);

        try {
            // Get transactions for period (Phase 2C.1 ensures payments are included)
            List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                    account.getId(), request.getStartDate().atStartOfDay(), request.getEndDate().atTime(23, 59, 59));

            // Calculate balances
            BigDecimal openingBalance = calculateOpeningBalance(account, request.getStartDate(), transactions);
            BigDecimal closingBalance = account.getBalance();
            BigDecimal totalDeposits = calculateTotalDeposits(transactions);
            BigDecimal totalWithdrawals = calculateTotalWithdrawals(transactions);
            BigDecimal totalPayments = calculateTotalPayments(transactions);

            // Update statement with calculations
            statement.setOpeningBalance(openingBalance);
            statement.setClosingBalance(closingBalance);
            statement.setTotalDeposits(totalDeposits);
            statement.setTotalWithdrawals(totalWithdrawals);
            statement.setTotalTransactions(transactions.size());

            // Generate PDF if requested
            String pdfPath = null;
            long pdfSize = 0;
            if (request.getIncludePdf() == null || request.getIncludePdf()) {
                byte[] pdfContent = pdfGenerationService.generateStatementPdf(statement, account, transactions);
                String pdfFileName = String.format("statements/%s/%s_statement.pdf", 
                        account.getAccountNumber(), period);
                pdfPath = s3Service.uploadFile(pdfContent, pdfFileName, "application/pdf");
                pdfSize = pdfContent.length;
            }

            // Generate CSV if requested
            String csvPath = null;
            long csvSize = 0;
            if (request.getIncludeCsv() != null && request.getIncludeCsv()) {
                byte[] csvContent = csvGenerationService.generateStatementCsv(transactions);
                String csvFileName = String.format("statements/%s/%s_transactions.csv", 
                        account.getAccountNumber(), period);
                csvPath = s3Service.uploadFile(csvContent, csvFileName, "text/csv");
                csvSize = csvContent.length;
            }

            // Mark as generated
            statement.markAsGenerated(pdfPath, csvPath, pdfSize, csvSize, s3Service.getBucketName());
            statement = statementRepository.save(statement);

            log.info("Statement generated successfully: {} for account {} by user {}", 
                    statement.getId(), account.getId(), generatedBy);

            // Publish event
            publishStatementGeneratedEvent(statement);

            return StatementResponseDto.fromEntity(statement);

        } catch (Exception e) {
            log.error("Failed to generate statement for account: {}", account.getId(), e);
            statement.markAsFailed(e.getMessage());
            statementRepository.save(statement);
            throw new RuntimeException("Statement generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get all statements for account
     */
    @Transactional(readOnly = true)
    public List<StatementResponseDto> getAccountStatements(UUID accountId) {
        log.info("Fetching statements for accountId: {}", accountId);
        
        return statementRepository.findByAccountIdOrderByPeriodEndDateDesc(accountId).stream()
                .map(StatementResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get statement by ID
     */
    @Transactional(readOnly = true)
    public StatementResponseDto getStatementById(UUID statementId) {
        log.info("Fetching statement: {}", statementId);
        
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found: " + statementId));
        
        return StatementResponseDto.fromEntity(statement);
    }

    /**
     * Download statement PDF
     */
    @Transactional
    public byte[] downloadStatementPdf(UUID statementId) {
        log.info("Downloading statement PDF: {}", statementId);
        
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found: " + statementId));
        
        if (!statement.isAvailableForDownload()) {
            throw new IllegalStateException("Statement is not available for download");
        }
        
        // Increment download count
        statement.incrementDownloadCount();
        statementRepository.save(statement);
        
        // Download from S3
        return s3Service.downloadFile(statement.getPdfFilePath());
    }

    /**
     * Download statement CSV
     */
    @Transactional
    public byte[] downloadStatementCsv(UUID statementId) {
        log.info("Downloading statement CSV: {}", statementId);
        
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found: " + statementId));
        
        if (statement.getCsvFilePath() == null) {
            throw new IllegalStateException("CSV not available for this statement");
        }
        
        statement.incrementDownloadCount();
        statementRepository.save(statement);
        
        return s3Service.downloadFile(statement.getCsvFilePath());
    }

    // ========== HELPER METHODS ==========

    private String calculatePeriod(LocalDate startDate, LocalDate endDate) {
        // For monthly statements: YYYY-MM format
        YearMonth yearMonth = YearMonth.from(startDate);
        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private BigDecimal calculateOpeningBalance(Account account, LocalDate startDate, List<Transaction> transactions) {
        // Calculate opening balance by subtracting net transactions from current balance
        BigDecimal netChange = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getTransactionType() == Transaction.TransactionType.DEPOSIT ||
                t.getTransactionType() == Transaction.TransactionType.TRANSFER) {
                if (t.getToAccountId() != null && t.getToAccountId().equals(account.getId())) {
                    netChange = netChange.add(t.getAmount());
                }
            }
            if (t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL ||
                t.getTransactionType() == Transaction.TransactionType.PAYMENT ||
                t.getTransactionType() == Transaction.TransactionType.TRANSFER) {
                if (t.getFromAccountId() != null && t.getFromAccountId().equals(account.getId())) {
                    netChange = netChange.subtract(t.getAmount());
                }
            }
        }
        return account.getBalance().subtract(netChange);
    }

    private BigDecimal calculateTotalDeposits(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalWithdrawals(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalPayments(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.PAYMENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void publishStatementGeneratedEvent(Statement statement) {
        if (eventProducer == null) {
            log.debug("Kafka is disabled, skipping StatementGeneratedEvent");
            return;
        }
        StatementGeneratedEvent event = StatementGeneratedEvent.builder()
                .statementId(statement.getId())
                .accountId(statement.getAccountId())
                .customerId(statement.getCustomerId())
                .statementPeriod(statement.getStatementPeriod())
                .periodStartDate(statement.getPeriodStartDate())
                .periodEndDate(statement.getPeriodEndDate())
                .statementType(statement.getStatementType().name())
                .openingBalance(statement.getOpeningBalance())
                .closingBalance(statement.getClosingBalance())
                .totalTransactions(statement.getTotalTransactions())
                .pdfFilePath(statement.getPdfFilePath())
                .csvFilePath(statement.getCsvFilePath())
                .s3Bucket(statement.getS3Bucket())
                .generatedAt(statement.getGeneratedAt())
                .build();
        
        eventProducer.publishStatementGenerated(event);
    }
}
