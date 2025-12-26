-- V2__Create_Accounts_Table.sql
-- Migration to create accounts table (PostgreSQL-safe)

CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(36) PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    interest_rate DECIMAL(5, 4),
    overdraft_limit DECIMAL(19, 4) DEFAULT 0.0000,
    minimum_balance DECIMAL(19, 4) DEFAULT 0.0000,
    monthly_fee DECIMAL(19, 4) DEFAULT 0.0000,
    last_statement_date TIMESTAMP,
    next_statement_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(36) NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- Safely create indexes (PostgreSQL-safe)
CREATE INDEX IF NOT EXISTS idx_account_number 
    ON accounts(account_number);

CREATE INDEX IF NOT EXISTS idx_user_id 
    ON accounts(user_id);

CREATE INDEX IF NOT EXISTS idx_account_type 
    ON accounts(account_type);

CREATE INDEX IF NOT EXISTS idx_status 
    ON accounts(status);
