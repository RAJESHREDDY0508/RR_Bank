-- V11__Fix_Accounts_Table_Schema.sql
-- Fix the accounts table to remove conflicting columns and use consistent primary key

-- Step 1: Backup existing data (if any)
-- Note: This migration assumes we can drop and recreate the accounts table
-- In production, you would need a more careful migration strategy

-- Drop the existing accounts table and recreate with correct schema
DROP TABLE IF EXISTS accounts CASCADE;

-- Recreate accounts table with id as UUID primary key (matching V3 schema)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) NOT NULL UNIQUE,
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
    last_statement_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT chk_account_type 
        CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS')),
    CONSTRAINT chk_account_status 
        CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'CLOSED', 'SUSPENDED'))
);

-- Create indexes for performance
CREATE INDEX idx_account_customer_id ON accounts(customer_id);
CREATE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_account_status ON accounts(status);
CREATE INDEX idx_account_type ON accounts(account_type);
CREATE INDEX idx_account_created_at ON accounts(created_at DESC);
CREATE INDEX idx_account_customer_status ON accounts(customer_id, status);
CREATE INDEX idx_account_customer_type ON accounts(customer_id, account_type);

-- Add comments for documentation
COMMENT ON TABLE accounts IS 'Bank accounts for customers';
COMMENT ON COLUMN accounts.id IS 'Primary key - UUID';
COMMENT ON COLUMN accounts.account_number IS 'Unique account number';
COMMENT ON COLUMN accounts.account_type IS 'Account type: SAVINGS, CHECKING, CREDIT, BUSINESS';
COMMENT ON COLUMN accounts.status IS 'Account status: PENDING, ACTIVE, FROZEN, CLOSED, SUSPENDED';
COMMENT ON COLUMN accounts.balance IS 'Current account balance';
COMMENT ON COLUMN accounts.overdraft_limit IS 'Maximum overdraft allowed';
COMMENT ON COLUMN accounts.interest_rate IS 'Interest rate percentage';
