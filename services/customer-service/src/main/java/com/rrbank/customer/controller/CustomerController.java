package com.rrbank.customer.controller;

import com.rrbank.customer.entity.Customer;
import com.rrbank.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Customer> getCustomerByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(customerService.getCustomerByUserId(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<Customer> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(customerService.getCustomerByUserId(UUID.fromString(userId)));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable UUID customerId,
            @RequestBody Customer updates) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, updates));
    }

    @PostMapping("/{customerId}/verify-kyc")
    public ResponseEntity<Customer> verifyKyc(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.verifyKyc(customerId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Customer Service is healthy");
    }
}
