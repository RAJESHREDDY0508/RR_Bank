-- ============================================================
-- V1__init.sql - RR-Bank Complete Database Schema
-- ============================================================
-- This migration creates all tables for the RR-Bank banking system.
-- Run this on a fresh database to create the complete schema.
-- ============================================================

-- ============================================================
-- TABLE: users
-- Purpose: Authentication and user account management
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(50),
    state VARCHAR(50),
    postal_code VARCHAR(10),
    country VARCHAR(50),
    date_of_birth TIMESTAMP,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    kyc_verified BOOLEAN DEFAULT FALSE,
    kyc_verification_date TIMESTAMP,
    last_login TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN', 'TELLER', 'MANAGER')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

COMMENT ON TABLE users IS 'User accounts for authentication and authorization';
COMMENT ON COLUMN users.user_id IS 'Primary key - UUID string';
COMMENT ON COLUMN users.role IS 'User role: CUSTOMER, ADMIN, TELLER, MANAGER';
COMMENT ON COLUMN users.status IS 'Account status: ACTIVE, INACTIVE, SUSPENDED, LOCKED';

-- ============================================================
-- TABLE: customers
-- Purpose: Customer profiles with KYC information
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code VARCHAR(10),
    country VARCHAR(50),
    ssn VARCHAR(255),
    id_type VARCHAR(50),
    id_number VARCHAR(100),
    customer_segment VARCHAR(50) DEFAULT 'REGULAR',
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP,
    kyc_document_type VARCHAR(50),
    kyc_document_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_customers_user_id UNIQUE (user_id),
    CONSTRAINT chk_customers_kyc_status CHECK (kyc_status IN ('PENDING', 'IN_PROGRESS', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_customers_segment CHECK (customer_segment IN ('REGULAR', 'PREMIUM', 'VIP', 'CORPORATE'))
);

CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_kyc_status ON customers(kyc_status);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at DESC);

COMMENT ON TABLE customers IS 'Customer profiles with KYC information';
COMMENT ON COLUMN customers.kyc_status IS 'KYC status: PENDING, IN_PROGRESS, VERIFIED, REJECTED, EXPIRED';

-- ============================================================
-- TABLE: accounts
-- Purpose: Bank accounts (Savings, Checking, Credit, Business)
-- ============================================================
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) NOT NULL,
    customer_id UUID NOT NULL,
    user_id VARCHAR(36),
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    minimum_balance DECIMAL(19, 4) DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    overdraft_limit DECIMAL(19, 4) DEFAULT 0.0000,
    interest_rate DECIMAL(5, 4) DEFAULT 0.0000,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    last_transaction_date TIMESTAMP,
    last_statement_date TIMESTAMP,
    branch_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'CLOSED', 'SUSPENDED')),
    CONSTRAINT chk_accounts_balance CHECK (balance >= (0 - overdraft_limit))
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_accounts_customer_status ON accounts(customer_id, status);

COMMENT ON TABLE accounts IS 'Bank accounts for customers';
COMMENT ON COLUMN accounts.account_type IS 'Account type: SAVINGS, CHECKING, CREDIT, BUSINESS';
COMMENT ON COLUMN accounts.status IS 'Account status: PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED';

