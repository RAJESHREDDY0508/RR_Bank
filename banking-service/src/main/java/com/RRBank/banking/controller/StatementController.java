package com.RRBank.banking.controller;

import com.RRBank.banking.dto.GenerateStatementRequestDto;
import com.RRBank.banking.dto.StatementResponseDto;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.service.OwnershipService;
import com.RRBank.banking.service.StatementService;
import com.RRBank.banking.util.SecurityUtil;
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
 * 
 * Phase 2A.1: Account ownership enforcement
 * - Customer can only generate/view statements for their own accounts
 * - Admin can access all statements
 * 
 * Phase 2C.3: Statement generation is synchronous (no @Async)
 * - generatedBy is correctly extracted from authentication
 * - No random UUID fallback
 */
@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Slf4j
public class StatementController {

    private final StatementService statementService;
    private final OwnershipService ownershipService;

    /**
     * Get all statements for an account
     * GET /api/statements/account/{accountId}
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<StatementResponseDto>> getAccountStatements(
            @PathVariable UUID accountId,
            Authentication authentication) {
        
        // Ownership check: customer can only view their own statements
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to get statements for accountId: {}", accountId);
        
        List<StatementResponseDto> statements = statementService.getAccountStatements(accountId);
        return ResponseEntity.ok(statements);
    }

    /**
     * Get statement by ID
     * GET /api/statements/{id}
     * 
     * Phase 2A.1: Ownership enforcement (via account ownership)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<StatementResponseDto> getStatementById(
            @PathVariable UUID id,
            Authentication authentication) {
        log.info("REST request to get statement by ID: {}", id);
        
        StatementResponseDto statement = statementService.getStatementById(id);
        
        // Ownership check: verify user owns the account this statement belongs to
        // StatementResponseDto.getAccountId() returns UUID, pass directly
        if (!SecurityUtil.isAdmin(authentication) && statement.getAccountId() != null) {
            ownershipService.requireAccountOwnership(statement.getAccountId(), authentication);
        }
        
        return ResponseEntity.ok(statement);
    }

    /**
     * Generate statement
     * POST /api/statements/generate
     * 
     * Phase 2A.1: Ownership enforcement - customer can only generate for their own accounts
     * Phase 2C.3: Statement generation is synchronous, generatedBy is correctly set
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<StatementResponseDto> generateStatement(
            @Valid @RequestBody GenerateStatementRequestDto request,
            Authentication authentication) {
        
        // Ownership check: customer can only generate statements for their own accounts
        Account account = ownershipService.requireAccountOwnership(request.getAccountId(), authentication);
        log.info("REST request to generate statement for accountId: {}", request.getAccountId());
        
        // Get the authenticated user ID - no random UUID fallback
        UUID generatedBy = SecurityUtil.requireUserId(authentication);
        
        // Statement generation is synchronous (no @Async)
        StatementResponseDto statement = statementService.generateStatement(request, generatedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(statement);
    }

    /**
     * Download statement PDF
     * GET /api/statements/{id}/download/pdf
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/{id}/download/pdf")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatementPdf(
            @PathVariable UUID id,
            Authentication authentication) {
        log.info("REST request to download statement PDF: {}", id);
        
        // Get statement first to check ownership
        StatementResponseDto statement = statementService.getStatementById(id);
        
        // Ownership check - getAccountId() returns UUID
        if (!SecurityUtil.isAdmin(authentication) && statement.getAccountId() != null) {
            ownershipService.requireAccountOwnership(statement.getAccountId(), authentication);
        }
        
        byte[] pdfContent = statementService.downloadStatementPdf(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
                String.format("statement_%s.pdf", statement.getStatementPeriod()));
        headers.setContentLength(pdfContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    /**
     * Download statement CSV
     * GET /api/statements/{id}/download/csv
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/{id}/download/csv")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatementCsv(
            @PathVariable UUID id,
            Authentication authentication) {
        log.info("REST request to download statement CSV: {}", id);
        
        // Get statement first to check ownership
        StatementResponseDto statement = statementService.getStatementById(id);
        
        // Ownership check - getAccountId() returns UUID
        if (!SecurityUtil.isAdmin(authentication) && statement.getAccountId() != null) {
            ownershipService.requireAccountOwnership(statement.getAccountId(), authentication);
        }
        
        byte[] csvContent = statementService.downloadStatementCsv(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", 
                String.format("statement_%s.csv", statement.getStatementPeriod()));
        headers.setContentLength(csvContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Download statement (default to PDF)
     * GET /api/statements/{id}/download
     * 
     * Phase 2A.1: Ownership enforcement
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable UUID id,
            Authentication authentication) {
        return downloadStatementPdf(id, authentication);
    }
}
