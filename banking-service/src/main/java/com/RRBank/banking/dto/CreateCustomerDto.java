package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Create Customer Request DTO
 * Used when creating a new customer profile
 * 
 * Example JSON:
 * {
 *   "userId": "550e8400-e29b-41d4-a716-446655440000",
 *   "firstName": "Raj",
 *   "lastName": "Kumar",
 *   "dateOfBirth": "1995-01-01",
 *   "phone": "+1234567890",
 *   "address": "123 Main Street",
 *   "city": "New Jersey",
 *   "state": "NJ",
 *   "zipCode": "07001",
 *   "country": "USA"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerDto {

    @NotNull(message = "User ID is required")
    @JsonProperty("userId")
    private UUID userId;

    @NotBlank(message = "First name is required")
    @JsonProperty("firstName")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @JsonProperty("lastName")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @JsonProperty("phone")
    private String phone;

    @NotBlank(message = "Address is required")
    @JsonProperty("address")
    private String address;

    @NotBlank(message = "City is required")
    @JsonProperty("city")
    private String city;

    @NotBlank(message = "State is required")
    @JsonProperty("state")
    private String state;

    @NotBlank(message = "Zip code is required")
    @JsonProperty("zipCode")
    private String zipCode;

    @NotBlank(message = "Country is required")
    @JsonProperty("country")
    private String country;
}
