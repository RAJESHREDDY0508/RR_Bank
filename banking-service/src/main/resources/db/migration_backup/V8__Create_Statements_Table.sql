-- Create Statements Table
CREATE TABLE IF NOT EXISTS statements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    statement_period VARCHAR(7) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    statement_type VARCHAR(20) NOT NULL,
    opening_balance DECIMAL(15,2) NOT NULL,
    closing_balance DECIMAL(15,2) NOT NULL,
    total_deposits DECIMAL(15,2),
    total_withdrawals DECIMAL(15,2),
    total_transactions INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    pdf_file_path VARCHAR(500),
    csv_file_path VARCHAR(500),
    pdf_file_size BIGINT,
    csv_file_size BIGINT,
    s3_bucket VARCHAR(200),
    generated_at TIMESTAMP,
    generated_by UUID,
    download_count INTEGER NOT NULL DEFAULT 0,
    last_downloaded_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_statement_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_statement_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_statement_generated_by FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_statement_type CHECK (statement_type IN ('MONTHLY', 'QUARTERLY', 'ANNUAL', 'ON_DEMAND')),
    CONSTRAINT chk_statement_status CHECK (status IN ('PENDING', 'GENERATING', 'GENERATED', 'FAILED', 'ARCHIVED')),
    CONSTRAINT uq_statement_account_period UNIQUE (account_id, statement_period)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_statement_account ON statements(account_id);
CREATE INDEX IF NOT EXISTS idx_statement_customer ON statements(customer_id);
CREATE INDEX IF NOT EXISTS idx_statement_period ON statements(statement_period);
CREATE INDEX IF NOT EXISTS idx_statement_status ON statements(status);
CREATE INDEX IF NOT EXISTS idx_statement_created_at ON statements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_statement_period_end ON statements(period_end_date DESC);

-- Create composite indexes
CREATE INDEX IF NOT EXISTS idx_statement_account_period ON statements(account_id, statement_period);
CREATE INDEX IF NOT EXISTS idx_statement_account_date ON statements(account_id, period_end_date DESC);
CREATE INDEX IF NOT EXISTS idx_statement_customer_date ON statements(customer_id, period_end_date DESC);

-- Add comments for documentation
COMMENT ON TABLE statements IS 'Account statements (monthly, quarterly, on-demand)';
COMMENT ON COLUMN statements.statement_period IS 'Format: YYYY-MM for monthly statements';
COMMENT ON COLUMN statements.statement_type IS 'MONTHLY, QUARTERLY, ANNUAL, ON_DEMAND';
COMMENT ON COLUMN statements.status IS 'PENDING, GENERATING, GENERATED, FAILED, ARCHIVED';
COMMENT ON COLUMN statements.pdf_file_path IS 'S3 path to PDF file (s3://bucket/key)';
COMMENT ON COLUMN statements.csv_file_path IS 'S3 path to CSV file (s3://bucket/key)';
COMMENT ON COLUMN statements.download_count IS 'Number of times statement was downloaded';
