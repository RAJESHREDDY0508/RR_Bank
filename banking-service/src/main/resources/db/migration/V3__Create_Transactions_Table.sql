-- V3__Create_Transactions_Table.sql
-- Migration to create transactions table (PostgreSQL-safe)

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    reference_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    balance_before DECIMAL(19, 4),
    balance_after DECIMAL(19, 4),
    fee DECIMAL(19, 4) DEFAULT 0.0000,
    from_account_number VARCHAR(20),
    to_account_number VARCHAR(20),
    merchant_name VARCHAR(100),
    merchant_category VARCHAR(50),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_date TIMESTAMP,
    completed_date TIMESTAMP,
    failure_reason VARCHAR(500),
    account_id VARCHAR(36) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id)
        REFERENCES accounts(account_id) ON DELETE CASCADE
);

-- UNIQUE index names (PostgreSQL requires unique index names globally)
CREATE INDEX IF NOT EXISTS idx_transactions_account_id 
    ON transactions(account_id);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date 
    ON transactions(transaction_date);

CREATE INDEX IF NOT EXISTS idx_transactions_reference_number 
    ON transactions(reference_number);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_type 
    ON transactions(transaction_type);

CREATE INDEX IF NOT EXISTS idx_transactions_status 
    ON transactions(status);
