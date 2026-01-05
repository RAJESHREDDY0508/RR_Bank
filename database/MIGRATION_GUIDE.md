# Database Migration Guide

This guide explains how to run database migrations for RR-Bank.

## Account Number Format Migration

The new account number format is: `XX##-####-####`
- `XX` = Account type prefix (SA=Savings, CH=Checking, CR=Credit, BU=Business)
- `##` = 2 random digits
- `####-####` = 8 random digits

Example: `CH12-3456-7890`

---

## Option 1: Automatic Migration (Recommended)

The application now includes an automatic migration that runs on startup.

**How it works:**
1. When the account-service starts, it checks all existing accounts
2. Accounts with old format numbers are automatically updated to the new format
3. Accounts already in the new format are skipped

**To run:**
```bash
# Just restart the account-service
cd services/account-service
mvn spring-boot:run
```

The migration class is: `AccountNumberMigration.java`

---

## Option 2: Flyway Migration

Flyway is now configured in the account-service for automatic SQL migrations.

**Location:** `services/account-service/src/main/resources/db/migration/`

**To add new migrations:**
1. Create a new SQL file with naming convention: `V{version}__{description}.sql`
2. Example: `V2__add_new_column.sql`
3. Restart the application - Flyway runs automatically

**Current migrations:**
- `V1__update_account_numbers.sql` - Updates existing account numbers to new format

---

## Option 3: Manual SQL Script

If you prefer to run the migration manually:

### For Local PostgreSQL

```bash
# Connect to the database
psql -h localhost -U rrbank -d account_db

# Run the migration script
\i database/V1__update_account_numbers.sql
```

### For Docker PostgreSQL

```bash
# Copy script to container
docker cp database/V1__update_account_numbers.sql postgres:/tmp/

# Execute in container
docker exec -it postgres psql -U rrbank -d account_db -f /tmp/V1__update_account_numbers.sql
```

### For Supabase

1. Go to Supabase Dashboard → SQL Editor
2. Paste the contents of `database/V1__update_account_numbers.sql`
3. Click "Run"

---

## Verifying the Migration

After running any migration option, verify:

```sql
-- Check account numbers
SELECT id, account_number, account_type FROM accounts;

-- All should match pattern: XX##-####-####
SELECT account_number FROM accounts WHERE account_number ~ '^[A-Z]{2}[0-9]{2}-[0-9]{4}-[0-9]{4}$';
```

---

## Rollback

If you need to rollback (not recommended as old format is less user-friendly):

```sql
-- This will regenerate random old-style numbers
UPDATE accounts 
SET account_number = 
    CASE account_type
        WHEN 'SAVINGS' THEN 'SAV'
        WHEN 'CHECKING' THEN 'CHK'
        WHEN 'CREDIT' THEN 'CRD'
        WHEN 'BUSINESS' THEN 'BUS'
    END || EXTRACT(EPOCH FROM NOW())::BIGINT || LPAD(FLOOR(RANDOM() * 10000)::TEXT, 4, '0');
```

---

## Disabling Automatic Migration

If you want to disable the startup migration after it has run:

1. Delete or rename `AccountNumberMigration.java`
2. Or add `@ConditionalOnProperty` annotation:

```java
@Component
@ConditionalOnProperty(name = "migration.account-numbers.enabled", havingValue = "true", matchIfMissing = false)
public class AccountNumberMigration implements CommandLineRunner {
    // ...
}
```

Then set `migration.account-numbers.enabled=false` in application.yml.

---

## Troubleshooting

### Migration not running?
- Check application logs for "Starting account number migration check..."
- Ensure the account-service can connect to the database

### Duplicate account number error?
- The migration checks for uniqueness, but if you see errors:
```sql
-- Find duplicates
SELECT account_number, COUNT(*) FROM accounts GROUP BY account_number HAVING COUNT(*) > 1;
```

### Flyway checksum error?
- If you modified a migration file that already ran:
```sql
DELETE FROM flyway_schema_history WHERE version = '1';
```
Then restart the application.
