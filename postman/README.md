# RR-Bank Postman Collection

This folder contains the Postman collection and environments for testing the RR-Bank API.

## Files

| File | Description |
|------|-------------|
| `RR-Bank-API-Collection.postman_collection.json` | Complete API collection with 80+ endpoints |
| `RR-Bank-Local.postman_environment.json` | Local development environment (localhost:8080) |
| `RR-Bank-Dev.postman_environment.json` | Development server environment |

## How to Import

### Using Postman Desktop
1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop all JSON files, or click **Upload Files**
4. Click **Import**

### Using Postman Web
1. Go to [web.postman.co](https://web.postman.co)
2. Click **Import** in the sidebar
3. Upload the JSON files

## Setting Up Environment

1. After importing, click the **Environment** dropdown (top right)
2. Select **RR-Bank - Local Development**
3. Variables will be auto-populated as you make requests

## Testing Flow

### Recommended Test Order

1. **Authentication**
   - Register User → Tokens auto-saved
   - Login → Tokens auto-saved

2. **Customer Setup**
   - Create Customer → `customerId` auto-saved
   - Submit KYC

3. **Account Operations**
   - Create Account → `accountId` and `accountNumber` auto-saved
   - Get Balance
   - Get Account Details

4. **Transactions**
   - Transfer Money (need two accounts)
   - View Transaction History
   - Get Statistics

5. **Payments**
   - Process Bill Payment
   - Process Merchant Payment

6. **Statements**
   - Generate Statement
   - Download PDF/CSV

7. **Admin Operations** (requires ADMIN role)
   - View All Accounts
   - View Fraud Alerts
   - View Audit Logs

## Auto-Saved Variables

The collection automatically saves these variables after successful requests:

| Variable | Saved From |
|----------|------------|
| `accessToken` | Register/Login response |
| `refreshToken` | Register/Login response |
| `userId` | Register/Login response |
| `customerId` | Create Customer response |
| `accountId` | Create Account response |
| `accountNumber` | Create Account response |

## API Categories

### Public Endpoints (No Auth)
- `GET /api/auth/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /actuator/health`

### Customer Endpoints (CUSTOMER or ADMIN role)
- Account management
- Transactions
- Payments
- Statements
- Notifications
- MFA

### Admin Endpoints (ADMIN role only)
- Customer management
- KYC verification
- Fraud detection
- Audit logs
- System statistics

## Troubleshooting

### "Unauthorized" Error
- Make sure you've logged in first
- Check that `accessToken` variable is set
- Try refreshing the token using `POST /api/auth/refresh`

### "Not Found" Error
- Check that the resource ID exists
- Verify the URL path is correct

### Connection Refused
- Ensure backend is running on port 8080
- Run `verify-system.bat` to check all services

## Collection Structure

```
RR-Bank API Collection/
├── Auth/
│   ├── Register User
│   ├── Login
│   ├── Refresh Token
│   └── Health Check
├── Customers/
│   ├── Create Customer
│   ├── Get Customer by ID
│   ├── Update Customer
│   ├── Submit KYC
│   └── ... (10 endpoints)
├── Accounts/
│   ├── Create Account
│   ├── Get Account
│   ├── Get Balance
│   └── ... (12 endpoints)
├── Transactions/
│   ├── Transfer Money
│   ├── Get Transactions
│   ├── Search
│   └── ... (8 endpoints)
├── Payments/
│   ├── Bill Payment
│   ├── Merchant Payment
│   └── ... (9 endpoints)
├── Statements/
│   ├── Generate
│   ├── Download PDF
│   └── ... (5 endpoints)
├── Fraud Detection/
│   ├── Get Alerts
│   ├── Fraud Rules
│   └── ... (11 endpoints)
├── Audit/
│   ├── Get Logs
│   ├── Search
│   └── ... (12 endpoints)
├── Notifications/
│   ├── Get Notifications
│   ├── Mark as Read
│   └── ... (6 endpoints)
├── MFA/
│   ├── Setup TOTP
│   ├── Verify
│   └── ... (9 endpoints)
└── Actuator/
    ├── Health
    ├── Info
    └── Prometheus
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Dec 22, 2024 | Initial collection - Phase 0 baseline |

---
*Part of RR-Bank Phase 0 - Freeze & Baseline*