-- ============================================================
-- TABLE: transactions
-- Purpose: Financial transactions (deposits, withdrawals, transfers)
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_reference VARCHAR(50) NOT NULL,
    from_account_id UUID,
    to_account_id UUID,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    category VARCHAR(50),
    idempotency_key VARCHAR(100),
    fee_amount DECIMAL(19, 4) DEFAULT 0.0000,
    exchange_rate DECIMAL(10, 6) DEFAULT 1.000000,
    balance_before DECIMAL(19, 4),
    balance_after DECIMAL(19, 4),
    failure_reason TEXT,
    initiated_by VARCHAR(36),
    approved_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_transactions_reference UNIQUE (transaction_reference),
    CONSTRAINT uk_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_transactions_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT', 'FEE', 'INTEREST', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED')),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_to_account ON transactions(to_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_idempotency ON transactions(idempotency_key);

COMMENT ON TABLE transactions IS 'Financial transactions';
COMMENT ON COLUMN transactions.transaction_type IS 'Type: DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, FEE, INTEREST, REFUND, ADJUSTMENT';

-- ============================================================
-- TABLE: payments
-- Purpose: Bill payments and scheduled payments
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_reference VARCHAR(50) NOT NULL,
    account_id UUID NOT NULL,
    payee_name VARCHAR(200) NOT NULL,
    payee_account_number VARCHAR(50),
    payee_bank_code VARCHAR(20),
    payee_bank_name VARCHAR(100),
    payment_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_date DATE,
    execution_date TIMESTAMP,
    description TEXT,
    recurring BOOLEAN DEFAULT FALSE,
    recurring_frequency VARCHAR(20),
    recurring_end_date DATE,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT fk_payments_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_payments_type CHECK (payment_type IN ('BILL_PAYMENT', 'WIRE_TRANSFER', 'ACH', 'INTERNAL', 'EXTERNAL')),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SCHEDULED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payments_reference ON payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payments_account ON payments(account_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_scheduled ON payments(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);

COMMENT ON TABLE payments IS 'Bill payments and scheduled payments';

-- ============================================================
-- TABLE: notifications
-- Purpose: User notifications (email, SMS, push)
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(10) DEFAULT 'NORMAL',
    reference_type VARCHAR(50),
    reference_id VARCHAR(36),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_notifications_type CHECK (notification_type IN ('TRANSACTION', 'SECURITY', 'ACCOUNT', 'MARKETING', 'SYSTEM', 'ALERT')),
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED')),
    CONSTRAINT chk_notifications_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

COMMENT ON TABLE notifications IS 'User notifications';

-- ============================================================
-- TABLE: fraud_events
-- Purpose: Fraud detection events and alerts
-- ============================================================
CREATE TABLE IF NOT EXISTS fraud_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID,
    account_id UUID,
    customer_id UUID,
    event_type VARCHAR(50) NOT NULL,
    risk_score DECIMAL(5, 2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    flagged_reason TEXT NOT NULL,
    details JSONB,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(36),
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    action_taken VARCHAR(50),
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    location VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_fraud_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_fraud_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_fraud_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT chk_fraud_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_fraud_risk_score CHECK (risk_score >= 0 AND risk_score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_fraud_transaction ON fraud_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_account ON fraud_events(account_id);
CREATE INDEX IF NOT EXISTS idx_fraud_risk_level ON fraud_events(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_resolved ON fraud_events(resolved);
CREATE INDEX IF NOT EXISTS idx_fraud_created_at ON fraud_events(created_at DESC);

COMMENT ON TABLE fraud_events IS 'Fraud detection events and alerts';

-- ============================================================
-- TABLE: fraud_rules
-- Purpose: Configurable fraud detection rules
-- ============================================================
CREATE TABLE IF NOT EXISTS fraud_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    description TEXT,
    threshold_amount DECIMAL(19, 4),
    time_window_minutes INTEGER,
    max_frequency INTEGER,
    risk_score_weight DECIMAL(5, 2) DEFAULT 1.00,
    enabled BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_fraud_rules_name UNIQUE (rule_name),
    CONSTRAINT chk_fraud_rules_type CHECK (rule_type IN ('AMOUNT_THRESHOLD', 'FREQUENCY', 'LOCATION', 'TIME', 'DUPLICATE', 'PATTERN', 'GEOGRAPHY', 'VELOCITY')),
    CONSTRAINT chk_fraud_rules_weight CHECK (risk_score_weight >= 0 AND risk_score_weight <= 100)
);

CREATE INDEX IF NOT EXISTS idx_fraud_rules_enabled ON fraud_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_type ON fraud_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_priority ON fraud_rules(priority DESC);

COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';

-- ============================================================
-- TABLE: statements
-- Purpose: Account statements
-- ============================================================
CREATE TABLE IF NOT EXISTS statements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    statement_period VARCHAR(7) NOT NULL,
    statement_date DATE NOT NULL,
    opening_balance DECIMAL(19, 4) NOT NULL,
    closing_balance DECIMAL(19, 4) NOT NULL,
    total_credits DECIMAL(19, 4) DEFAULT 0.0000,
    total_debits DECIMAL(19, 4) DEFAULT 0.0000,
    transaction_count INTEGER DEFAULT 0,
    file_path VARCHAR(500),
    generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_statements_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT uk_statements_account_period UNIQUE (account_id, statement_period)
);

CREATE INDEX IF NOT EXISTS idx_statements_account ON statements(account_id);
CREATE INDEX IF NOT EXISTS idx_statements_period ON statements(statement_period);
CREATE INDEX IF NOT EXISTS idx_statements_date ON statements(statement_date DESC);

COMMENT ON TABLE statements IS 'Account statements';

-- ============================================================
-- TABLE: audit_logs
-- Purpose: System audit trail for compliance
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    service_name VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    customer_id UUID,
    account_id UUID,
    transaction_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(36),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(36),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL'))
);

CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_service ON audit_logs(service_name);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_customer ON audit_logs(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_account ON audit_logs(account_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at DESC);

COMMENT ON TABLE audit_logs IS 'System audit trail for compliance';

-- ============================================================
-- INITIAL DATA: Default fraud rules
-- ============================================================
INSERT INTO fraud_rules (id, rule_name, rule_type, description, threshold_amount, time_window_minutes, max_frequency, risk_score_weight, enabled, priority)
VALUES 
    (gen_random_uuid(), 'High Value Transaction', 'AMOUNT_THRESHOLD', 'Flag transactions above $10,000', 10000.00, NULL, NULL, 10.0, TRUE, 1),
    (gen_random_uuid(), 'Rapid Transactions', 'FREQUENCY', 'Multiple transactions in short time', NULL, 5, 5, 8.0, TRUE, 2),
    (gen_random_uuid(), 'Night Transaction', 'TIME', 'Large transaction during night hours', 5000.00, NULL, NULL, 5.0, TRUE, 3),
    (gen_random_uuid(), 'Velocity Check', 'VELOCITY', 'Daily transaction velocity exceeded', 50000.00, 1440, 10, 8.0, TRUE, 4)
ON CONFLICT (rule_name) DO NOTHING;

-- ============================================================
-- FUNCTIONS: Auto-update timestamps
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at column
DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN 
        SELECT table_name 
        FROM information_schema.columns 
        WHERE column_name = 'updated_at' 
        AND table_schema = 'public'
        AND table_name NOT IN ('audit_logs')
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_update_%s_updated_at ON %I', t, t);
        EXECUTE format('CREATE TRIGGER trg_update_%s_updated_at 
                       BEFORE UPDATE ON %I 
                       FOR EACH ROW 
                       EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END $$;

-- ============================================================
-- SCHEMA COMPLETE
-- ============================================================
COMMENT ON SCHEMA public IS 'RR-Bank Banking Application Database Schema v1.0';
