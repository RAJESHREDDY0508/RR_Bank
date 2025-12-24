# RR-Bank Banking Application

A comprehensive, enterprise-grade banking application built with Spring Boot 4.0, featuring modern banking functionalities, secure authentication, and microservices-ready architecture.

## 🏗️ Architecture

### Technology Stack

**Backend:**
- Spring Boot 4.0
- Java 21
- PostgreSQL (Database)
- Redis (Caching)
- Kafka (Event Streaming)
- Flyway (Database Migrations)
- JWT (Authentication)

**Frontend:**
- React 18
- Tailwind CSS
- Lucide React Icons

### System Architecture

```
┌─────────────────┐
│   React Client  │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│   API Gateway   │ (Port 8080)
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────┐
│        Banking Application          │
│  ┌──────────┐  ┌──────────┐        │
│  │   Auth   │  │ Account  │        │
│  │ Service  │  │ Service  │        │
│  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐        │
│  │Transaction│  │  Cache   │        │
│  │  Service │  │  (Redis) │        │
│  └──────────┘  └──────────┘        │
└─────────┬───────────────────────────┘
          │
          ↓
┌─────────────────────────────────────┐
│  ┌──────────┐  ┌──────────┐        │
│  │PostgreSQL│  │  Kafka   │        │
│  │ Database │  │  Events  │        │
│  └──────────┘  └──────────┘        │
└─────────────────────────────────────┘
```

## 📦 Project Structure

```
Banking-Application/
├── src/
│   ├── main/
│   │   ├── java/com/RRBank/banking/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── entity/              # JPA Entities
│   │   │   ├── exception/           # Exception handling
│   │   │   ├── repository/          # Data repositories
│   │   │   ├── security/            # Security & JWT
│   │   │   ├── service/             # Business logic
│   │   │   └── BankingApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/        # Flyway migrations
│   └── test/                        # Unit tests
└── pom.xml
```

## 🚀 Getting Started

### Prerequisites

1. **Java 21** (JDK 21+)
2. **Maven 3.8+**
3. **PostgreSQL 15+**
4. **Redis 7+** (optional, for caching)
5. **Apache Kafka** (optional, for event streaming)

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE rrbank;
```

2. Update `application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rrbank
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run with Maven:
```bash
cd Banking-Application
./mvnw clean install
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Running with Docker (Optional)

```bash
docker-compose up -d
```

## 🔐 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | User login |
| POST | `/api/auth/refresh` | Refresh access token |

### Account Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/accounts` | Create new account | Yes |
| GET | `/api/accounts` | Get user accounts | Yes |
| GET | `/api/accounts/{accountNumber}` | Get account details | Yes |
| PUT | `/api/accounts/{accountNumber}/status` | Update account status | Yes |

### Transactions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/transactions/transfer` | Transfer funds | Yes |
| POST | `/api/transactions/deposit` | Deposit funds | Yes |
| POST | `/api/transactions/withdraw` | Withdraw funds | Yes |
| GET | `/api/transactions/account/{accountId}` | Get transaction history | Yes |
| GET | `/api/transactions/{referenceNumber}` | Get transaction details | Yes |

## 📝 API Usage Examples

### Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass@123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john.doe",
    "password": "SecurePass@123"
  }'
```

### Create Account

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "CHECKING"
  }'
```

### Transfer Funds

```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "123456789012",
    "toAccountNumber": "987654321098",
    "amount": 500.00,
    "description": "Payment for services"
  }'
```

## 🔧 Configuration

### Key Configuration Properties

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/rrbank
spring.jpa.hibernate.ddl-auto=validate

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# JWT
jwt.secret=YOUR_SECRET_KEY
jwt.expiration=86400000
jwt.refresh-expiration=604800000
```

## 🔒 Security Features

- **JWT-based Authentication**: Secure token-based authentication
- **Role-based Authorization**: CUSTOMER, ADMIN, MANAGER, SUPPORT roles
- **Password Encryption**: BCrypt with strength 12
- **Account Locking**: Auto-lock after 5 failed login attempts
- **CORS Configuration**: Configurable cross-origin access
- **Input Validation**: Comprehensive validation on all inputs

## 📊 Monitoring & Health

### Actuator Endpoints

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`

## 🧪 Testing

Run tests with Maven:
```bash
./mvnw test
```

## 🗄️ Database Schema

### Users Table
- User authentication and profile information
- KYC verification status
- Account security features

### Accounts Table
- Multiple account types (Checking, Savings, Credit)
- Balance tracking
- Account status management

### Transactions Table
- Complete transaction history
- Support for deposits, withdrawals, transfers
- Transaction status tracking

## 📈 Features

### Core Banking Features
- ✅ User Registration & Authentication
- ✅ Multiple Account Types
- ✅ Fund Transfers
- ✅ Deposits & Withdrawals
- ✅ Transaction History
- ✅ Account Management

### Technical Features
- ✅ RESTful API Design
- ✅ JWT Security
- ✅ Database Migrations (Flyway)
- ✅ Caching (Redis)
- ✅ Event Streaming (Kafka)
- ✅ Exception Handling
- ✅ API Documentation
- ✅ Health Monitoring

## 🚧 Future Enhancements

- [ ] Bill Payment Integration
- [ ] Inter-bank Transfers
- [ ] Loan Management
- [ ] Investment Accounts
- [ ] Mobile Banking API
- [ ] Two-Factor Authentication (2FA)
- [ ] Email/SMS Notifications
- [ ] Statement Generation
- [ ] Transaction Alerts
- [ ] Credit Score Integration

## 📄 License

This project is licensed under the MIT License.

## 👥 Contributors

- RR-Bank Development Team

## 📞 Support

For support and questions, please contact: support@rrbank.com

## 🔄 Version History

- **1.0.0** - Initial release with core banking features
