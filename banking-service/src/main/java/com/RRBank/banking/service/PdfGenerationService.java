package com.RRBank.banking.service;

import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Statement;
import com.RRBank.banking.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF Generation Service
 * Generates PDF statements
 * 
 * NOTE: This is a simplified mock implementation
 * In production, use libraries like:
 * - iText (com.itextpdf:itext7-core)
 * - Apache PDFBox
 * - Flying Saucer (HTML to PDF)
 */
@Service
@Slf4j
public class PdfGenerationService {

    /**
     * Generate PDF statement
     * 
     * @param statement Statement entity
     * @param account Account entity
     * @param transactions List of transactions
     * @return PDF content as byte array
     */
    public byte[] generateStatementPdf(Statement statement, Account account, 
                                       List<Transaction> transactions) {
        try {
            log.info("Generating PDF statement for account: {}, period: {}", 
                    account.getAccountNumber(), statement.getStatementPeriod());

            // MOCK: Generate simple text-based "PDF"
            // In production, use iText or PDFBox to create actual PDF
            
            StringBuilder pdfContent = new StringBuilder();
            
            // Header
            pdfContent.append("=====================================\n");
            pdfContent.append("       RR BANK - ACCOUNT STATEMENT    \n");
            pdfContent.append("=====================================\n\n");
            
            // Account Info
            pdfContent.append("Account Number: ").append(account.getAccountNumber()).append("\n");
            pdfContent.append("Account Type: ").append(account.getAccountType()).append("\n");
            pdfContent.append("Statement Period: ").append(statement.getStatementPeriod()).append("\n");
            pdfContent.append("From: ").append(formatDate(statement.getPeriodStartDate())).append("\n");
            pdfContent.append("To: ").append(formatDate(statement.getPeriodEndDate())).append("\n\n");
            
            // Balance Summary
            pdfContent.append("===== BALANCE SUMMARY =====\n");
            pdfContent.append("Opening Balance: $").append(statement.getOpeningBalance()).append("\n");
            pdfContent.append("Total Deposits: $").append(statement.getTotalDeposits()).append("\n");
            pdfContent.append("Total Withdrawals: $").append(statement.getTotalWithdrawals()).append("\n");
            pdfContent.append("Closing Balance: $").append(statement.getClosingBalance()).append("\n\n");
            
            // Transaction Details
            pdfContent.append("===== TRANSACTION HISTORY =====\n");
            pdfContent.append(String.format("%-12s %-10s %-15s %s\n", 
                    "Date", "Type", "Amount", "Description"));
            pdfContent.append("-------------------------------------------------------\n");
            
            for (Transaction txn : transactions) {
                pdfContent.append(String.format("%-12s %-10s $%-14s %s\n",
                        formatDate(txn.getCreatedAt().toLocalDate()),
                        txn.getTransactionType(),
                        txn.getAmount(),
                        truncate(txn.getDescription() != null ? txn.getDescription() : "", 30)));
            }
            
            pdfContent.append("\n\nTotal Transactions: ").append(transactions.size()).append("\n");
            
            // Footer
            pdfContent.append("\n\n");
            pdfContent.append("=====================================\n");
            pdfContent.append("   Thank you for banking with us!   \n");
            pdfContent.append("   Questions? Contact: 1-800-RRBANK  \n");
            pdfContent.append("=====================================\n");
            
            log.info("PDF statement generated successfully");
            
            return pdfContent.toString().getBytes();
            
        } catch (Exception e) {
            log.error("Failed to generate PDF statement", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Example of production iText implementation:
     * 
     * public byte[] generateStatementPdf(...) {
     *     ByteArrayOutputStream baos = new ByteArrayOutputStream();
     *     PdfWriter writer = new PdfWriter(baos);
     *     PdfDocument pdfDoc = new PdfDocument(writer);
     *     Document document = new Document(pdfDoc, PageSize.A4);
     *     
     *     // Add header
     *     document.add(new Paragraph("RR BANK - ACCOUNT STATEMENT")
     *         .setTextAlignment(TextAlignment.CENTER)
     *         .setFontSize(20)
     *         .setBold());
     *     
     *     // Add account info table
     *     Table infoTable = new Table(2);
     *     infoTable.addCell("Account Number:");
     *     infoTable.addCell(account.getAccountNumber());
     *     document.add(infoTable);
     *     
     *     // Add transactions table
     *     Table txnTable = new Table(4);
     *     txnTable.addHeaderCell("Date");
     *     txnTable.addHeaderCell("Type");
     *     txnTable.addHeaderCell("Amount");
     *     txnTable.addHeaderCell("Description");
     *     
     *     for (Transaction txn : transactions) {
     *         txnTable.addCell(txn.getCreatedAt().toString());
     *         txnTable.addCell(txn.getTransactionType().toString());
     *         txnTable.addCell("$" + txn.getAmount().toString());
     *         txnTable.addCell(txn.getDescription());
     *     }
     *     document.add(txnTable);
     *     
     *     document.close();
     *     return baos.toByteArray();
     * }
     */

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    private String truncate(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
