-- ============================================================
-- V15__Add_Account_Requests_Table.sql
-- Purpose: Add account_requests table for account opening workflow
-- ============================================================

-- ============================================================
-- TABLE: account_requests
-- Purpose: Account opening requests for admin approval
-- ============================================================
CREATE TABLE IF NOT EXISTS account_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    initial_deposit DECIMAL(19, 4) DEFAULT 0.0000,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_notes TEXT,
    admin_notes TEXT,
    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP,
    account_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_account_requests_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'CREDIT', 'BUSINESS')),
    CONSTRAINT chk_account_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_account_requests_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX IF NOT EXISTS idx_account_requests_user ON account_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_account_requests_status ON account_requests(status);
CREATE INDEX IF NOT EXISTS idx_account_requests_created ON account_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_account_requests_user_type_status ON account_requests(user_id, account_type, status);

COMMENT ON TABLE account_requests IS 'Account opening requests pending admin approval';
COMMENT ON COLUMN account_requests.status IS 'Request status: PENDING, APPROVED, REJECTED, CANCELLED';

-- Apply trigger to update updated_at
CREATE TRIGGER trg_update_account_requests_updated_at 
    BEFORE UPDATE ON account_requests 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
