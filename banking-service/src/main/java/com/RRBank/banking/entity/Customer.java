package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Entity - Bank customers with KYC information
 * 
 * IMPORTANT: 
 * - customer.id is UUID (native PostgreSQL UUID)
 * - customer.user_id references users.user_id but stored as UUID for FK reference
 * - The user_id in customers table is UUID type, not VARCHAR(36)
 * 
 * Database constraints:
 * - kyc_status: PENDING, IN_PROGRESS, VERIFIED, REJECTED, EXPIRED
 * - customer_segment: REGULAR, PREMIUM, VIP, CORPORATE
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_user_id", columnList = "user_id"),
    @Index(name = "idx_customers_phone", columnList = "phone"),
    @Index(name = "idx_customers_kyc_status", columnList = "kyc_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to User - stored as UUID in database
     * Note: There's a type mismatch between users.user_id (VARCHAR) and customers.user_id (UUID)
     * This is a known schema issue - the FK doesn't enforce strict type matching in PostgreSQL
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "ssn", length = 255)
    private String ssn;

    @Column(name = "id_type", length = 50)
    private String idType;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_segment", length = 50)
    @Builder.Default
    private CustomerSegment customerSegment = CustomerSegment.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_document_type", length = 50)
    private String kycDocumentType;

    @Column(name = "kyc_document_number", length = 100)
    private String kycDocumentNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (kycStatus == null) {
            kycStatus = KycStatus.PENDING;
        }
        if (customerSegment == null) {
            customerSegment = CustomerSegment.REGULAR;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get full name of customer
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if KYC is verified
     */
    public boolean isKycVerified() {
        return kycStatus == KycStatus.VERIFIED;
    }

    /**
     * Get userId as String (for compatibility with User.userId)
     */
    public String getUserIdAsString() {
        return userId != null ? userId.toString() : null;
    }

    /**
     * Set userId from String
     */
    public void setUserIdFromString(String userIdStr) {
        this.userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * KYC Status - MUST match database constraint
     * Database: CHECK (kyc_status IN ('PENDING', 'IN_PROGRESS', 'VERIFIED', 'REJECTED', 'EXPIRED'))
     */
    public enum KycStatus {
        PENDING,
        IN_PROGRESS,
        VERIFIED,
        REJECTED,
        EXPIRED
    }

    /**
     * Customer Segment - MUST match database constraint
     * Database: CHECK (customer_segment IN ('REGULAR', 'PREMIUM', 'VIP', 'CORPORATE'))
     */
    public enum CustomerSegment {
        REGULAR,
        PREMIUM,
        VIP,
        CORPORATE
    }
}
