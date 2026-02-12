package com.rrbank.transaction.controller;

import com.rrbank.transaction.dto.TransactionDTOs.*;
import com.rrbank.transaction.entity.Transaction;
import com.rrbank.transaction.repository.TransactionRepository;
import com.rrbank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:5173"})
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("POST deposit for account: {}", request.getAccountId());
        
        if (idempotencyKey != null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        
        UUID userUuid = userId != null ? UUID.fromString(userId) : null;
        TransactionResponse response = transactionService.deposit(request, userUuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("POST withdraw from account: {}", request.getAccountId());
        
        if (idempotencyKey != null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        
        UUID userUuid = userId != null ? UUID.fromString(userId) : null;
        TransactionResponse response = transactionService.withdraw(request, userUuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("POST transfer from {} to {}", request.getFromAccountId(), request.getToAccountId());
        
        if (idempotencyKey != null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        
        UUID userUuid = userId != null ? UUID.fromString(userId) : null;
        TransactionResponse response = transactionService.transfer(request, userUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET all transactions - page: {}, size: {}, status: {}, type: {}", page, size, status, type);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Transaction> transactions;
        if (status != null && !status.isEmpty()) {
            transactions = transactionRepository.findByStatusString(status, pageable);
        } else if (type != null && !type.isEmpty()) {
            transactions = transactionRepository.findByTypeString(type, pageable);
        } else {
            transactions = transactionRepository.findAll(pageable);
        }
        
        Page<TransactionResponse> response = transactions.map(this::toTransactionResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        log.info("GET transaction: {}", transactionId);
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<TransactionResponse> getByReference(@PathVariable String reference) {
        log.info("GET transaction by reference: {}", reference);
        return ResponseEntity.ok(transactionService.getTransactionByReference(reference));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getByAccount(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET transactions for account: {}, startDate: {}, endDate: {}, type: {}", 
                accountId, startDate, endDate, type);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (startDate != null || endDate != null || (type != null && !type.isEmpty())) {
            return ResponseEntity.ok(transactionService.getTransactionsByAccountAndDateRange(
                    accountId, startDate, endDate, type, pageable));
        }
        
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId, pageable));
    }

    @GetMapping(value = "/account/{accountId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("EXPORT transactions for account: {}, startDate: {}, endDate: {}", accountId, startDate, endDate);
        
        try {
            List<TransactionResponse> transactions = transactionService.exportTransactions(accountId, startDate, endDate);
            
            // Generate CSV
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Type,Description,Amount,Status,Reference\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (TransactionResponse tx : transactions) {
                csv.append(tx.getCreatedAt() != null ? tx.getCreatedAt().format(formatter) : "").append(",");
                csv.append(tx.getTransactionType()).append(",");
                csv.append("\"").append(tx.getDescription() != null ? tx.getDescription().replace("\"", "\"\"") : "").append("\",");
                csv.append(tx.getAmount()).append(",");
                csv.append(tx.getStatus()).append(",");
                csv.append(tx.getTransactionReference() != null ? tx.getTransactionReference() : "").append("\n");
            }
            
            String filename = "transactions_" + accountId.toString().substring(0, 8) + "_" + 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
            
            byte[] csvBytes = csv.toString().getBytes("UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvBytes.length);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");
            
            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exporting transactions for account: {}", accountId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is healthy");
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId() != null ? tx.getId().toString() : null)
                .transactionReference(tx.getTransactionReference())
                .transactionType(tx.getTransactionType().name())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .fromAccountId(tx.getFromAccountId() != null ? tx.getFromAccountId().toString() : null)
                .toAccountId(tx.getToAccountId() != null ? tx.getToAccountId().toString() : null)
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
