-- ============================================================
-- V14__Add_Ledger_And_Limits_System.sql
-- Ledger-based accounting, Transaction limits, and Enhanced Auth
-- ============================================================

-- ============================================================
-- TABLE: ledger_entries (IMMUTABLE - Core banking ledger)
-- Purpose: Double-entry bookkeeping for all financial transactions
-- ============================================================
CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    transaction_id UUID,
    entry_type VARCHAR(10) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    running_balance DECIMAL(19, 4),
    reference_id VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created_at ON ledger_entries(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_reference ON ledger_entries(reference_id);

-- ============================================================
-- TABLE: idempotency_records (Prevent duplicate transactions)
-- ============================================================
CREATE TABLE IF NOT EXISTS idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_data JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_records(expires_at);

-- ============================================================
-- TABLE: transaction_limits (User transaction limits)
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    limit_type VARCHAR(30) NOT NULL,
    daily_limit DECIMAL(19, 4) NOT NULL DEFAULT 10000.00,
    per_transaction_limit DECIMAL(19, 4) NOT NULL DEFAULT 5000.00,
    monthly_limit DECIMAL(19, 4) DEFAULT 100000.00,
    remaining_daily DECIMAL(19, 4),
    remaining_monthly DECIMAL(19, 4),
    last_reset_date DATE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_limit_type UNIQUE (user_id, limit_type),
    CONSTRAINT chk_limit_type CHECK (limit_type IN ('TRANSFER', 'WITHDRAWAL', 'DEPOSIT', 'PAYMENT', 'ALL'))
);

CREATE INDEX IF NOT EXISTS idx_limits_user_id ON transaction_limits(user_id);
CREATE INDEX IF NOT EXISTS idx_limits_type ON transaction_limits(limit_type);

-- ============================================================
-- TABLE: velocity_checks (Transaction velocity monitoring)
-- ============================================================
CREATE TABLE IF NOT EXISTS velocity_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    account_id UUID,
    check_type VARCHAR(30) NOT NULL,
    window_minutes INTEGER NOT NULL DEFAULT 60,
    max_count INTEGER NOT NULL DEFAULT 10,
    current_count INTEGER DEFAULT 0,
    window_start TIMESTAMP,
    last_transaction_at TIMESTAMP,
    blocked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_velocity_type CHECK (check_type IN ('TRANSACTION_COUNT', 'AMOUNT_SUM', 'FAILED_COUNT'))
);

CREATE INDEX IF NOT EXISTS idx_velocity_user ON velocity_checks(user_id);
CREATE INDEX IF NOT EXISTS idx_velocity_account ON velocity_checks(account_id);

-- ============================================================
-- TABLE: password_reset_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    
    CONSTRAINT uk_reset_token UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_reset_user ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_reset_token ON password_reset_tokens(token_hash);

-- ============================================================
-- TABLE: email_verifications
-- ============================================================
CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    email VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_email_verify_token UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_email_verify_user ON email_verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verify_token ON email_verifications(token_hash);

-- ============================================================
-- TABLE: refresh_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_refresh_token UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token ON refresh_tokens(token_hash);

-- ============================================================
-- TABLE: account_requests (Admin approval workflow)
-- ============================================================
CREATE TABLE IF NOT EXISTS account_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    initial_deposit DECIMAL(19, 4) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_notes TEXT,
    admin_notes TEXT,
    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP,
    account_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_request_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_request_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS'))
);

CREATE INDEX IF NOT EXISTS idx_request_user ON account_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_request_status ON account_requests(status);
CREATE INDEX IF NOT EXISTS idx_request_created ON account_requests(created_at DESC);

-- ============================================================
-- TABLE: login_history
-- ============================================================
CREATE TABLE IF NOT EXISTS login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36),
    username VARCHAR(100),
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    location VARCHAR(200),
    failure_reason VARCHAR(100),
    mfa_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_login_user ON login_history(user_id);
CREATE INDEX IF NOT EXISTS idx_login_created ON login_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_ip ON login_history(ip_address);

-- ============================================================
-- FUNCTION: Calculate account balance from ledger
-- ============================================================
CREATE OR REPLACE FUNCTION calculate_ledger_balance(p_account_id UUID)
RETURNS DECIMAL(19, 4) AS $$
DECLARE
    v_balance DECIMAL(19, 4);
BEGIN
    SELECT COALESCE(
        SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END),
        0
    )
    INTO v_balance
    FROM ledger_entries
    WHERE account_id = p_account_id;
    
    RETURN v_balance;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Add email_verified column to users
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'email_verified'
    ) THEN
        ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- ============================================================
-- Update users status constraint
-- ============================================================
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
ALTER TABLE users ADD CONSTRAINT chk_users_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED', 'PENDING_VERIFICATION'));

-- ============================================================
-- Add missing columns to transactions
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'fee') THEN
        ALTER TABLE transactions ADD COLUMN fee DECIMAL(19, 4) DEFAULT 0.0000;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'from_account_number') THEN
        ALTER TABLE transactions ADD COLUMN from_account_number VARCHAR(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'to_account_number') THEN
        ALTER TABLE transactions ADD COLUMN to_account_number VARCHAR(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'merchant_name') THEN
        ALTER TABLE transactions ADD COLUMN merchant_name VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'transaction_date') THEN
        ALTER TABLE transactions ADD COLUMN transaction_date TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'completed_date') THEN
        ALTER TABLE transactions ADD COLUMN completed_date TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'failed_at') THEN
        ALTER TABLE transactions ADD COLUMN failed_at TIMESTAMP;
    END IF;
END $$;

-- ============================================================
-- Insert default limits for existing users
-- ============================================================
INSERT INTO transaction_limits (user_id, limit_type, daily_limit, per_transaction_limit, monthly_limit, enabled)
SELECT user_id, 'ALL', 10000.00, 5000.00, 100000.00, TRUE
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_limits tl 
    WHERE tl.user_id = users.user_id AND tl.limit_type = 'ALL'
)
ON CONFLICT DO NOTHING;

COMMENT ON SCHEMA public IS 'RR-Bank Banking Application Database Schema v1.4 - Ledger & Limits System';
