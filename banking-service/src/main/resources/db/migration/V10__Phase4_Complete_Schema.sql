-- ============================================
-- V10: Comprehensive Database Schema for Phase 4
-- Create all missing tables, indexes, and constraints
-- ============================================

-- ============================================
-- TABLE: fraud_rules
-- Purpose: Define fraud detection rules
-- ============================================
CREATE TABLE IF NOT EXISTS fraud_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL,
    description TEXT,
    threshold_amount DECIMAL(15,2),
    time_window_minutes INTEGER,
    max_frequency INTEGER,
    risk_score_weight DECIMAL(5,2) DEFAULT 1.0,
    enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_threshold_positive CHECK (threshold_amount IS NULL OR threshold_amount >= 0),
    CONSTRAINT chk_time_window_positive CHECK (time_window_minutes IS NULL OR time_window_minutes > 0),
    CONSTRAINT chk_frequency_positive CHECK (max_frequency IS NULL OR max_frequency > 0),
    CONSTRAINT chk_risk_weight_range CHECK (risk_score_weight BETWEEN 0 AND 100)
);

-- ============================================
-- ADD MISSING COLUMNS TO EXISTING TABLES
-- ============================================

-- Add missing columns to users table (if not exists)
DO $$ 
BEGIN
    -- Add last_login column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='last_login') THEN
        ALTER TABLE users ADD COLUMN last_login TIMESTAMP;
    END IF;
    
    -- Add failed_login_attempts if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='failed_login_attempts') THEN
        ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
    END IF;
    
    -- Add account_locked if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='account_locked') THEN
        ALTER TABLE users ADD COLUMN account_locked BOOLEAN DEFAULT false;
    END IF;
    
    -- Add locked_until if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='locked_until') THEN
        ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
    END IF;
END $$;

-- Add missing columns to customers table
DO $$ 
BEGIN
    -- Add ssn column if not exists (encrypted)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='customers' AND column_name='ssn') THEN
        ALTER TABLE customers ADD COLUMN ssn VARCHAR(255);
    END IF;
    
    -- Add id_type if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='customers' AND column_name='id_type') THEN
        ALTER TABLE customers ADD COLUMN id_type VARCHAR(50);
    END IF;
    
    -- Add id_number if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='customers' AND column_name='id_number') THEN
        ALTER TABLE customers ADD COLUMN id_number VARCHAR(100);
    END IF;
    
    -- Add customer_segment if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='customers' AND column_name='customer_segment') THEN
        ALTER TABLE customers ADD COLUMN customer_segment VARCHAR(50) DEFAULT 'REGULAR';
    END IF;
END $$;

-- Add missing columns to accounts table
DO $$ 
BEGIN
    -- Add last_transaction_date if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='accounts' AND column_name='last_transaction_date') THEN
        ALTER TABLE accounts ADD COLUMN last_transaction_date TIMESTAMP;
    END IF;
    
    -- Add minimum_balance if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='accounts' AND column_name='minimum_balance') THEN
        ALTER TABLE accounts ADD COLUMN minimum_balance DECIMAL(15,2) DEFAULT 0.00;
    END IF;
    
    -- Add branch_code if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='accounts' AND column_name='branch_code') THEN
        ALTER TABLE accounts ADD COLUMN branch_code VARCHAR(20);
    END IF;
END $$;

-- Add missing columns to transactions table
DO $$ 
BEGIN
    -- Add fee_amount if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='transactions' AND column_name='fee_amount') THEN
        ALTER TABLE transactions ADD COLUMN fee_amount DECIMAL(15,2) DEFAULT 0.00;
    END IF;
    
    -- Add exchange_rate if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='transactions' AND column_name='exchange_rate') THEN
        ALTER TABLE transactions ADD COLUMN exchange_rate DECIMAL(10,6) DEFAULT 1.0;
    END IF;
    
    -- Add failure_reason if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='transactions' AND column_name='failure_reason') THEN
        ALTER TABLE transactions ADD COLUMN failure_reason TEXT;
    END IF;
    
    -- Add balance_before if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='transactions' AND column_name='balance_before') THEN
        ALTER TABLE transactions ADD COLUMN balance_before DECIMAL(15,2);
    END IF;
    
    -- Add balance_after if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='transactions' AND column_name='balance_after') THEN
        ALTER TABLE transactions ADD COLUMN balance_after DECIMAL(15,2);
    END IF;
END $$;

