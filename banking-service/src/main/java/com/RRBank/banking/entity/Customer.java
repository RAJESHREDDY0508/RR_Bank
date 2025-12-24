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
 * Customer Entity
 * Represents bank customers with profile and KYC information
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_user_id", columnList = "user_id"),
    @Index(name = "idx_customer_phone", columnList = "phone"),
    @Index(name = "idx_customer_kyc_status", columnList = "kyc_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

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
     * KYC Status Enum
     */
    public enum KycStatus {
        PENDING,
        IN_PROGRESS,
        VERIFIED,
        REJECTED,
        EXPIRED
    }
}
