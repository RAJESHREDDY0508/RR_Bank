package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Update Customer Request DTO
 * Used for updating customer profile
 * 
 * Supported date formats: yyyy-MM-dd, MM-dd-yyyy, dd-MM-yyyy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerDto {

    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("dateOfBirth")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateOfBirth;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("zipCode")
    private String zipCode;
    
    @JsonProperty("country")
    private String country;
}
