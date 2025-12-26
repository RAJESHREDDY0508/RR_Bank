-- ============================================================
-- V2__Add_MFA_And_Session_Tables.sql
-- Add tables for MFA, OTP codes, and user sessions
-- ============================================================

-- ============================================================
-- TABLE: user_mfa
-- Purpose: Multi-Factor Authentication settings per user
-- ============================================================
CREATE TABLE IF NOT EXISTS user_mfa (
    mfa_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    
    -- TOTP (Google Authenticator)
    totp_enabled BOOLEAN DEFAULT FALSE,
    totp_secret VARCHAR(255),
    totp_verified BOOLEAN DEFAULT FALSE,
    
    -- SMS OTP
    sms_enabled BOOLEAN DEFAULT FALSE,
    sms_phone_number VARCHAR(20),
    sms_verified BOOLEAN DEFAULT FALSE,
    
    -- Email OTP
    email_enabled BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    
    -- Backup codes
    backup_codes VARCHAR(1000),
    backup_codes_generated_at TIMESTAMP,
    
    -- Preferred method
    preferred_method VARCHAR(20) DEFAULT 'NONE',
    
    -- Usage tracking
    last_mfa_at TIMESTAMP,
    mfa_attempts INTEGER DEFAULT 0,
    mfa_locked_until TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_user_mfa_method CHECK (preferred_method IN ('NONE', 'TOTP', 'SMS', 'EMAIL', 'BACKUP'))
);

CREATE INDEX IF NOT EXISTS idx_user_mfa_user_id ON user_mfa(user_id);

COMMENT ON TABLE user_mfa IS 'Multi-Factor Authentication settings per user';

-- ============================================================
-- TABLE: otp_codes
-- Purpose: Temporary OTP codes for verification
-- ============================================================
CREATE TABLE IF NOT EXISTS otp_codes (
    otp_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    code VARCHAR(10) NOT NULL,
    otp_type VARCHAR(20) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    destination VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    
    CONSTRAINT chk_otp_type CHECK (otp_type IN ('SMS', 'EMAIL', 'TOTP')),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN (
        'LOGIN', 'MFA_SETUP', 'MFA_VERIFICATION', 'PASSWORD_RESET',
        'TRANSACTION', 'PROFILE_CHANGE', 'DEVICE_VERIFICATION',
        'PHONE_VERIFICATION', 'EMAIL_VERIFICATION'
    ))
);

CREATE INDEX IF NOT EXISTS idx_otp_user_id ON otp_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_code ON otp_codes(code);
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON otp_codes(expires_at);

COMMENT ON TABLE otp_codes IS 'Temporary OTP codes for verification';

-- ============================================================
-- TABLE: user_sessions
-- Purpose: Track user login sessions and trusted devices
-- ============================================================
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255),
    
    -- Device information
    device_id VARCHAR(100),
    device_name VARCHAR(100),
    device_type VARCHAR(20),
    device_fingerprint VARCHAR(255),
    browser VARCHAR(100),
    os VARCHAR(100),
    
    -- Location information
    ip_address VARCHAR(45),
    city VARCHAR(100),
    country VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Session status
    is_active BOOLEAN DEFAULT TRUE,
    is_trusted BOOLEAN DEFAULT FALSE,
    mfa_verified BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP,
    expires_at TIMESTAMP,
    terminated_at TIMESTAMP,
    termination_reason VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_session_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_session_token ON user_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_session_device_id ON user_sessions(device_id);
CREATE INDEX IF NOT EXISTS idx_session_active ON user_sessions(is_active);

COMMENT ON TABLE user_sessions IS 'User login sessions and trusted devices';

-- ============================================================
-- Apply updated_at trigger to new tables
-- ============================================================
DO $$
BEGIN
    -- user_mfa trigger
    DROP TRIGGER IF EXISTS trg_update_user_mfa_updated_at ON user_mfa;
    CREATE TRIGGER trg_update_user_mfa_updated_at 
        BEFORE UPDATE ON user_mfa 
        FOR EACH ROW 
        EXECUTE FUNCTION update_updated_at_column();
END $$;
