package com.rrbank.customer.dto;

import lombok.*;
import java.time.LocalDate;

public class CustomerDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateCustomerRequest {
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private LocalDate dateOfBirth;
        private Boolean kycVerified;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateCustomerRequest {
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String address;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private LocalDate dateOfBirth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerResponse {
        private String id;
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private LocalDate dateOfBirth;
        private Boolean kycVerified;
        private String createdAt;
        private String updatedAt;
    }
}
