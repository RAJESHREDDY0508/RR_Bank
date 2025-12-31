# RR-Bank Phase 2 - Secure & Correct Core Banking

## Overview
Phase 2 implements security, correctness, and reliability improvements to the core banking functionality.

**Date Completed:** December 29, 2024

---

## ✅ Phase 2A - Ownership & Authorization

### 2A.1 Account Ownership Enforcement

**New Files Created:**
- `util/SecurityUtil.java` - Helper class for authentication/authorization
- `service/OwnershipService.java` - Centralized ownership checks

**Controllers Updated:**
- `AccountController.java` - Ownership checks on deposit, withdraw, get balance, etc.
- `TransactionController.java` - Ownership checks on transfers and transaction history
- `PaymentController.java` - Ownership checks on bill and merchant payments
- `StatementController.java` - Ownership checks on statement generation and download

**Rule Implemented:**
- If admin → allow all operations
- Else customer can only access their own accounts
- 403 Forbidden if customer tries to access another user's account

---

## ✅ Phase 2B - Optional Idempotency

### 2B.1 Idempotency-Key Header Support

**Endpoints Supporting Idempotency:**
- `POST /api/accounts/{id}/deposit`
- `POST /api/accounts/{id}/withdraw`
- `POST /api/transactions/transfer`
- `POST /api/payments/bill`
- `POST /api/payments/merchant`

**Behavior:**
- If `Idempotency-Key` header is present:
  - Check for existing transaction with same key
  - If found with matching request → return cached response
  - If found with different request → return 409 Conflict
  - If not found → process normally, store result
- If `Idempotency-Key` header is absent → normal behavior (no change)

**Files Updated:**
- `entity/Payment.java` - Added `idempotencyKey` field
- `repository/PaymentRepository.java` - Added `findByIdempotencyKey` method
- `service/IdempotencyService.java` - Added `storeRecord` method
- `service/PaymentService.java` - Idempotency check/store logic

---

## ✅ Phase 2C - Statements & Transaction Correctness

### 2C.1 Payments Appear in Statements

**Problem:** Payments created `Payment` rows but statements read `Transaction` rows.

**Solution:** When creating a payment, also create a `Transaction` record.

**Files Updated:**
- `service/PaymentService.java` - Added `createPaymentTransaction()` method
  - Creates Transaction with type `PAYMENT`
  - Sets `balanceBefore` and `balanceAfter`
  - Links to payment reference

**Result:** Payments now appear in account statements and transaction history.

### 2C.2 Transfers Create Two Transaction Records

**Problem:** Transfers didn't reliably record before/after and weren't statement-friendly.

**Solution:** Create TWO Transaction rows per transfer:
1. `TRANSFER_OUT` - Debit on source account
2. `TRANSFER_IN` - Credit on destination account

**Files Updated:**
- `entity/Transaction.java` - Added `TRANSFER_OUT` and `TRANSFER_IN` to TransactionType enum
- `service/TransactionService.java` - Updated `transfer()` method to create two records

**Result:** 
- From account statement shows `TRANSFER_OUT` with correct before/after
- To account statement shows `TRANSFER_IN` with correct before/after

### 2C.3 Statement Generation is Synchronous

**Problems Fixed:**
1. `@Async` annotation removed from statement generation
2. `generatedBy` user ID extracted correctly (no random UUID fallback)
3. Ownership check enforced before generating

**Files Updated:**
- `controller/StatementController.java` - Uses `SecurityUtil.requireUserId()`
- `service/StatementService.java` - Removed `@Async`, validates `generatedBy` is not null

**Result:**
- Statement generate returns immediately with result
- `generatedBy` correctly reflects the authenticated user
- Cannot generate statement for another user's account

---

## ✅ Phase 2D - Reliability: Locking & Deadlock Safety

### 2D.1 Transfer Locking Order

**Problem:** Concurrent transfers could deadlock when two transfers lock accounts in opposite order.

**Solution:** Always lock accounts in deterministic order (smaller UUID first).

