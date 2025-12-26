-- Create Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    event_id VARCHAR(100),
    event_type VARCHAR(50),
    reference_id UUID,
    reference_type VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'ACCOUNT_CREATED', 'ACCOUNT_UPDATED', 'ACCOUNT_CLOSED',
        'TRANSACTION_COMPLETED', 'TRANSACTION_FAILED',
        'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_SCHEDULED',
        'BALANCE_LOW', 'BALANCE_UPDATED',
        'KYC_VERIFIED', 'KYC_REJECTED',
        'LOGIN_ALERT', 'SECURITY_ALERT', 'GENERAL'
    )),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notification_channel ON notifications(channel);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_reference ON notifications(reference_id, reference_type);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_status_retry ON notifications(status, retry_count);

-- Add comments for documentation
COMMENT ON TABLE notifications IS 'User notifications sent via email, SMS, push, or in-app';
COMMENT ON COLUMN notifications.notification_type IS 'Type of notification: ACCOUNT_CREATED, TRANSACTION_COMPLETED, etc.';
COMMENT ON COLUMN notifications.channel IS 'Channel: EMAIL, SMS, PUSH, IN_APP';
COMMENT ON COLUMN notifications.status IS 'Status: PENDING, SENT, FAILED, CANCELLED';
COMMENT ON COLUMN notifications.reference_id IS 'Reference to related entity (transaction, payment, account, etc.)';
COMMENT ON COLUMN notifications.reference_type IS 'Type of referenced entity: TRANSACTION, PAYMENT, ACCOUNT, etc.';
COMMENT ON COLUMN notifications.retry_count IS 'Number of times sending was retried';
