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
 * Statement Entity
 * Represents account statements (monthly, quarterly, or on-demand)
 */
@Entity
@Table(name = "statements", indexes = {
    @Index(name = "idx_statement_account", columnList = "account_id"),
    @Index(name = "idx_statement_customer", columnList = "customer_id"),
    @Index(name = "idx_statement_period", columnList = "statement_period"),
    @Index(name = "idx_statement_status", columnList = "status"),
    @Index(name = "idx_statement_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "statement_period", nullable = false, length = 7)
    private String statementPeriod; // Format: YYYY-MM

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "total_deposits", precision = 15, scale = 2)
    private BigDecimal totalDeposits;

    @Column(name = "total_withdrawals", precision = 15, scale = 2)
    private BigDecimal totalWithdrawals;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatementStatus status;

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath; // S3 path

    @Column(name = "csv_file_path", length = 500)
    private String csvFilePath; // S3 path

    @Column(name = "pdf_file_size")
    private Long pdfFileSize; // In bytes

    @Column(name = "csv_file_size")
    private Long csvFileSize; // In bytes

    @Column(name = "s3_bucket", length = 200)
    private String s3Bucket;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by")
    private UUID generatedBy; // User who requested (null for automated)

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

    @Column(name = "last_downloaded_at")
    private LocalDateTime lastDownloadedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (status == null) {
            status = StatementStatus.PENDING;
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if statement is generated
     */
    public boolean isGenerated() {
        return status == StatementStatus.GENERATED;
    }

    /**
     * Check if statement is available for download
     */
    public boolean isAvailableForDownload() {
        return status == StatementStatus.GENERATED && pdfFilePath != null;
    }

    /**
     * Mark as generated
     */
    public void markAsGenerated(String pdfPath, String csvPath, long pdfSize, long csvSize, String bucket) {
        this.status = StatementStatus.GENERATED;
        this.pdfFilePath = pdfPath;
        this.csvFilePath = csvPath;
        this.pdfFileSize = pdfSize;
        this.csvFileSize = csvSize;
        this.s3Bucket = bucket;
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String error) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = error;
    }

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
        this.lastDownloadedAt = LocalDateTime.now();
    }

    /**
     * Statement Type Enum
     */
    public enum StatementType {
        MONTHLY,        // Monthly statement
        QUARTERLY,      // Quarterly statement
        ANNUAL,         // Annual statement
        ON_DEMAND       // User-requested statement
    }

    /**
     * Statement Status Enum
     */
    public enum StatementStatus {
        PENDING,        // Waiting to be generated
        GENERATING,     // Being generated
        GENERATED,      // Successfully generated
        FAILED,         // Generation failed
        ARCHIVED        // Archived (old statement)
    }
}
