-- Create Fraud Events Table
CREATE TABLE IF NOT EXISTS fraud_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    customer_id UUID,
    transaction_amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(50),
    risk_score DECIMAL(5,2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fraud_reasons TEXT,
    rules_triggered TEXT,
    location_ip VARCHAR(50),
    location_country VARCHAR(100),
    location_city VARCHAR(100),
    device_fingerprint VARCHAR(200),
    transaction_velocity DECIMAL(10,2),
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    action_taken VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_fraud_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_fraud_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_fraud_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_fraud_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_fraud_status CHECK (status IN ('PENDING_REVIEW', 'UNDER_INVESTIGATION', 'REVIEWED', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE', 'AUTO_CLEARED'))
);

-- Create Fraud Rules Table
CREATE TABLE IF NOT EXISTS fraud_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    threshold_value DECIMAL(15,2),
    time_window_minutes INTEGER,
    risk_score_points INTEGER NOT NULL,
    priority INTEGER NOT NULL DEFAULT 5,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_block BOOLEAN NOT NULL DEFAULT FALSE,
    country_blacklist TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_fraud_rule_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_fraud_rule_type CHECK (rule_type IN (
        'HIGH_AMOUNT', 'TRANSACTION_VELOCITY', 'UNUSUAL_LOCATION', 'AMOUNT_SPIKE',
        'OFF_HOURS', 'NEW_PAYEE', 'DORMANT_ACCOUNT', 'ROUND_AMOUNT',
        'FOREIGN_TRANSACTION', 'MULTIPLE_FAILURES', 'RAPID_SUCCESSION',
        'DEVICE_CHANGE', 'IP_CHANGE', 'BLACKLISTED_LOCATION'
    ))
);

-- Create indexes for fraud_events
CREATE INDEX IF NOT EXISTS idx_fraud_transaction ON fraud_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_fraud_account ON fraud_events(account_id);
CREATE INDEX IF NOT EXISTS idx_fraud_status ON fraud_events(status);
CREATE INDEX IF NOT EXISTS idx_fraud_risk_level ON fraud_events(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_created_at ON fraud_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_account_created ON fraud_events(account_id, created_at DESC);

-- Create indexes for fraud_rules
CREATE INDEX IF NOT EXISTS idx_fraud_rule_type ON fraud_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_fraud_rule_enabled ON fraud_rules(is_enabled);
CREATE INDEX IF NOT EXISTS idx_fraud_rule_priority ON fraud_rules(priority DESC);

-- Insert default fraud detection rules
INSERT INTO fraud_rules (id, rule_name, rule_description, rule_type, threshold_value, risk_score_points, priority, is_enabled, auto_block)
VALUES 
    (gen_random_uuid(), 'High Amount Transaction', 'Transactions over $10,000', 'HIGH_AMOUNT', 10000.00, 30, 10, TRUE, FALSE),
    (gen_random_uuid(), 'Transaction Velocity', 'More than 5 transactions in 30 minutes', 'TRANSACTION_VELOCITY', NULL, 25, 8, TRUE, FALSE),
    (gen_random_uuid(), 'Foreign Transaction', 'Transaction from outside the US', 'FOREIGN_TRANSACTION', NULL, 15, 5, TRUE, FALSE),
    (gen_random_uuid(), 'Off Hours Transaction', 'Transaction between 2 AM and 6 AM', 'OFF_HOURS', NULL, 10, 3, TRUE, FALSE),
    (gen_random_uuid(), 'Round Amount Alert', 'Suspiciously round amounts (e.g., $5,000.00)', 'ROUND_AMOUNT', NULL, 20, 7, TRUE, FALSE),
    (gen_random_uuid(), 'Rapid Succession', 'Multiple transactions within 5 minutes', 'RAPID_SUCCESSION', NULL, 25, 9, TRUE, FALSE);

-- Add comments
COMMENT ON TABLE fraud_events IS 'Fraud detection events and flagged transactions';
COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';
COMMENT ON COLUMN fraud_events.risk_score IS 'Risk score from 0-100';
COMMENT ON COLUMN fraud_events.risk_level IS 'LOW (0-25), MEDIUM (26-50), HIGH (51-75), CRITICAL (76-100)';
COMMENT ON COLUMN fraud_events.action_taken IS 'BLOCKED, ALLOWED, FLAGGED_FOR_REVIEW';
COMMENT ON COLUMN fraud_rules.priority IS 'Higher number = higher priority (1-10)';
COMMENT ON COLUMN fraud_rules.auto_block IS 'If true, automatically block transactions that trigger this rule';
