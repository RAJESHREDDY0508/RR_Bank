package com.RRBank.banking.scheduler;

import com.RRBank.banking.dto.GenerateStatementRequestDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Statement Scheduler
 * Automatically generates monthly statements for all active accounts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementScheduler {

    private final StatementService statementService;
    private final AccountRepository accountRepository;

    /**
     * Generate monthly statements
     * Runs on 1st of every month at 2:00 AM
     * Cron: second, minute, hour, day-of-month, month, day-of-week
     */
    @Scheduled(cron = "0 0 2 1 * ?") // Run at 2:00 AM on 1st of every month
    public void generateMonthlyStatements() {
        log.info("Starting automated monthly statement generation");

        try {
            // Get previous month
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            LocalDate startDate = lastMonth.atDay(1);
            LocalDate endDate = lastMonth.atEndOfMonth();

            log.info("Generating statements for period: {} to {}", startDate, endDate);

            // Get all active accounts
            List<Account> activeAccounts = accountRepository.findByStatus(Account.AccountStatus.ACTIVE);
            
            log.info("Found {} active accounts for statement generation", activeAccounts.size());

            int successCount = 0;
            int failureCount = 0;

            // Generate statement for each account
            for (Account account : activeAccounts) {
                try {
                    GenerateStatementRequestDto request = GenerateStatementRequestDto.builder()
                            .accountId(account.getId())
                            .startDate(startDate)
                            .endDate(endDate)
                            .statementType("MONTHLY")
                            .includePdf(true)
                            .includeCsv(false)
                            .build();

                    statementService.generateStatement(request, null); // null = automated
                    successCount++;
                    
                    log.info("Statement generated for account: {}", account.getAccountNumber());

                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to generate statement for account: {}", account.getAccountNumber(), e);
                }

                // Add delay to avoid overwhelming the system
                Thread.sleep(1000); // 1 second delay between accounts
            }

            log.info("Monthly statement generation completed. Success: {}, Failed: {}", 
                    successCount, failureCount);

        } catch (Exception e) {
            log.error("Error in monthly statement generation scheduler", e);
        }
    }

    /**
     * Manual trigger for testing
     */
    public void triggerManualGeneration() {
        log.info("Manual statement generation triggered");
        generateMonthlyStatements();
    }
}
