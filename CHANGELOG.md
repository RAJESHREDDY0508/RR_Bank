# Changelog

All notable changes to the RR-Bank platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.1.0] - 2026-01

### Added
- Admin Console with full RBAC (Super Admin, Admin User, Audit Officer, Fraud Analyst)
- KYC compliance workflow with admin approval/rejection pipeline
- KYC status banner on customer dashboard with transaction gating
- Admin Service (port 8089) for dashboard metrics and system administration
- Responsive table and filter components in Admin Console
- Fraud alerts management page in Admin Console
- Audit log viewer with export capability
- Account statistics endpoint for admin dashboard
- Transaction statistics endpoint for admin reporting

### Changed
- Auth Service now publishes `user-created` Kafka event on registration
- Customer Service listens for `user-created` events to auto-provision customer records
- Account Service delegates balance queries to Ledger Service
- Admin Console API client handles `ApiResponse` wrapper format
- Improved error handling across all admin API endpoints with default fallbacks

### Fixed
- KYC endpoints returning 500 errors due to missing null safety
- MUI Tooltip warnings on disabled buttons (wrapped in span elements)
- Account Requests page failing to load due to API response wrapper mismatch
- Docker DNS resolution failures during Maven builds
- Customer count synchronization between Auth and Customer services

---

## [1.0.0] - 2026-01

### Added
- Initial release of the RR-Bank microservices platform
- Auth Service with JWT-based authentication and registration
- Customer Service with profile management
- Account Service with CRUD operations (Savings, Checking)
- Transaction Service with SAGA orchestration (deposit, withdraw, transfer)
- Ledger Service as immutable financial source of truth
- Fraud Service with daily limits, velocity checks, and risk scoring
- Notification Service with Kafka-driven event processing
- Audit Service for immutable compliance logging
- API Gateway with JWT validation, routing, and CORS
- Customer Portal (React) for banking operations
- Docker Compose orchestration for full-stack local deployment
- PostgreSQL per-service database isolation
- Redis caching for balance lookups
- Apache Kafka event streaming infrastructure
- Kubernetes deployment manifests for Oracle Cloud (OKE)
- Postman collection for API testing