-- Add missing columns to fraud_events table
DO $$ 
BEGIN
    -- Add action_taken if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='fraud_events' AND column_name='action_taken') THEN
        ALTER TABLE fraud_events ADD COLUMN action_taken VARCHAR(50);
    END IF;
    
    -- Add ip_address if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='fraud_events' AND column_name='ip_address') THEN
        ALTER TABLE fraud_events ADD COLUMN ip_address VARCHAR(45);
    END IF;
    
    -- Add device_fingerprint if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='fraud_events' AND column_name='device_fingerprint') THEN
        ALTER TABLE fraud_events ADD COLUMN device_fingerprint VARCHAR(255);
    END IF;
    
    -- Add location if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='fraud_events' AND column_name='location') THEN
        ALTER TABLE fraud_events ADD COLUMN location VARCHAR(200);
    END IF;
END $$;

-- ============================================
-- ADDITIONAL INDEXES FOR PERFORMANCE
-- ============================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- Customers table indexes
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_kyc_status ON customers(kyc_status);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at DESC);

-- Accounts table indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_accounts_opened_at ON accounts(opened_at DESC);
CREATE INDEX IF NOT EXISTS idx_accounts_customer_status ON accounts(customer_id, status);

-- Transactions table indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_transactions_idempotency ON transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_transactions_from_date ON transactions(from_account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_to_date ON transactions(to_account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_initiated_by ON transactions(initiated_by);

-- Payments table indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_type ON payments(payment_type);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payments_scheduled ON payments(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_payments_account_status ON payments(account_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);

-- Notifications table indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notifications(sent_at DESC);

-- Fraud events indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_fraud_risk_level ON fraud_events(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_resolved ON fraud_events(resolved);
CREATE INDEX IF NOT EXISTS idx_fraud_transaction_risk ON fraud_events(transaction_id, risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_created_at ON fraud_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_resolved_by ON fraud_events(resolved_by);

-- Fraud rules indexes
CREATE INDEX IF NOT EXISTS idx_fraud_rules_enabled ON fraud_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_type ON fraud_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_priority ON fraud_rules(priority DESC);

-- Statements table indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_statements_period ON statements(statement_period);
CREATE INDEX IF NOT EXISTS idx_statements_date ON statements(statement_date DESC);
CREATE INDEX IF NOT EXISTS idx_statements_account_period ON statements(account_id, statement_period);

-- Audit logs indexes (additional to existing)
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_service ON audit_logs(service_name);
CREATE INDEX IF NOT EXISTS idx_audit_customer ON audit_logs(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_account ON audit_logs(account_id);
CREATE INDEX IF NOT EXISTS idx_audit_transaction ON audit_logs(transaction_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_date ON audit_logs(event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_ip ON audit_logs(ip_address);

-- ============================================
-- COMPOSITE INDEXES FOR COMMON QUERIES
-- ============================================

-- For fraud detection queries
CREATE INDEX IF NOT EXISTS idx_fraud_unresolved_high_risk 
ON fraud_events(resolved, risk_level, created_at DESC) 
WHERE resolved = false AND risk_level IN ('HIGH', 'CRITICAL');

-- For transaction history queries
CREATE INDEX IF NOT EXISTS idx_transactions_account_date_status 
ON transactions(from_account_id, created_at DESC, status);

-- For account statement generation
CREATE INDEX IF NOT EXISTS idx_transactions_account_period 
ON transactions(from_account_id, created_at) 
WHERE status = 'COMPLETED';

-- For payment processing
CREATE INDEX IF NOT EXISTS idx_payments_scheduled_pending 
ON payments(scheduled_date, status) 
WHERE status = 'PENDING';

-- For notification delivery
CREATE INDEX IF NOT EXISTS idx_notifications_pending 
ON notifications(status, created_at) 
WHERE status = 'PENDING';

-- ============================================
-- CONSTRAINTS AND CHECKS
-- ============================================

-- Add check constraints if not exist
DO $$
BEGIN
    -- Transaction amount must be positive
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_transaction_amount_positive') THEN
        ALTER TABLE transactions ADD CONSTRAINT chk_transaction_amount_positive 
        CHECK (amount > 0);
    END IF;
    
    -- Payment amount must be positive
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payment_amount_positive') THEN
        ALTER TABLE payments ADD CONSTRAINT chk_payment_amount_positive 
        CHECK (amount > 0);
    END IF;
    
    -- Account balance constraints
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_account_minimum_balance') THEN
        ALTER TABLE accounts ADD CONSTRAINT chk_account_minimum_balance 
        CHECK (minimum_balance >= 0);
    END IF;
    
    -- Fraud risk score range
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_fraud_risk_score') THEN
        ALTER TABLE fraud_events ADD CONSTRAINT chk_fraud_risk_score 
        CHECK (risk_score >= 0 AND risk_score <= 100);
    END IF;
END $$;

-- ============================================
-- FUNCTIONS AND TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all tables with updated_at column
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN 
        SELECT table_name 
        FROM information_schema.columns 
        WHERE column_name = 'updated_at' 
        AND table_schema = 'public'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_update_%s_updated_at ON %I', t, t);
        EXECUTE format('CREATE TRIGGER trg_update_%s_updated_at 
                       BEFORE UPDATE ON %I 
                       FOR EACH ROW 
                       EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END $$;

-- Function to generate account number
CREATE OR REPLACE FUNCTION generate_account_number()
RETURNS VARCHAR AS $$
DECLARE
    new_account_number VARCHAR(20);
BEGIN
    new_account_number := 'ACC' || LPAD(nextval('account_number_seq')::TEXT, 14, '0');
    RETURN new_account_number;
END;
$$ LANGUAGE plpgsql;

-- Create sequence for account numbers if not exists
CREATE SEQUENCE IF NOT EXISTS account_number_seq START WITH 1000000000;

-- Function to generate transaction reference
CREATE OR REPLACE FUNCTION generate_transaction_reference()
RETURNS VARCHAR AS $$
DECLARE
    new_ref VARCHAR(50);
BEGIN
    new_ref := 'TXN' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD(nextval('transaction_ref_seq')::TEXT, 10, '0');
    RETURN new_ref;
END;
$$ LANGUAGE plpgsql;

-- Create sequence for transaction references if not exists
CREATE SEQUENCE IF NOT EXISTS transaction_ref_seq START WITH 1;

-- Function to generate payment reference
CREATE OR REPLACE FUNCTION generate_payment_reference()
RETURNS VARCHAR AS $$
DECLARE
    new_ref VARCHAR(50);
BEGIN
    new_ref := 'PAY' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD(nextval('payment_ref_seq')::TEXT, 10, '0');
    RETURN new_ref;
END;
$$ LANGUAGE plpgsql;

-- Create sequence for payment references if not exists
CREATE SEQUENCE IF NOT EXISTS payment_ref_seq START WITH 1;

-- ============================================
-- VIEWS FOR COMMON QUERIES
-- ============================================

-- View: Account Summary with Customer Details
CREATE OR REPLACE VIEW vw_account_summary AS
SELECT 
    a.id as account_id,
    a.account_number,
    a.account_type,
    a.balance,
    a.currency,
    a.status as account_status,
    c.id as customer_id,
    c.first_name,
    c.last_name,
    c.email,
    c.phone,
    c.kyc_status,
    u.username,
    u.role,
    a.opened_at,
    a.last_transaction_date
FROM accounts a
INNER JOIN customers c ON a.customer_id = c.id
INNER JOIN users u ON c.user_id = u.id;

-- View: Recent Transactions (Last 30 days)
CREATE OR REPLACE VIEW vw_recent_transactions AS
SELECT 
    t.id,
    t.transaction_reference,
    t.transaction_type,
    t.amount,
    t.currency,
    t.status,
    t.description,
    fa.account_number as from_account,
    ta.account_number as to_account,
    fc.first_name || ' ' || fc.last_name as from_customer,
    tc.first_name || ' ' || tc.last_name as to_customer,
    t.created_at,
    t.completed_at
FROM transactions t
LEFT JOIN accounts fa ON t.from_account_id = fa.id
LEFT JOIN accounts ta ON t.to_account_id = ta.id
LEFT JOIN customers fc ON fa.customer_id = fc.id
LEFT JOIN customers tc ON ta.customer_id = tc.id
WHERE t.created_at >= CURRENT_DATE - INTERVAL '30 days';

-- View: High Risk Fraud Events
CREATE OR REPLACE VIEW vw_high_risk_fraud AS
SELECT 
    fe.id,
    fe.risk_score,
    fe.risk_level,
    fe.flagged_reason,
    fe.resolved,
    t.transaction_reference,
    t.amount,
    t.transaction_type,
    a.account_number,
    c.first_name || ' ' || c.last_name as customer_name,
    fe.created_at
FROM fraud_events fe
INNER JOIN transactions t ON fe.transaction_id = t.id
INNER JOIN accounts a ON t.from_account_id = a.id
INNER JOIN customers c ON a.customer_id = c.id
WHERE fe.risk_level IN ('HIGH', 'CRITICAL') AND fe.resolved = false;

-- View: Pending Notifications
CREATE OR REPLACE VIEW vw_pending_notifications AS
SELECT 
    n.id,
    n.notification_type,
    n.channel,
    n.subject,
    n.message,
    u.username,
    u.email,
    c.phone,
    n.created_at
FROM notifications n
INNER JOIN users u ON n.user_id = u.id
LEFT JOIN customers c ON u.id = c.user_id
WHERE n.status = 'PENDING'
ORDER BY n.created_at ASC;

-- ============================================
-- MATERIALIZED VIEWS FOR ANALYTICS
-- ============================================

-- Materialized View: Daily Transaction Summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_transaction_summary AS
SELECT 
    DATE(created_at) as transaction_date,
    transaction_type,
    status,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount,
    MIN(amount) as min_amount,
    MAX(amount) as max_amount
FROM transactions
GROUP BY DATE(created_at), transaction_type, status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_txn_summary 
ON mv_daily_transaction_summary(transaction_date, transaction_type, status);

-- Materialized View: Customer Account Statistics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_customer_statistics AS
SELECT 
    c.id as customer_id,
    c.first_name || ' ' || c.last_name as customer_name,
    c.kyc_status,
    COUNT(DISTINCT a.id) as total_accounts,
    SUM(CASE WHEN a.status = 'ACTIVE' THEN 1 ELSE 0 END) as active_accounts,
    SUM(a.balance) as total_balance,
    COUNT(DISTINCT t.id) as total_transactions,
    MAX(t.created_at) as last_transaction_date
FROM customers c
LEFT JOIN accounts a ON c.id = a.customer_id
LEFT JOIN transactions t ON (a.id = t.from_account_id OR a.id = t.to_account_id)
GROUP BY c.id, c.first_name, c.last_name, c.kyc_status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_stats 
ON mv_customer_statistics(customer_id);

-- ============================================
-- INITIAL FRAUD RULES DATA
-- ============================================

INSERT INTO fraud_rules (rule_name, rule_type, description, threshold_amount, time_window_minutes, max_frequency, risk_score_weight, enabled, priority)
VALUES 
    ('High Value Transaction', 'AMOUNT_THRESHOLD', 'Flag transactions above $10,000', 10000.00, NULL, NULL, 10.0, true, 1),
    ('Rapid Transactions', 'FREQUENCY', 'Multiple transactions in short time', NULL, 5, 5, 8.0, true, 2),
    ('Unusual Location', 'LOCATION', 'Transaction from unusual location', NULL, NULL, NULL, 7.0, true, 3),
    ('Night Transaction', 'TIME', 'Large transaction during night hours', 5000.00, NULL, NULL, 5.0, true, 4),
    ('Duplicate Transaction', 'DUPLICATE', 'Same amount to same recipient', NULL, 60, 2, 9.0, true, 5),
    ('Round Amount', 'PATTERN', 'Round amount transactions', NULL, NULL, NULL, 3.0, true, 6),
    ('Cross Border', 'GEOGRAPHY', 'International transactions', NULL, NULL, NULL, 6.0, true, 7),
    ('Velocity Check', 'VELOCITY', 'Daily transaction velocity exceeded', 50000.00, 1440, 10, 8.0, true, 8)
ON CONFLICT (rule_name) DO NOTHING;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE fraud_rules IS 'Configuration for fraud detection rules';
COMMENT ON COLUMN fraud_rules.rule_type IS 'Type of fraud rule: AMOUNT_THRESHOLD, FREQUENCY, LOCATION, TIME, etc.';
COMMENT ON COLUMN fraud_rules.risk_score_weight IS 'Weight applied to risk score calculation (0-100)';
COMMENT ON COLUMN fraud_rules.priority IS 'Rule execution priority (lower number = higher priority)';

-- ============================================
-- GRANT PERMISSIONS (OPTIONAL)
-- ============================================

-- Grant read access to application role
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO banking_app_role;
-- GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO banking_app_role;

-- ============================================
-- VERIFY INSTALLATION
-- ============================================

-- Count all tables
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    
    RAISE NOTICE 'Total tables created: %', table_count;
END $$;

COMMENT ON SCHEMA public IS 'RR-Bank Banking Application - Phase 4 Database Schema - Completed';
