# RR-Bank Frontend Integration Complete

## What Was Updated

### Customer Frontend (`frontend/src/`)

1. **bankService.js** - Complete API service layer with:
   - Enhanced auth service (login, logout, forgot password, reset password)
   - Account service (open account request, view requests, cancel request)
   - Transaction service (deposit, withdraw, transfer with idempotency)
   - Ledger entries API
   - Transaction limits API
   - Admin service (for admin users)

2. **Accounts.jsx** - Enhanced with:
   - Open new account request (goes to admin approval)
   - View pending/approved/rejected requests
   - Cancel pending requests
   - Transaction limits display

3. **Transfer.jsx** - Complete money movement page:
   - Deposit tab
   - Withdraw tab
   - Transfer tab
   - Real-time limit validation
   - Idempotency key generation

4. **Transactions.jsx** - Enhanced with:
   - Transaction history view
   - Ledger entries view (toggle)
   - Pagination
   - Date filtering

5. **ForgotPassword.jsx** - New page for password reset request
6. **ResetPassword.jsx** - New page to reset password with token
7. **Login.jsx** - Updated with forgot password link

### Admin Frontend (`frontend/admin-app/src/`)

1. **accountRequests.ts** - New API for account requests
2. **userLimits.ts** - New API for user limit management
3. **AccountRequests.tsx** - New page with:
   - View all pending account requests
   - Approve with notes
   - Reject with reason
   - View request details
4. **Sidebar.tsx** - Updated with Account Requests link + badge count
5. **App.tsx** - Added AccountRequests route

### Backend (Already completed in previous session)

- LedgerService, IdempotencyService, TransactionLimitService
- AccountRequestService
- All entities, repositories, DTOs
- V14 migration

---

## How to Run

### 1. Start Backend

```bash
cd banking-service

# Run migrations (make sure Flyway is enabled)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 2. Start Customer Frontend

```bash
cd frontend

# Install dependencies (including uuid)
npm install

# Start dev server
npm run dev
```

Customer frontend runs on: http://localhost:5173

### 3. Start Admin Frontend

```bash
cd frontend/admin-app

# Install dependencies
npm install

# Start dev server
npm run dev
```

Admin frontend runs on: http://localhost:5174

---

## Test Flow

### Customer Flow:
1. Register new user at http://localhost:5173/register
2. Login
3. Go to Accounts page → Click "Open New Account"
4. Select account type, enter initial deposit, submit
5. Request goes to PENDING status

### Admin Flow:
1. Login as admin at http://localhost:5174/login
2. Go to "Account Requests" in sidebar
3. See pending requests with badge count
4. Click Approve or Reject
5. Customer's account is created (if approved)

### Transaction Flow:
1. Customer logs in
2. Goes to Transfer page
3. Selects Deposit/Withdraw/Transfer tab
4. Enters amount (validates against limits)
5. Transaction creates ledger entries
6. Balance updated from ledger

---

## API Endpoints Used

### Customer APIs:
- POST `/api/auth/register` - Register
- POST `/api/auth/login` - Login
- POST `/api/auth/logout` - Logout
- POST `/api/auth/forgot-password` - Request reset
- POST `/api/auth/reset-password` - Reset password
- GET `/api/accounts` - Get user's accounts
- POST `/api/accounts` - Request new account
- GET `/api/accounts/requests` - Get user's requests
- DELETE `/api/accounts/requests/{id}` - Cancel request
- POST `/api/transactions/deposit` - Deposit
- POST `/api/transactions/withdraw` - Withdraw
- POST `/api/transactions/transfer` - Transfer
- GET `/api/transactions/ledger` - Ledger entries
- GET `/api/transactions/limits` - User's limits

### Admin APIs:
- GET `/api/admin/account-requests` - Pending requests
- GET `/api/admin/account-requests/count` - Count
- POST `/api/admin/account-requests/{id}/approve` - Approve
- POST `/api/admin/account-requests/{id}/reject` - Reject
- GET `/api/admin/users/{id}/limits` - User limits
- PUT `/api/admin/users/{id}/limits` - Update limits

---

## Important Notes

1. **Idempotency**: All money operations require `Idempotency-Key` header
2. **Limits**: Default limits are $5,000/transaction, $10,000/day, $100,000/month
3. **Velocity**: Max 20 transactions per hour
4. **Ledger**: Balance = SUM(CREDIT) - SUM(DEBIT)
5. **Account Approval**: New accounts require admin approval
