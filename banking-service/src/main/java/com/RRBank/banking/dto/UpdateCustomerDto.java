package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Update Customer Request DTO
 * Used for updating customer profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerDto {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
