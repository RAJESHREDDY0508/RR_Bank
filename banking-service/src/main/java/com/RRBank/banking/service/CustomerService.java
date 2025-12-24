package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.event.CustomerCreatedEvent;
import com.RRBank.banking.event.CustomerUpdatedEvent;
import com.RRBank.banking.event.KycVerifiedEvent;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.CustomerRepository;
import com.RRBank.banking.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer Service
 * Business logic for customer management
 */
@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    
    @Autowired(required = false)
    private CustomerEventProducer eventProducer;
    
    @Autowired
    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get user ID by username
     * Used to extract userId from JWT token authentication
     */
    @Transactional(readOnly = true)
    public UUID getUserIdByUsername(String username) {
        log.debug("Looking up userId for username: {}", username);
        return userRepository.findByUsername(username)
                .map(user -> {
                    try {
                        return UUID.fromString(user.getUserId());
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID format for userId: {}", user.getUserId());
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Create new customer profile
     */
    @Transactional
    public CustomerResponseDto createCustomer(CreateCustomerDto dto) {
        log.info("Creating customer for userId: {}", dto.getUserId());
        
        // Validate userId is provided
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required. Please login first.");
        }

        // Check if customer already exists for this user
        if (customerRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("Customer profile already exists for this user");
        }

        // Create customer entity
        Customer customer = Customer.builder()
                .userId(dto.getUserId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry())
                .kycStatus(Customer.KycStatus.PENDING)
                .build();

        customer = customerRepository.save(customer);
        log.info("Customer created with ID: {}", customer.getId());

        // Publish event to Kafka
        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
                .customerId(customer.getId())
                .userId(customer.getUserId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt())
                .build();
        
        if (eventProducer != null) {
            eventProducer.publishCustomerCreated(event);
        }

        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Get customer by ID
     */
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(UUID customerId) {
        log.info("Fetching customer with ID: {}", customerId);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
        
        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Get customer by user ID
     */
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByUserId(UUID userId) {
        log.info("Fetching customer for userId: {}", userId);
        
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for user ID: " + userId));
        
        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Update customer profile
     */
    @Transactional
    public CustomerResponseDto updateCustomer(UUID customerId, UpdateCustomerDto dto) {
        log.info("Updating customer with ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Track updated fields
        Map<String, Object> updatedFields = new HashMap<>();

        if (dto.getFirstName() != null && !dto.getFirstName().equals(customer.getFirstName())) {
            customer.setFirstName(dto.getFirstName());
            updatedFields.put("firstName", dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().equals(customer.getLastName())) {
            customer.setLastName(dto.getLastName());
            updatedFields.put("lastName", dto.getLastName());
        }

        if (dto.getDateOfBirth() != null) {
            customer.setDateOfBirth(dto.getDateOfBirth());
            updatedFields.put("dateOfBirth", dto.getDateOfBirth());
        }

        if (dto.getPhone() != null) {
            customer.setPhone(dto.getPhone());
            updatedFields.put("phone", dto.getPhone());
        }

        if (dto.getAddress() != null) {
            customer.setAddress(dto.getAddress());
            updatedFields.put("address", dto.getAddress());
        }

        if (dto.getCity() != null) {
            customer.setCity(dto.getCity());
            updatedFields.put("city", dto.getCity());
        }

        if (dto.getState() != null) {
            customer.setState(dto.getState());
            updatedFields.put("state", dto.getState());
        }

        if (dto.getZipCode() != null) {
            customer.setZipCode(dto.getZipCode());
            updatedFields.put("zipCode", dto.getZipCode());
        }

        if (dto.getCountry() != null) {
            customer.setCountry(dto.getCountry());
            updatedFields.put("country", dto.getCountry());
        }

        customer = customerRepository.save(customer);
        log.info("Customer updated with ID: {}", customerId);

        // Publish event if any fields were updated
        if (!updatedFields.isEmpty()) {
            CustomerUpdatedEvent event = CustomerUpdatedEvent.builder()
                    .customerId(customer.getId())
                    .userId(customer.getUserId())
                    .updatedFields(updatedFields)
                    .updatedAt(customer.getUpdatedAt())
                    .build();
            
            if (eventProducer != null) {
                eventProducer.publishCustomerUpdated(event);
            }
        }

        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Submit KYC for verification
     */
    @Transactional
    public CustomerResponseDto submitKyc(KycRequestDto dto) {
        log.info("Submitting KYC for customer ID: {}", dto.getCustomerId());

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + dto.getCustomerId()));

        // Update KYC information
        customer.setKycDocumentType(dto.getDocumentType());
        customer.setKycDocumentNumber(dto.getDocumentNumber());
        customer.setKycStatus(Customer.KycStatus.IN_PROGRESS);

        customer = customerRepository.save(customer);
        log.info("KYC submitted for customer ID: {}", customer.getId());

        // In a real system, this would trigger an automated or manual KYC verification process
        // For now, we'll auto-approve (in production, this would be done by a separate service)

        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Verify KYC (Admin operation)
     */
    @Transactional
    public CustomerResponseDto verifyKyc(UUID customerId, boolean approved, String verifiedBy) {
        log.info("Verifying KYC for customer ID: {}, approved: {}", customerId, approved);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        Customer.KycStatus newStatus = approved ? Customer.KycStatus.VERIFIED : Customer.KycStatus.REJECTED;
        customer.setKycStatus(newStatus);
        
        if (approved) {
            customer.setKycVerifiedAt(LocalDateTime.now());
        }

        customer = customerRepository.save(customer);
        log.info("KYC verification completed for customer ID: {}, status: {}", customerId, newStatus);

        // Publish KYC verified event
        KycVerifiedEvent event = KycVerifiedEvent.builder()
                .customerId(customer.getId())
                .userId(customer.getUserId())
                .kycStatus(newStatus.name())
                .documentType(customer.getKycDocumentType())
                .verifiedAt(customer.getKycVerifiedAt())
                .verifiedBy(verifiedBy)
                .build();
        
        if (eventProducer != null) {
            eventProducer.publishKycVerified(event);
        }

        return CustomerResponseDto.fromEntity(customer);
    }

    /**
     * Get all customers (Admin operation)
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        log.info("Fetching all customers");
        
        return customerRepository.findAll().stream()
                .map(CustomerResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search customers by name
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> searchCustomers(String search) {
        log.info("Searching customers with query: {}", search);
        
        return customerRepository.searchByName(search).stream()
                .map(CustomerResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get customers by KYC status
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getCustomersByKycStatus(String status) {
        log.info("Fetching customers with KYC status: {}", status);
        
        Customer.KycStatus kycStatus = Customer.KycStatus.valueOf(status.toUpperCase());
        
        return customerRepository.findByKycStatus(kycStatus).stream()
                .map(CustomerResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending KYC customers
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getPendingKycCustomers() {
        log.info("Fetching pending KYC customers");
        
        return customerRepository.findPendingKycCustomers().stream()
                .map(CustomerResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Delete customer (Admin operation - soft delete or hard delete)
     */
    @Transactional
    public void deleteCustomer(UUID customerId) {
        log.info("Deleting customer with ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        customerRepository.delete(customer);
        log.info("Customer deleted with ID: {}", customerId);
    }
}
