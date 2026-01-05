package com.rrbank.account.migration;

import com.rrbank.account.entity.Account;
import com.rrbank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * One-time migration to update existing account numbers to new format.
 * Format: XX##-####-#### (e.g., CH12-3456-7890)
 * 
 * This runs on application startup and only updates accounts that don't
 * already have the new format.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AccountNumberMigration implements CommandLineRunner {

    private final AccountRepository accountRepository;
    
    // Pattern to match new format: XX##-####-####
    private static final Pattern NEW_FORMAT_PATTERN = Pattern.compile("^[A-Z]{2}\\d{2}-\\d{4}-\\d{4}$");

    @Override
    public void run(String... args) {
        log.info("Starting account number migration check...");
        
        List<Account> accounts = accountRepository.findAll();
        int migrated = 0;
        
        for (Account account : accounts) {
            String currentNumber = account.getAccountNumber();
            
            // Skip if already in new format
            if (currentNumber != null && NEW_FORMAT_PATTERN.matcher(currentNumber).matches()) {
                continue;
            }
            
            // Generate new account number
            String newNumber = generateAccountNumber(account.getAccountType());
            
            // Make sure it's unique
            while (accountRepository.existsByAccountNumber(newNumber)) {
                newNumber = generateAccountNumber(account.getAccountType());
            }
            
            log.info("Migrating account {}: {} -> {}", account.getId(), currentNumber, newNumber);
            account.setAccountNumber(newNumber);
            accountRepository.save(account);
            migrated++;
        }
        
        if (migrated > 0) {
            log.info("Account number migration completed. {} accounts updated.", migrated);
        } else {
            log.info("Account number migration check completed. No accounts needed updating.");
        }
    }
    
    private String generateAccountNumber(Account.AccountType accountType) {
        String prefix = switch (accountType) {
            case SAVINGS -> "SA";
            case CHECKING -> "CH";
            case CREDIT -> "CR";
            case BUSINESS -> "BU";
        };
        
        Random random = new Random();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            digits.append(random.nextInt(10));
        }
        
        String number = prefix + digits.toString();
        return number.substring(0, 4) + "-" + number.substring(4, 8) + "-" + number.substring(8, 12);
    }
}
