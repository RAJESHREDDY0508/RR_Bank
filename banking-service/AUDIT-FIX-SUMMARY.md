# RR-Bank Banking Application - Audit & Fix Summary

## Changes Made

### 1. DTO Fixes

#### CreateCustomerDto.java
- Added `@JsonFormat` for `dateOfBirth` field
- Added `@JsonProperty` annotations for all fields
- Added `@Past` validation for date of birth
- Added example JSON in JavaDoc

#### CreateAccountDto.java
- Changed `@NotNull` to `@NotBlank` for accountType
- Added `@Pattern` validation for valid account types
- Added `@JsonProperty` annotations
- Set default currency value with `@Builder.Default`
- Added example JSON in JavaDoc

#### TransferRequestDto.java
- **CRITICAL FIX**: Removed `@NotNull` from `fromAccountId` and `toAccountId`
- This allows DEPOSIT (null fromAccountId) and WITHDRAWAL (null toAccountId)
- Added `transactionType` field for explicit type specification
- Added `@JsonProperty` annotations

#### UpdateAccountStatusDto.java
- Changed `@NotNull` to `@NotBlank` for status
- Added `@Pattern` validation for valid statuses (including PENDING)
- Added `@JsonProperty` annotations

#### KycRequestDto.java
- Added `@Pattern` validation for document types
- Added `@JsonProperty` annotations
- Added example JSON in JavaDoc

### 2. Entity Fixes

#### Account.java
- Added `PENDING` status to `AccountStatus` enum
- Accounts now require admin approval before becoming ACTIVE

### 3. Service Fixes

#### AccountService.java
- Changed default account status from `ACTIVE` to `PENDING`
- Accounts now require admin approval workflow

#### TransactionService.java
- **MAJOR FIX**: Added support for DEPOSIT, WITHDRAWAL, and TRANSFER
- Added `determineTransactionType()` method for auto-detection
- Split transaction execution into separate methods:
  - `executeDeposit()` - credit to account
  - `executeWithdrawal()` - debit from account
  - `executeTransfer()` - debit and credit
- Updated `compensateTransaction()` to handle all transaction types
- Improved balance tracking on transactions

### 4. Security Fixes

#### SecurityConfig.java
- Changed endpoint restrictions to allow both CUSTOMER and ADMIN roles
- `/api/accounts/**` - now allows CUSTOMER and ADMIN
- `/api/transactions/**` - now allows CUSTOMER and ADMIN
- `/api/transfers/**` - now allows CUSTOMER and ADMIN
- `/api/customers/**` - now allows CUSTOMER and ADMIN

### 5. Exception Handling Fixes

#### GlobalExceptionHandler.java
- Added `HttpMessageNotReadableException` handler
- Provides clear error messages for:
  - Malformed JSON syntax
  - Type mismatches
  - Invalid date formats
  - Missing request body

---

## API Request Bodies (Postman Ready)

### Register User
```json
{
  "username": "rajkumar",
  "email": "raj.kumar@example.com",
  "password": "Password@123",
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "+1234567890"
}
```

### Login
```json
{
  "usernameOrEmail": "rajkumar",
  "password": "Password@123"
}
```

### Create Customer
```json
{
  "userId": "UUID_FROM_REGISTER",
  "firstName": "Raj",
  "lastName": "Kumar",
  "dateOfBirth": "1995-01-15",
  "phone": "+1234567890",
  "address": "123 Main Street",
  "city": "Newark",
  "state": "NJ",
  "zipCode": "07001",
  "country": "USA"
}
```

### Submit KYC
```json
{
  "customerId": "CUSTOMER_UUID",
  "documentType": "PASSPORT",
  "documentNumber": "AB1234567"
}
```

### Create Account
```json
{
  "customerId": "CUSTOMER_UUID",
  "accountType": "SAVINGS",
  "initialBalance": 0.00,
  "currency": "USD"
}
```

### Approve Account (Admin)
```json
{
  "status": "ACTIVE",
  "reason": "Account verified and approved"
}
```

### Deposit Money
```json
{
  "fromAccountId": null,
  "toAccountId": "ACCOUNT_UUID",
  "amount": 1000.00,
  "description": "Initial deposit"
}
```

### Transfer Money
```json
{
  "fromAccountId": "FROM_ACCOUNT_UUID",
  "toAccountId": "TO_ACCOUNT_UUID",
  "amount": 200.00,
  "description": "Payment"
}
```

### Withdrawal
```json
{
  "fromAccountId": "ACCOUNT_UUID",
  "toAccountId": null,
  "amount": 100.00,
  "description": "ATM withdrawal"
}
```

---

## Valid Enum Values

| Field | Valid Values |
|-------|-------------|
| accountType | SAVINGS, CHECKING, CREDIT, BUSINESS |
| status | PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED |
| documentType | PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN |
| transactionType | TRANSFER, DEPOSIT, WITHDRAWAL |

---

## Complete Flow

1. **Register User** → Get accessToken + userId
2. **Login** → Refresh accessToken if needed
3. **Create Customer** → Get customerId
4. **Submit KYC** → Status becomes IN_PROGRESS
5. **Verify KYC (Admin)** → Status becomes VERIFIED
6. **Create Account** → Status is PENDING
7. **Approve Account (Admin)** → Status becomes ACTIVE
8. **Deposit Money** → Balance updated
9. **Transfer/Withdraw** → Money moved
10. **Check Balance** → Current balance
11. **Transaction History** → All transactions

---

## Run the Application

```bash
mvn clean spring-boot:run
```

Application starts on: http://localhost:8080