**Code in `TransactionService.transfer()`:**
```java
// Phase 2D.1: Deterministic lock ordering - always lock smaller UUID first
UUID firstLock = request.getFromAccountId().compareTo(request.getToAccountId()) < 0 
        ? request.getFromAccountId() : request.getToAccountId();
UUID secondLock = request.getFromAccountId().compareTo(request.getToAccountId()) < 0 
        ? request.getToAccountId() : request.getFromAccountId();

// Lock accounts in deterministic order
Account firstAccount = accountRepository.findByIdWithLock(firstLock)...
Account secondAccount = accountRepository.findByIdWithLock(secondLock)...
```

**Result:** Parallel transfers don't deadlock under load.

---

## Files Modified Summary

```
banking-service/src/main/java/com/RRBank/banking/
├── controller/
│   ├── AccountController.java      ✅ Ownership enforcement
│   ├── PaymentController.java      ✅ Ownership enforcement + idempotency header
│   ├── StatementController.java    ✅ Ownership enforcement + proper userId extraction
│   └── TransactionController.java  ✅ Ownership enforcement + idempotency header
├── entity/
│   ├── Payment.java                ✅ Added idempotencyKey field
│   └── Transaction.java            ✅ Added TRANSFER_OUT, TRANSFER_IN types
├── repository/
│   └── PaymentRepository.java      ✅ Added findByIdempotencyKey
├── service/
│   ├── IdempotencyService.java     ✅ Added storeRecord method
│   ├── OwnershipService.java       ✅ NEW - Centralized ownership checks
│   ├── PaymentService.java         ✅ Idempotency + Transaction creation
│   ├── StatementService.java       ✅ Removed @Async, proper userId
│   └── TransactionService.java     ✅ Two-sided transfers + lock ordering
└── util/
    └── SecurityUtil.java           ✅ NEW - Auth helper methods
```

---

## Phase 2 Definition of Done Checklist

| Requirement | Status |
|-------------|--------|
| ✅ Customer cannot act on other users' accounts | Implemented via OwnershipService |
| ✅ Statements include deposits/withdrawals/transfers/payments/refunds | Payments create Transaction records |
| ✅ Transfer history is correct for both accounts | TRANSFER_OUT + TRANSFER_IN model |
| ✅ Statement generation is synchronous and attributed correctly | Removed @Async, proper generatedBy |
| ✅ Optional idempotency works when header is present | Idempotency-Key header support |
| ✅ No transfer deadlocks under parallel calls | Deterministic lock ordering |

---

## Testing Recommendations

### Ownership Tests
```bash
# Test: Customer A cannot access Customer B's account
curl -H "Authorization: Bearer TOKEN_A" http://localhost:8081/api/accounts/{B_ACCOUNT_ID}
# Expected: 403 Forbidden

# Test: Admin can access any account
curl -H "Authorization: Bearer ADMIN_TOKEN" http://localhost:8081/api/accounts/{ANY_ACCOUNT_ID}
# Expected: 200 OK
```

### Idempotency Tests
```bash
# Test: Duplicate request with same key returns same result
curl -X POST -H "Idempotency-Key: test-123" -H "Content-Type: application/json" \
     -d '{"amount": 100}' http://localhost:8081/api/accounts/{id}/deposit
# Run twice - second request should return cached result

# Test: Same key with different amount returns 409
curl -X POST -H "Idempotency-Key: test-123" -H "Content-Type: application/json" \
     -d '{"amount": 200}' http://localhost:8081/api/accounts/{id}/deposit
# Expected: 409 Conflict
```

### Transfer Tests
```bash
# Test: Transfer creates two transaction records
curl -X POST -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" \
     -d '{"fromAccountId": "...", "toAccountId": "...", "amount": 100}' \
     http://localhost:8081/api/transactions/transfer
# Then check both accounts' transaction history
```

---

## Next Steps: Phase 3

Phase 3 will implement real-world banking features:
- Payees / Beneficiaries
- Holds & Pending Transactions
- Fraud review workflow
- KYC-lite workflow
- Scheduled payments + recurring transfers
- Dispute flow
