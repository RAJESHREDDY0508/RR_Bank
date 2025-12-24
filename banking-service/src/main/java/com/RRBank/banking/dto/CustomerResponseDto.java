package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Response DTO
 * Used for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String kycStatus;
    private LocalDateTime kycVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Customer entity to CustomerResponseDto
     */
    public static CustomerResponseDto fromEntity(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .userId(customer.getUserId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .fullName(customer.getFullName())
                .dateOfBirth(customer.getDateOfBirth())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .city(customer.getCity())
                .state(customer.getState())
                .zipCode(customer.getZipCode())
                .country(customer.getCountry())
                .kycStatus(customer.getKycStatus().name())
                .kycVerifiedAt(customer.getKycVerifiedAt())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
