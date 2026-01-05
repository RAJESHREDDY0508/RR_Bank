package com.rrbank.customer.service;

import com.rrbank.customer.entity.Customer;
import com.rrbank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creating customer for user: {}", customer.getUserId());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found for user: " + userId));
    }

    @Transactional
    public Customer updateCustomer(UUID customerId, Customer updates) {
        Customer customer = getCustomer(customerId);
        if (updates.getFirstName() != null) customer.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null) customer.setLastName(updates.getLastName());
        if (updates.getPhoneNumber() != null) customer.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getAddress() != null) customer.setAddress(updates.getAddress());
        if (updates.getCity() != null) customer.setCity(updates.getCity());
        if (updates.getState() != null) customer.setState(updates.getState());
        if (updates.getPostalCode() != null) customer.setPostalCode(updates.getPostalCode());
        if (updates.getCountry() != null) customer.setCountry(updates.getCountry());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer verifyKyc(UUID customerId) {
        Customer customer = getCustomer(customerId);
        customer.setKycVerified(true);
        customer.setKycVerifiedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }
}
