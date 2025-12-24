# RR-Bank API - Postman Ready Request Bodies

## Complete End-to-End Flow

### 1. Register User
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "rajkumar",
  "email": "raj.kumar@example.com",
  "password": "Password@123",
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "+1234567890"
}
```

### 2. Login
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "rajkumar",
  "password": "Password@123"
}
```
**Save:** `accessToken` and `userId` from response

### 3. Create Customer Profile
```
POST http://localhost:8080/api/customers
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "userId": "{{userId}}",
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
**Save:** `id` as `customerId`

### 4. Submit KYC
```
POST http://localhost:8080/api/customers/kyc
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "customerId": "{{customerId}}",
  "documentType": "PASSPORT",
  "documentNumber": "AB1234567"
}
```

### 5. Verify KYC (Admin)
```
PUT http://localhost:8080/api/customers/{{customerId}}/kyc/verify?approved=true&verifiedBy=ADMIN
Authorization: Bearer {{accessToken}}
```

### 6. Create Bank Account
```
POST http://localhost:8080/api/accounts
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "customerId": "{{customerId}}",
  "accountType": "SAVINGS",
  "initialBalance": 0.00,
  "currency": "USD"
}
```
**Save:** `id` as `accountId` (Status: PENDING)

### 7. Approve Account (Admin)
```
PUT http://localhost:8080/api/accounts/{{accountId}}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "status": "ACTIVE",
  "reason": "Account verified and approved"
}
```

### 8. Deposit Money
```
POST http://localhost:8080/api/transactions/transfer
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "fromAccountId": null,
  "toAccountId": "{{accountId}}",
  "amount": 1000.00,
  "description": "Initial deposit"
}
```

### 9. Transfer Money (requires 2nd account)
```
POST http://localhost:8080/api/transactions/transfer
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "fromAccountId": "{{accountId}}",
  "toAccountId": "{{accountId2}}",
  "amount": 200.00,
  "description": "Payment for services"
}
```

### 10. Check Balance
```
GET http://localhost:8080/api/accounts/{{accountId}}/balance
Authorization: Bearer {{accessToken}}
```

### 11. Get Transaction History
```
GET http://localhost:8080/api/transactions/account/{{accountId}}?page=0&size=20
Authorization: Bearer {{accessToken}}
```

---

## Valid Enum Values

| Field | Valid Values |
|-------|-------------|
| accountType | SAVINGS, CHECKING, CREDIT, BUSINESS |
| status | PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED |
| documentType | PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN |
| transactionType | TRANSFER, DEPOSIT, WITHDRAWAL |

## Date Formats
- dateOfBirth: `yyyy-MM-dd` (e.g., "1995-01-15")

## Notes
- All UUIDs are auto-generated
- Accounts are created with PENDING status (requires admin approval)
- Deposits: set `fromAccountId` to `null`
- Withdrawals: set `toAccountId` to `null`
