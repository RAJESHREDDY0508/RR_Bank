-- Flyway Migration V1: Update account numbers to new format
-- Format: XX##-####-#### (e.g., CH12-3456-7890)

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
