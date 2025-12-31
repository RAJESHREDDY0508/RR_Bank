-- ============================================================
-- RR-Bank Database Migrations
-- Run this in PostgreSQL after creating the database
-- ============================================================

-- Create database (run separately as postgres superuser)
-- CREATE DATABASE rrbank;

-- Connect to rrbank database first, then run the rest

-- ============================================================
-- Phase 3: Payees Table
-- ============================================================
CREATE TABLE IF NOT EXISTS payees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    payee_name VARCHAR(200) NOT NULL,
    payee_account_number VARCHAR(50) NOT NULL,
    payee_bank_code VARCHAR(20),
    payee_bank_name VARCHAR(100),
    payee_routing_number VARCHAR(20),
    payee_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    verified_by UUID,
    transfer_limit DECIMAL(19,4),
    daily_limit DECIMAL(19,4),
    email VARCHAR(255),
    phone VARCHAR(20),
    notes TEXT,
    is_internal BOOLEAN DEFAULT FALSE,
    internal_account_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payees_customer_id ON payees(customer_id);
CREATE INDEX IF NOT EXISTS idx_payees_account_number ON payees(payee_account_number);
CREATE INDEX IF NOT EXISTS idx_payees_status ON payees(status);

-- ============================================================
-- Phase 3: Holds Table
-- ============================================================
CREATE TABLE IF NOT EXISTS holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    transaction_id UUID,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    hold_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reason TEXT,
    reference VARCHAR(100),
    expires_at TIMESTAMP,
    released_at TIMESTAMP,
    released_by UUID,
    release_reason TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_holds_account_id ON holds(account_id);
CREATE INDEX IF NOT EXISTS idx_holds_status ON holds(status);
CREATE INDEX IF NOT EXISTS idx_holds_expires_at ON holds(expires_at);

-- ============================================================
-- Phase 3: Fraud Cases Table
-- ============================================================
CREATE TABLE IF NOT EXISTS fraud_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(50) NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    customer_id UUID,
    transaction_id UUID,
    amount DECIMAL(19,4),
    case_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    risk_score INTEGER,
    description TEXT,
    detection_method VARCHAR(100),
    fraud_indicators TEXT,
    assigned_to UUID,
    assigned_at TIMESTAMP,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    resolution TEXT,
    resolution_type VARCHAR(30),
    account_action_taken VARCHAR(50),
    transaction_reversed BOOLEAN DEFAULT FALSE,
    hold_id UUID,
    due_date TIMESTAMP,
    escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMP,
    escalated_to UUID,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_cases_account_id ON fraud_cases(account_id);
CREATE INDEX IF NOT EXISTS idx_fraud_cases_transaction_id ON fraud_cases(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_cases_status ON fraud_cases(status);
CREATE INDEX IF NOT EXISTS idx_fraud_cases_priority ON fraud_cases(priority);

-- ============================================================
-- Phase 3: Scheduled Payments Table
-- ============================================================
CREATE TABLE IF NOT EXISTS scheduled_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_reference VARCHAR(50) NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    payee_id UUID,
    to_account_id UUID,
    to_account_number VARCHAR(50),
    payee_name VARCHAR(200),
    payment_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_execution_date DATE,
    last_execution_date DATE,
    execution_count INTEGER DEFAULT 0,
    max_executions INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    last_execution_status VARCHAR(30),
    last_execution_error TEXT,
    consecutive_failures INTEGER DEFAULT 0,
    max_consecutive_failures INTEGER DEFAULT 3,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scheduled_account_id ON scheduled_payments(account_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_next_execution ON scheduled_payments(next_execution_date);
CREATE INDEX IF NOT EXISTS idx_scheduled_status ON scheduled_payments(status);

-- ============================================================
-- Phase 3: Disputes Table
-- ============================================================
CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    disputed_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    dispute_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reason TEXT NOT NULL,
    customer_description TEXT,
    merchant_name VARCHAR(200),
    transaction_date TIMESTAMP,
    provisional_credit_issued BOOLEAN DEFAULT FALSE,
    provisional_credit_amount DECIMAL(19,4),
    provisional_credit_date TIMESTAMP,
    assigned_to UUID,
    assigned_at TIMESTAMP,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    resolution VARCHAR(30),
    resolution_notes TEXT,
    refund_amount DECIMAL(19,4),
    refund_transaction_id UUID,
    supporting_documents TEXT,
    due_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_disputes_transaction_id ON disputes(transaction_id);
CREATE INDEX IF NOT EXISTS idx_disputes_account_id ON disputes(account_id);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);

-- ============================================================
-- Phase 2: Add idempotency_key to payments table
-- ============================================================
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'payments' AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;
    END IF;
END $$;

-- ============================================================
-- Verify all tables exist
-- ============================================================
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name IN ('payees', 'holds', 'fraud_cases', 'scheduled_payments', 'disputes');
    
    IF table_count = 5 THEN
        RAISE NOTICE 'All Phase 3 tables created successfully!';
    ELSE
        RAISE WARNING 'Some tables may be missing. Found % of 5 tables.', table_count;
    END IF;
END $$;

SELECT 'Migration completed successfully!' as status;
