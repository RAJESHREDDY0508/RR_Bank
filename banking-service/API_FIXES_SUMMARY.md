# RR-Bank API Fixes Summary

## Issues Fixed

### 1. Frontend ↔ Backend API Mismatch

#### Account Creation Without customerId
**Problem:** Frontend doesn't have/submit `customerId` when creating accounts.

**Solution:**
- Modified `CreateAccountDto.java` - Made `customerId` optional (removed `@NotNull`)
- Modified `AccountController.java` - Auto-resolves `customerId` from authenticated user if not provided
- Added `resolveCustomerIdFromUserId()` method to `AccountService.java`

#### Deposit/Withdraw Alternative Endpoints
**Problem:** Some frontends call `POST /api/transactions/deposit|withdraw` instead of `/api/accounts/{id}/deposit|withdraw`.

**Solution:**
- Added `POST /api/transactions/deposit` endpoint to `TransactionController.java`
- Added `POST /api/transactions/withdraw` endpoint to `TransactionController.java`
- Created new DTOs:
  - `DepositWithAccountRequest.java` - Accepts accountId or accountNumber
  - `WithdrawWithAccountRequest.java` - Accepts accountId or accountNumber

#### Admin Dashboard Endpoint
**Problem:** Frontend calls `GET /api/admin/dashboard`, backend only had `/api/admin/dashboard/metrics`.

**Solution:**
- Added `GET /api/admin/dashboard` alias endpoint in `AdminController.java`

---

### 2. JWT_SECRET Startup Failure

**Problem:** In `local` profile, `jwt.secret: ${JWT_SECRET}` with no default → service won't start unless env var is set.

**Solution:**
- Modified `application-local.yml` to provide a default development secret:
  ```yaml
  jwt:
    secret: ${JWT_SECRET:rr-bank-local-development-secret-key-32-chars-minimum-for-hs256-algorithm}
  ```

⚠️ **WARNING:** This default is for LOCAL DEVELOPMENT ONLY. Always set `JWT_SECRET` environment variable in production!

---

### 3. Notification Endpoints Missing `/me` Convenience Endpoints

**Problem:** NotificationController required UUID in URL, didn't offer `/me/*` style endpoints that frontends expect.

**Solution:**
- Added convenience endpoints for authenticated user:
  - `GET /api/notifications` - Get all notifications for authenticated user
  - `GET /api/notifications/unread` - Get unread notifications
  - `GET /api/notifications/unread/count` - Get unread count
  - `PUT /api/notifications/read-all` - Mark all as read

- Added ownership validation to existing `/user/{userId}` endpoints
- Endpoints now verify that users can only access their own notifications (unless admin)

---

## Files Modified

1. **`application-local.yml`**
   - Added default JWT secret for local development

2. **`AccountController.java`**
   - Modified `createAccount()` to auto-resolve customerId from authenticated user

3. **`AccountService.java`**
   - Added `CustomerRepository` dependency
   - Added `resolveCustomerIdFromUserId()` helper method

4. **`CreateAccountDto.java`**
   - Made `customerId` optional (removed @NotNull validation)

5. **`TransactionController.java`**
   - Added `POST /api/transactions/deposit` endpoint
   - Added `POST /api/transactions/withdraw` endpoint
   - Added overloaded `resolveAccountId()` helper method

6. **`NotificationController.java`**
   - Added `/me` style convenience endpoints
   - Added ownership validation with `validateOwnership()` helper
   - Added `extractUserId()` helper method
   - Added Authentication import

7. **`AdminController.java`**
   - Added `GET /api/admin/dashboard` alias endpoint

## New Files Created

1. **`DepositWithAccountRequest.java`**
   - DTO for `/api/transactions/deposit` endpoint
   - Accepts either `accountId` (UUID) or `accountNumber` (String)

2. **`WithdrawWithAccountRequest.java`**
   - DTO for `/api/transactions/withdraw` endpoint
   - Accepts either `accountId` (UUID) or `accountNumber` (String)

---

## API Endpoint Summary

### Account Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/accounts` | CUSTOMER/ADMIN | Create account (customerId now optional) |
| GET | `/api/accounts/me` | CUSTOMER/ADMIN | Get authenticated user's accounts |
| GET | `/api/accounts` | ADMIN | Get all accounts |
| POST | `/api/accounts/{id}/deposit` | CUSTOMER/ADMIN | Deposit to account |
| POST | `/api/accounts/{id}/withdraw` | CUSTOMER/ADMIN | Withdraw from account |

### Transaction Endpoints (NEW)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/transactions/deposit` | CUSTOMER/ADMIN | Deposit (alternative) |
| POST | `/api/transactions/withdraw` | CUSTOMER/ADMIN | Withdraw (alternative) |

### Notification Endpoints (NEW `/me` style)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/notifications` | CUSTOMER/ADMIN | Get my notifications |
| GET | `/api/notifications/unread` | CUSTOMER/ADMIN | Get my unread notifications |
| GET | `/api/notifications/unread/count` | CUSTOMER/ADMIN | Get my unread count |
| PUT | `/api/notifications/read-all` | CUSTOMER/ADMIN | Mark all my notifications as read |

### Admin Endpoints
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/dashboard` | ADMIN | Dashboard metrics (NEW alias) |
| GET | `/api/admin/dashboard/metrics` | ADMIN | Dashboard metrics |

---

## Testing Recommendations

1. **Account Creation:**
   ```bash
   # Without customerId (auto-resolved)
   curl -X POST http://localhost:8081/api/accounts \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"accountType": "SAVINGS", "initialBalance": 1000}'
   ```

2. **Deposit via Transactions Endpoint:**
   ```bash
   curl -X POST http://localhost:8081/api/transactions/deposit \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"accountId": "uuid-here", "amount": 500, "description": "Test deposit"}'
   ```

3. **Notifications (authenticated user):**
   ```bash
   curl http://localhost:8081/api/notifications \
     -H "Authorization: Bearer $TOKEN"
   ```

4. **Local Startup (no JWT_SECRET needed):**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```
