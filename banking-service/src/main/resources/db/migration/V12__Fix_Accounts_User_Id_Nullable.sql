-- V12__Fix_Accounts_User_Id_Nullable.sql
-- Make user_id nullable since accounts are linked via customer_id, not user_id directly

-- First, alter the user_id column to allow NULL
ALTER TABLE accounts ALTER COLUMN user_id DROP NOT NULL;

-- Add comment explaining the relationship
COMMENT ON COLUMN accounts.user_id IS 'Optional direct link to user - accounts are primarily linked via customer_id';
