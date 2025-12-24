package com.RRBank.banking.service;

import com.RRBank.banking.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV Generation Service
 * Generates CSV export of transactions
 */
@Service
@Slf4j
public class CsvGenerationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate CSV statement
     * 
     * @param transactions List of transactions
     * @return CSV content as byte array
     */
    public byte[] generateStatementCsv(List<Transaction> transactions) {
        try {
            log.info("Generating CSV statement with {} transactions", transactions.size());

            StringBuilder csv = new StringBuilder();
            
            // CSV Header
            csv.append("Date,Transaction ID,Type,From Account,To Account,Amount,Currency,Status,Description\n");
            
            // Transaction Rows
            for (Transaction txn : transactions) {
                csv.append(escapeCSV(formatDate(txn.getCreatedAt()))).append(",");
                csv.append(escapeCSV(txn.getId().toString())).append(",");
                csv.append(escapeCSV(txn.getTransactionType().name())).append(",");
                csv.append(escapeCSV(txn.getFromAccountId() != null ? txn.getFromAccountId().toString() : "")).append(",");
                csv.append(escapeCSV(txn.getToAccountId() != null ? txn.getToAccountId().toString() : "")).append(",");
                csv.append(txn.getAmount()).append(",");
                csv.append(escapeCSV(txn.getCurrency())).append(",");
                csv.append(escapeCSV(txn.getStatus().name())).append(",");
                csv.append(escapeCSV(txn.getDescription() != null ? txn.getDescription() : "")).append("\n");
            }
            
            log.info("CSV statement generated successfully");
            
            return csv.toString().getBytes();
            
        } catch (Exception e) {
            log.error("Failed to generate CSV statement", e);
            throw new RuntimeException("CSV generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }
}
