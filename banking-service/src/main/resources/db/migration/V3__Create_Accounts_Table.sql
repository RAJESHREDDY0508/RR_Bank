-- Create Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    overdraft_limit DECIMAL(15,2) DEFAULT 0.00,
    interest_rate DECIMAL(5,2),
    opened_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT chk_balance CHECK (balance >= 0 - overdraft_limit),
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED', 'SUSPENDED'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_account_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_account_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_account_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_account_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_account_created_at ON accounts(created_at DESC);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_account_customer_status ON accounts(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_account_customer_type ON accounts(customer_id, account_type);

-- Add comments for documentation
COMMENT ON TABLE accounts IS 'Bank accounts for customers';
COMMENT ON COLUMN accounts.account_number IS 'Unique account number';
COMMENT ON COLUMN accounts.account_type IS 'Account type: SAVINGS, CHECKING, CREDIT, BUSINESS';
COMMENT ON COLUMN accounts.status IS 'Account status: ACTIVE, FROZEN, CLOSED, SUSPENDED';
COMMENT ON COLUMN accounts.balance IS 'Current account balance';
COMMENT ON COLUMN accounts.overdraft_limit IS 'Maximum overdraft allowed';
COMMENT ON COLUMN accounts.interest_rate IS 'Interest rate percentage';
