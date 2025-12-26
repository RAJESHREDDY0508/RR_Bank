package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statement Entity - Account statements
 * 
 * This entity maps to the 'statements' table in the database.
 * Each statement represents a periodic summary of account activity.
 */
@Entity
@Table(name = "statements", indexes = {
    @Index(name = "idx_statements_account", columnList = "account_id"),
    @Index(name = "idx_statements_customer", columnList = "customer_id"),
    @Index(name = "idx_statements_period", columnList = "statement_period"),
    @Index(name = "idx_statements_date", columnList = "statement_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "statement_period", nullable = false, length = 7)
    private String statementPeriod; // Format: YYYY-MM

    @Column(name = "statement_date")
    private LocalDate statementDate;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    @Builder.Default
    private StatementType statementType = StatementType.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatementStatus status = StatementStatus.PENDING;

    @Column(name = "opening_balance", precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance;

    @Column(name = "total_credits", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "total_debits", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "total_deposits", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Column(name = "total_withdrawals", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "total_transactions")
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    @Column(name = "csv_file_path", length = 500)
    private String csvFilePath;

    @Column(name = "pdf_file_size")
    private Long pdfFileSize;

    @Column(name = "csv_file_size")
    private Long csvFileSize;

    @Column(name = "s3_bucket", length = 255)
    private String s3Bucket;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "generated_by")
    private UUID generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        if (statementDate == null && periodEndDate != null) {
            statementDate = periodEndDate;
        }
        if (totalCredits == null) {
            totalCredits = BigDecimal.ZERO;
        }
        if (totalDebits == null) {
            totalDebits = BigDecimal.ZERO;
        }
        if (totalDeposits == null) {
            totalDeposits = BigDecimal.ZERO;
        }
        if (totalWithdrawals == null) {
            totalWithdrawals = BigDecimal.ZERO;
        }
        if (transactionCount == null) {
            transactionCount = 0;
        }
        if (totalTransactions == null) {
            totalTransactions = 0;
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
        if (statementType == null) {
            statementType = StatementType.MONTHLY;
        }
        if (status == null) {
            status = StatementStatus.PENDING;
        }
    }

    /**
     * Check if statement file is available for download
     */
    public boolean isAvailable() {
        return status == StatementStatus.GENERATED && 
               ((filePath != null && !filePath.isEmpty()) || 
                (pdfFilePath != null && !pdfFilePath.isEmpty()));
    }

    /**
     * Check if available for download (alias)
     */
    public boolean isAvailableForDownload() {
        return isAvailable();
    }

    /**
     * Mark statement as generated (single file)
     */
    public void markAsGenerated(String path) {
        this.filePath = path;
        this.pdfFilePath = path;
        this.generatedAt = LocalDateTime.now();
        this.status = StatementStatus.GENERATED;
    }

    /**
     * Mark statement as generated with PDF and CSV paths
     */
    public void markAsGenerated(String pdfPath, String csvPath, long pdfSize, long csvSize) {
        this.pdfFilePath = pdfPath;
        this.csvFilePath = csvPath;
        this.pdfFileSize = pdfSize;
        this.csvFileSize = csvSize;
        this.generatedAt = LocalDateTime.now();
        this.status = StatementStatus.GENERATED;
    }

    /**
     * Mark statement as generated with S3 bucket
     */
    public void markAsGenerated(String pdfPath, String csvPath, long pdfSize, long csvSize, String bucket) {
        this.pdfFilePath = pdfPath;
        this.csvFilePath = csvPath;
        this.pdfFileSize = pdfSize;
        this.csvFileSize = csvSize;
        this.s3Bucket = bucket;
        this.generatedAt = LocalDateTime.now();
        this.status = StatementStatus.GENERATED;
    }

    /**
     * Mark statement as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount != null ? this.downloadCount : 0) + 1;
    }

    /**
     * Calculate net change in balance
     */
    public BigDecimal getNetChange() {
        if (closingBalance == null || openingBalance == null) {
            return BigDecimal.ZERO;
        }
        return closingBalance.subtract(openingBalance);
    }

    /**
     * Statement Type
     */
    public enum StatementType {
        MONTHLY,        // Monthly statement
        QUARTERLY,      // Quarterly statement
        ANNUAL,         // Annual statement
        ON_DEMAND       // On-demand/custom statement
    }

    /**
     * Statement Status
     */
    public enum StatementStatus {
        PENDING,        // Statement generation pending
        GENERATING,     // Statement is being generated
        GENERATED,      // Statement successfully generated
        FAILED,         // Statement generation failed
        ARCHIVED        // Statement archived
    }
}
