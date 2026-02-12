-- RR-Bank Admin Database Setup Script
-- Run this in psql or pgAdmin as a superuser (postgres)

-- Create user if not exists
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rrbank') THEN
      CREATE ROLE rrbank WITH LOGIN PASSWORD 'rrbank_secret';
   END IF;
END
$$;

-- Create database if not exists
SELECT 'CREATE DATABASE admin_db OWNER rrbank'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'admin_db')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE admin_db TO rrbank;

-- Connect to admin_db and grant schema privileges
\c admin_db
GRANT ALL ON SCHEMA public TO rrbank;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO rrbank;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO rrbank;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO rrbank;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO rrbank;
