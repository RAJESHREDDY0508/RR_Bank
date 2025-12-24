-- Create Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_reference VARCHAR(50) NOT NULL UNIQUE,
    from_account_id UUID,
    to_account_id UUID,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    idempotency_key VARCHAR(100) UNIQUE,
    initiated_by UUID,
    failure_reason TEXT,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_transaction_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_initiator FOREIGN KEY (initiated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND', 'FEE', 'INTEREST')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REVERSED', 'CANCELLED'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_transaction_from_account ON transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_to_account ON transactions(to_account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_reference ON transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_transaction_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transaction_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_idempotency_key ON transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_transaction_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transaction_initiated_by ON transactions(initiated_by);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_transaction_account_date ON transactions(from_account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_account_status ON transactions(from_account_id, status);
CREATE INDEX IF NOT EXISTS idx_transaction_status_date ON transactions(status, created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE transactions IS 'Money transfers and transactions';
COMMENT ON COLUMN transactions.transaction_reference IS 'Unique transaction reference number';
COMMENT ON COLUMN transactions.transaction_type IS 'Type: TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REFUND, FEE, INTEREST';
COMMENT ON COLUMN transactions.status IS 'Status: PENDING, PROCESSING, COMPLETED, FAILED, REVERSED, CANCELLED';
COMMENT ON COLUMN transactions.idempotency_key IS 'Key for preventing duplicate transactions';
COMMENT ON COLUMN transactions.failure_reason IS 'Reason for failure if transaction failed';
