-- ============================================================
-- RR-Bank Database Migration: Update Account Numbers
-- ============================================================
-- This script updates existing account numbers to the new format
-- New Format: XX##-####-#### (e.g., CH12-3456-7890)
-- Where XX is the account type prefix:
--   SA = Savings
--   CH = Checking
--   CR = Credit
--   BU = Business
-- ============================================================

-- Run this on your account_db database

-- First, let's see what we have
SELECT id, account_number, account_type FROM accounts;

-- Update account numbers to new format
UPDATE accounts 
SET account_number = 
    CASE account_type
        WHEN 'SAVINGS' THEN 'SA'
        WHEN 'CHECKING' THEN 'CH'
        WHEN 'CREDIT' THEN 'CR'
        WHEN 'BUSINESS' THEN 'BU'
        ELSE 'XX'
    END 
    || LPAD(FLOOR(RANDOM() * 100)::TEXT, 2, '0')
    || '-'
    || LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0')
    || '-'
    || LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0')
WHERE account_number NOT LIKE '__%-____-____';

-- Verify the update
SELECT id, account_number, account_type FROM accounts;

-- ============================================================
-- Alternative: If you want deterministic account numbers based on ID
-- ============================================================
-- UPDATE accounts 
-- SET account_number = 
--     CASE account_type
--         WHEN 'SAVINGS' THEN 'SA'
--         WHEN 'CHECKING' THEN 'CH'
--         WHEN 'CREDIT' THEN 'CR'
--         WHEN 'BUSINESS' THEN 'BU'
--         ELSE 'XX'
--     END 
--     || SUBSTRING(REPLACE(id::TEXT, '-', ''), 1, 2)
--     || '-'
--     || SUBSTRING(REPLACE(id::TEXT, '-', ''), 3, 4)
--     || '-'
--     || SUBSTRING(REPLACE(id::TEXT, '-', ''), 7, 4)
-- WHERE account_number NOT LIKE '__%-____-____';
