-- ============================================================
-- V13__Fix_Fraud_Events_Add_Missing_Columns.sql
-- Adds missing columns to fraud_events table
-- ============================================================

-- Add resolved column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'resolved') THEN
        ALTER TABLE fraud_events ADD COLUMN resolved BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Add resolved_by column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'resolved_by') THEN
        ALTER TABLE fraud_events ADD COLUMN resolved_by VARCHAR(36);
    END IF;
END $$;

-- Add resolved_at column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'resolved_at') THEN
        ALTER TABLE fraud_events ADD COLUMN resolved_at TIMESTAMP;
    END IF;
END $$;

-- Add resolution_notes column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'resolution_notes') THEN
        ALTER TABLE fraud_events ADD COLUMN resolution_notes TEXT;
    END IF;
END $$;

-- Add action_taken column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'action_taken') THEN
        ALTER TABLE fraud_events ADD COLUMN action_taken VARCHAR(50);
    END IF;
END $$;

-- Add status column if it doesn't exist (for FraudStatus enum)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'status') THEN
        ALTER TABLE fraud_events ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING_REVIEW';
    END IF;
END $$;

-- Create index on resolved column (only if column exists)
CREATE INDEX IF NOT EXISTS idx_fraud_resolved ON fraud_events(resolved);

-- Add other missing columns that the entity expects
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'transaction_amount') THEN
        ALTER TABLE fraud_events ADD COLUMN transaction_amount DECIMAL(19, 4);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'transaction_type') THEN
        ALTER TABLE fraud_events ADD COLUMN transaction_type VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'fraud_reasons') THEN
        ALTER TABLE fraud_events ADD COLUMN fraud_reasons TEXT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'rules_triggered') THEN
        ALTER TABLE fraud_events ADD COLUMN rules_triggered TEXT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'location_country') THEN
        ALTER TABLE fraud_events ADD COLUMN location_country VARCHAR(100);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'fraud_events' AND column_name = 'location_city') THEN
        ALTER TABLE fraud_events ADD COLUMN location_city VARCHAR(100);
    END IF;
END $$;
