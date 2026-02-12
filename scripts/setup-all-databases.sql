-- RR-Bank Complete Database Setup
-- Run this with: psql -U postgres -f setup-all-databases.sql

-- Create all databases needed for RR-Bank
CREATE DATABASE admin_db;
CREATE DATABASE auth_db;
CREATE DATABASE customer_db;
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;
CREATE DATABASE fraud_db;
CREATE DATABASE audit_db;

-- Confirm databases created
\l
