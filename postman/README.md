# RR-Bank Postman Collection

Updated Postman collection with **Deposit**, **Withdrawal**, and **Initial Deposit** transaction support.

## ✅ What's New

1. **Auto-Create Initial Deposit Transaction**
   - When creating an account with `initialBalance > 0`, an "Initial Deposit" transaction is automatically created
   - This transaction appears in the Transactions API
   - No duplicate logic - reuses existing transaction service

2. **Deposit API** - `POST /api/accounts/{accountId}/deposit`
3. **Withdrawal API** - `POST /api/accounts/{accountId}/withdraw`
4. **My Accounts API** - `GET /api/accounts/me`

## Files

| File | Description |
|------|-------------|
| `RR-Bank-API-Collection.postman_collection.json` | Complete API collection |
| `RR-Bank-Local.postman_environment.json` | Local environment (localhost:8080) |
| `RR-Bank-Dev.postman_environment.json` | Dev server environment |

---

## 🚀 How to Start the Application

### Prerequisites
- Java 21+
- PostgreSQL running on port 5432
- Redis running on port 6379 (optional, for caching)

### Step 1: Start PostgreSQL
```bash
# If using Docker
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=rrbank -p 5432:5432 postgres:15

# Or ensure your local PostgreSQL is running
```

### Step 2: Start Redis (Optional)
```bash
# If using Docker
docker run -d --name redis -p 6379:6379 redis:7
```

### Step 3: Navigate to Banking Service
```bash
cd C:\Users\rajes\Desktop\projects\RR-Bank\banking-service
```

### Step 4: Build the Application
```bash
# Windows
.\mvnw.cmd clean compile

# Or if you have Maven installed globally
mvn clean compile
```

### Step 5: Run the Application
```bash
# Windows - using Maven wrapper
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Or with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or run the JAR directly after building
.\mvnw.cmd clean package -DskipTests
java -jar target/banking-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Step 6: Verify Application is Running
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

---

## 📬 Import into Postman

1. Open **Postman**
2. Click **Import** (top left)
3. Navigate to: `C:\Users\rajes\Desktop\projects\RR-Bank\postman\`
4. Select all JSON files
5. Click **Import**
6. Select environment: **RR-Bank - Local Development**

---

## 🧪 Testing Flow

### Complete Test Sequence:

1. **Register User** → Auto-saves `accessToken`, `userId`
2. **Create Customer** → Auto-saves `customerId`
3. **Create Account** (with initialBalance: 1000) → Auto-saves `accountId`
4. **Get Transactions by Account** → Should show "Initial Deposit" transaction
5. **Deposit Money** → Add $500, verify balance updated
6. **Get Transactions by Account** → Should show 2 transactions
7. **Withdraw Money** → Remove $100, verify balance updated
8. **Get Transactions by Account** → Should show 3 transactions

---

## 📋 API Reference

### Account APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts` | Create account (auto-creates initial deposit) |
| GET | `/api/accounts/me` | Get my accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts/{id}/balance` | Get account balance |
| POST | `/api/accounts/{id}/deposit` | Deposit money |
| POST | `/api/accounts/{id}/withdraw` | Withdraw money |

### Transaction APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/transfer` | Transfer between accounts |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| GET | `/api/transactions/account/{id}` | Get transactions for account |
| GET | `/api/transactions/account/{id}/recent` | Get recent transactions |
| GET | `/api/transactions/account/{id}/stats` | Get transaction statistics |
| GET | `/api/transactions/search` | Search transactions |

---

## 📝 Example Requests

### Create Account with Initial Balance
```json
POST /api/accounts
{
    "customerId": "{{customerId}}",
    "accountType": "CHECKING",
    "currency": "USD",
    "initialBalance": 1000.00
}
```

**Result:** Account created + "Initial Deposit" transaction created

### Deposit Money
```json
POST /api/accounts/{{accountId}}/deposit
{
    "amount": 500.00,
    "currency": "USD",
    "description": "Cash deposit"
}
```

### Withdraw Money
```json
POST /api/accounts/{{accountId}}/withdraw
{
    "amount": 100.00,
    "currency": "USD",
    "description": "ATM withdrawal"
}
```

---

## 🔍 Validation Checklist

After running the test sequence, verify:

- [x] Account created from frontend/Postman
- [x] Initial balance appears as first transaction ("Initial Deposit")
- [x] Deposit via API updates balance
- [x] Deposit appears in transaction history
- [x] Withdrawal via API updates balance
- [x] Withdrawal appears in transaction history
- [x] Application starts without errors
- [x] No duplicate transactions
- [x] All APIs return expected responses

---

## ⚠️ Troubleshooting

### "Account not found"
- Ensure you've created an account first
- Check that `accountId` variable is set in Postman

### "Insufficient balance"
- Check account balance before withdrawing
- Deposit more money first

### "Cannot deposit to inactive account"
- Account status must be ACTIVE
- Use Update Account Status (Admin) to activate

### Connection Refused
- Ensure backend is running on port 8080
- Check PostgreSQL is running on port 5432
- Run: `curl http://localhost:8080/actuator/health`

---

## 📂 Project Structure

```
C:\Users\rajes\Desktop\projects\RR-Bank\
├── banking-service/                 # Spring Boot Backend
│   ├── src/main/java/com/RRBank/banking/
│   │   ├── controller/
│   │   │   ├── AccountController.java     # Deposit/Withdraw endpoints
│   │   │   └── TransactionController.java # Transaction queries
│   │   ├── service/
│   │   │   ├── AccountService.java        # Creates initial deposit
│   │   │   └── TransactionService.java    # Deposit/Withdraw logic
│   │   └── dto/
│   │       ├── DepositRequest.java
│   │       └── WithdrawRequest.java
│   └── pom.xml
├── frontend/                        # React Frontend
└── postman/                         # This folder
    ├── RR-Bank-API-Collection.postman_collection.json
    ├── RR-Bank-Local.postman_environment.json
    ├── RR-Bank-Dev.postman_environment.json
    └── README.md
```

---

*Updated: December 26, 2024*
