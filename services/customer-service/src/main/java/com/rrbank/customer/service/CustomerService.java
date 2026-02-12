package com.rrbank.customer.service;

import com.rrbank.customer.entity.Customer;
import com.rrbank.customer.entity.Customer.KycStatus;
import com.rrbank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
        customer.setKycStatus(KycStatus.APPROVED);
        return customerRepository.save(customer);
    }

    // ==================== KYC STATUS METHODS ====================

    @Transactional(readOnly = true)
    public List<Customer> getCustomersByKycStatus(KycStatus status) {
        return customerRepository.findByKycStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<Customer> getCustomersByKycStatus(KycStatus status, Pageable pageable) {
        return customerRepository.findByKycStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public long countByKycStatus(KycStatus status) {
        return customerRepository.countByKycStatus(status);
    }

    @Transactional(readOnly = true)
    public KycStatus getKycStatusByUserId(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        return customer.getKycStatus() != null ? customer.getKycStatus() : KycStatus.PENDING;
    }

    @Transactional
    public Customer approveKyc(UUID customerId) {
        Customer customer = getCustomer(customerId);
        customer.setKycStatus(KycStatus.APPROVED);
        customer.setKycVerified(true);
        customer.setKycVerifiedAt(LocalDateTime.now());
        customer.setKycRejectionReason(null);
        log.info("KYC approved for customer: {}", customerId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer approveKycByUserId(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        customer.setKycStatus(KycStatus.APPROVED);
        customer.setKycVerified(true);
        customer.setKycVerifiedAt(LocalDateTime.now());
        customer.setKycRejectionReason(null);
        log.info("KYC approved for user: {}", userId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer rejectKyc(UUID customerId, String reason) {
        Customer customer = getCustomer(customerId);
        customer.setKycStatus(KycStatus.REJECTED);
        customer.setKycVerified(false);
        customer.setKycRejectionReason(reason);
        log.info("KYC rejected for customer: {} - Reason: {}", customerId, reason);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer rejectKycByUserId(UUID userId, String reason) {
        Customer customer = getCustomerByUserId(userId);
        customer.setKycStatus(KycStatus.REJECTED);
        customer.setKycVerified(false);
        customer.setKycRejectionReason(reason);
        log.info("KYC rejected for user: {} - Reason: {}", userId, reason);
        return customerRepository.save(customer);
    }

    @Transactional
    public void migrateExistingCustomersKycStatus() {
        int approvedCount = customerRepository.migrateVerifiedToApproved();
        int pendingCount = customerRepository.migrateUnverifiedToPending();
        log.info("KYC status migration completed: {} set to APPROVED, {} set to PENDING", approvedCount, pendingCount);
    }
}
