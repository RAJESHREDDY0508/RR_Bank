package com.rrbank.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    public enum KycStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    private String address;
    private String city;
    private String state;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    private String country;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_rejection_reason")
    private String kycRejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
