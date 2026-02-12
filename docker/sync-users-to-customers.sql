-- ============================================================
-- RR-Bank - Sync Users to Customers
-- This script creates customer records for existing users
-- that don't have a corresponding customer entry.
-- ============================================================

-- Run this in postgres container:
-- docker exec -it rrbank-postgres psql -U postgres

-- First, check how many users exist without customers
\echo '=== Users without Customer records ==='
SELECT u.id as user_id, u.username, u.email, u.first_name, u.last_name
FROM auth_db.public.users u
WHERE NOT EXISTS (
    SELECT 1 FROM customer_db.public.customers c WHERE c.user_id = u.id
);

-- Count users vs customers
\echo '=== User Count ==='
SELECT COUNT(*) as user_count FROM auth_db.public.users;

\echo '=== Customer Count ==='
SELECT COUNT(*) as customer_count FROM customer_db.public.customers;

-- Insert missing customers
\echo '=== Creating missing customer records ==='
INSERT INTO customer_db.public.customers (id, user_id, first_name, last_name, email, kyc_verified, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    u.id,
    COALESCE(u.first_name, 'User'),
    COALESCE(u.last_name, 'Unknown'),
    u.email,
    false,
    NOW(),
    NOW()
FROM auth_db.public.users u
WHERE NOT EXISTS (
    SELECT 1 FROM customer_db.public.customers c WHERE c.user_id = u.id
)
ON CONFLICT (user_id) DO NOTHING;

\echo '=== New Customer Count ==='
SELECT COUNT(*) as customer_count FROM customer_db.public.customers;

\echo '=== Sync Complete ==='
