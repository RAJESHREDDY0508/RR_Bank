# API Reference

All API requests are routed through the API Gateway at `http://localhost:8080`. Authenticated endpoints require a JWT token in the `Authorization` header.

## Authentication

### Register

```
POST /api/auth/register
```

**Request Body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:** `201 Created`

```json
{
  "message": "User registered successfully",
  "userId": "uuid"
}
```

### Login

```
POST /api/auth/login
```

**Request Body:**

```json
{
  "usernameOrEmail": "john@example.com",
  "password": "Password123!"
}
```

**Response:** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "userId": "uuid"
}
```

All subsequent requests must include:

```
Authorization: Bearer <token>
```

---

## Accounts

### Create Account

```
POST /api/accounts
```

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "accountType": "SAVINGS"
}
```

**Account Types:** `SAVINGS`, `CHECKING`

### List Accounts

```
GET /api/accounts
```

Returns all accounts belonging to the authenticated user.

### Get Account Details

```
GET /api/accounts/{accountId}
```

### Get Account Balance

```
GET /api/accounts/{accountId}/balance
```

Balance is fetched from the Ledger Service (source of truth).

---

## Transactions

### Deposit

```
POST /api/transactions/deposit
```

**Request Body:**

```json
{
  "accountId": "uuid",
  "amount": 1000.00,
  "description": "Initial deposit"
}
```

### Withdrawal

```
POST /api/transactions/withdraw
```

**Request Body:**

```json
{
  "accountId": "uuid",
  "amount": 200.00,
  "description": "ATM withdrawal"
}
```

### Transfer

```
POST /api/transactions/transfer
```

**Request Body:**

```json
{
  "fromAccountId": "source-uuid",
  "toAccountId": "destination-uuid",
  "amount": 500.00,
  "description": "Rent payment"
}
```

### Transaction History

```
GET /api/transactions?accountId={accountId}&page=0&size=20
```

**Query Parameters:**

| Parameter   | Type   | Default | Description              |
|-------------|--------|---------|--------------------------|
| `accountId` | UUID   | --      | Filter by account        |
| `page`      | int    | 0       | Page number              |
| `size`      | int    | 20      | Results per page         |

---

## Customers

### Get Customer Profile

```
GET /api/customers/profile
```

Returns the authenticated user's customer profile including KYC status.

### Get Customer by ID (Admin)

```
GET /api/customers/{customerId}
```

---

## KYC (Admin)

### List KYC Requests

```
GET /api/admin/kyc/pending
```

### Approve KYC

```
PUT /api/admin/kyc/{customerId}/approve
```

### Reject KYC

```
PUT /api/admin/kyc/{customerId}/reject
```

---

## Admin Dashboard

### Dashboard Metrics

```
GET /api/admin/dashboard/stats
```

Returns aggregate statistics: total customers, accounts, transactions, and fraud alerts.

### Fraud Alerts

```
GET /api/admin/fraud-alerts?page=0&size=20
```

### Audit Logs

```
GET /api/admin/audit-logs?page=0&size=20
```

**Query Parameters:**

| Parameter | Type   | Description                    |
|-----------|--------|--------------------------------|
| `page`    | int    | Page number                    |
| `size`    | int    | Results per page               |
| `action`  | string | Filter by action type          |
| `from`    | date   | Start date (ISO 8601)          |
| `to`      | date   | End date (ISO 8601)            |

---

## Error Responses

All error responses follow a consistent format:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "timestamp": "2026-01-15T10:30:00Z"
}
```

### Common Status Codes

| Code | Meaning                                         |
|------|-------------------------------------------------|
| 200  | Success                                         |
| 201  | Resource created                                |
| 400  | Invalid request body or parameters              |
| 401  | Missing or invalid authentication token         |
| 403  | Insufficient permissions                        |
| 404  | Resource not found                              |
| 409  | Conflict (e.g., duplicate registration)         |
| 422  | Business rule violation (e.g., insufficient funds) |
| 500  | Internal server error                           |

## Fraud Limits

Transactions are subject to the following limits enforced by the Fraud Service:

| Rule              | Limit                    |
|-------------------|--------------------------|
| Daily maximum     | $10,000 per account      |
| Withdrawal velocity | 5 per hour per account |
| Risk score threshold | Configurable per rule  |

Transactions exceeding these limits are automatically flagged and may be blocked.

---

## Postman Collection

A complete Postman collection with pre-configured requests and test flows is available in the `postman/` directory. Import it into Postman for interactive API testing.
