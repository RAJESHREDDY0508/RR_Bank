-- Create Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_reference VARCHAR(50) NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    payee_name VARCHAR(200) NOT NULL,
    payee_account VARCHAR(100),
    payee_reference VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    description TEXT,
    scheduled_date DATE,
    processed_at TIMESTAMP,
    gateway_transaction_id VARCHAR(100),
    gateway_response TEXT,
    failure_reason TEXT,
    initiated_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_payment_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_initiator FOREIGN KEY (initiated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('BILL', 'MERCHANT', 'P2P', 'SUBSCRIPTION', 'INVOICE')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SCHEDULED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('DEBIT_CARD', 'CREDIT_CARD', 'ACH', 'WIRE', 'WALLET'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_payment_account ON payments(account_id);
CREATE INDEX IF NOT EXISTS idx_payment_customer ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_reference ON payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_type ON payments(payment_type);
CREATE INDEX IF NOT EXISTS idx_payment_created_at ON payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_scheduled_date ON payments(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_payment_initiated_by ON payments(initiated_by);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_payment_customer_status ON payments(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_account_date ON payments(account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_status_scheduled ON payments(status, scheduled_date);

-- Add comments for documentation
COMMENT ON TABLE payments IS 'Bill and merchant payments';
COMMENT ON COLUMN payments.payment_reference IS 'Unique payment reference number';
COMMENT ON COLUMN payments.payment_type IS 'Type: BILL, MERCHANT, P2P, SUBSCRIPTION, INVOICE';
COMMENT ON COLUMN payments.status IS 'Status: PENDING, SCHEDULED, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED';
COMMENT ON COLUMN payments.payment_method IS 'Method: DEBIT_CARD, CREDIT_CARD, ACH, WIRE, WALLET';
COMMENT ON COLUMN payments.gateway_transaction_id IS 'Transaction ID from external payment gateway';
COMMENT ON COLUMN payments.scheduled_date IS 'Date when payment should be processed';
