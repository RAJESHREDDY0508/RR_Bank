-- ============================================================
-- RR-Bank Microservices - Database Initialization
-- Creates separate databases for each microservice
-- ============================================================

-- Create databases for each service
CREATE DATABASE auth_db;
CREATE DATABASE customer_db;
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;
CREATE DATABASE fraud_db;
CREATE DATABASE audit_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE customer_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE account_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE transaction_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE ledger_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE fraud_db TO rrbank;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO rrbank;
