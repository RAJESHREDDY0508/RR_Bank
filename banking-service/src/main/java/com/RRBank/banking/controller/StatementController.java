package com.RRBank.banking.controller;

import com.RRBank.banking.dto.GenerateStatementRequestDto;
import com.RRBank.banking.dto.StatementResponseDto;
import com.RRBank.banking.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Statement Controller
 * REST API endpoints for statement management
 */
@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Slf4j
public class StatementController {

    private final StatementService statementService;

    /**
     * Get all statements for an account
     * GET /api/statements/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<StatementResponseDto>> getAccountStatements(@PathVariable UUID accountId) {
        log.info("REST request to get statements for accountId: {}", accountId);
        List<StatementResponseDto> statements = statementService.getAccountStatements(accountId);
        return ResponseEntity.ok(statements);
    }

    /**
     * Get statement by ID
     * GET /api/statements/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<StatementResponseDto> getStatementById(@PathVariable UUID id) {
        log.info("REST request to get statement by ID: {}", id);
        StatementResponseDto statement = statementService.getStatementById(id);
        return ResponseEntity.ok(statement);
    }

    /**
     * Generate statement
     * POST /api/statements/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<StatementResponseDto> generateStatement(
            @Valid @RequestBody GenerateStatementRequestDto request,
            Authentication authentication) {
        log.info("REST request to generate statement for accountId: {}", request.getAccountId());
        
        UUID generatedBy = extractUserIdFromAuthentication(authentication);
        
        StatementResponseDto statement = statementService.generateStatement(request, generatedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(statement);
    }

    /**
     * Download statement PDF
     * GET /api/statements/{id}/download/pdf
     */
    @GetMapping("/{id}/download/pdf")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatementPdf(@PathVariable UUID id) {
        log.info("REST request to download statement PDF: {}", id);
        
        byte[] pdfContent = statementService.downloadStatementPdf(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "statement.pdf");
        headers.setContentLength(pdfContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    /**
     * Download statement CSV
     * GET /api/statements/{id}/download/csv
     */
    @GetMapping("/{id}/download/csv")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatementCsv(@PathVariable UUID id) {
        log.info("REST request to download statement CSV: {}", id);
        
        byte[] csvContent = statementService.downloadStatementCsv(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "statement.csv");
        headers.setContentLength(csvContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Download statement (default to PDF)
     * GET /api/statements/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatement(@PathVariable UUID id) {
        return downloadStatementPdf(id);
    }

    // ========== HELPER METHODS ==========

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse userId from authentication: {}", authentication.getName());
                return UUID.randomUUID(); // Placeholder
            }
        }
        return UUID.randomUUID(); // Placeholder
    }
}
