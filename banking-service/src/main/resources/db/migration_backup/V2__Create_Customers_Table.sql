-- Create Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code VARCHAR(10),
    country VARCHAR(50),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP,
    kyc_document_type VARCHAR(50),
    kyc_document_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_customer_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customer_kyc_status ON customers(kyc_status);
CREATE INDEX IF NOT EXISTS idx_customer_city ON customers(city);
CREATE INDEX IF NOT EXISTS idx_customer_state ON customers(state);
CREATE INDEX IF NOT EXISTS idx_customer_country ON customers(country);
CREATE INDEX IF NOT EXISTS idx_customer_created_at ON customers(created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE customers IS 'Customer profiles with KYC information';
COMMENT ON COLUMN customers.kyc_status IS 'KYC verification status: PENDING, IN_PROGRESS, VERIFIED, REJECTED, EXPIRED';
COMMENT ON COLUMN customers.kyc_document_type IS 'Document type used for KYC: PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN';
