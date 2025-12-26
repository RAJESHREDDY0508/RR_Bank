-- V9__Create_Audit_Logs_Table.sql
-- Audit Service - Immutable Audit Trail
-- Author: RR-Bank Development Team
-- Date: 2024-12-02

-- Create audit_logs table (APPEND-ONLY, NO UPDATES OR DELETES)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Timestamp information
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Event identification
    event_type VARCHAR(100) NOT NULL,
    event_source VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    
    -- Entity references
    entity_type VARCHAR(50),
    entity_id UUID,
    user_id UUID,
    customer_id UUID,
    account_id UUID,
    
    -- Action details
    action VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    
    -- Additional context
    ip_address VARCHAR(50),
    metadata TEXT,
    
    -- Flags
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    compliance_flag BOOLEAN DEFAULT FALSE,
    
    -- Audit trail integrity
    CONSTRAINT no_updates CHECK (created_at IS NOT NULL)
);

-- Create indexes for efficient querying
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_customer ON audit_logs(customer_id);
CREATE INDEX idx_audit_account ON audit_logs(account_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_severity ON audit_logs(severity);
CREATE INDEX idx_audit_compliance ON audit_logs(compliance_flag) WHERE compliance_flag = TRUE;
CREATE INDEX idx_audit_sensitive ON audit_logs(is_sensitive) WHERE is_sensitive = TRUE;

-- Create composite indexes for common query patterns
CREATE INDEX idx_audit_customer_timestamp ON audit_logs(customer_id, timestamp DESC);
CREATE INDEX idx_audit_account_timestamp ON audit_logs(account_id, timestamp DESC);
CREATE INDEX idx_audit_event_timestamp ON audit_logs(event_type, timestamp DESC);

-- Add comments for documentation
COMMENT ON TABLE audit_logs IS 'Immutable audit trail for compliance and security tracking. APPEND-ONLY: No updates or deletes allowed.';
COMMENT ON COLUMN audit_logs.id IS 'Unique identifier for audit log entry';
COMMENT ON COLUMN audit_logs.timestamp IS 'When the audited event occurred';
COMMENT ON COLUMN audit_logs.event_type IS 'Type of event (e.g., ACCOUNT_CREATED, TRANSACTION_COMPLETED)';
COMMENT ON COLUMN audit_logs.event_source IS 'Service that generated the event (e.g., AccountService)';
COMMENT ON COLUMN audit_logs.severity IS 'Severity level: DEBUG, INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN audit_logs.entity_type IS 'Type of entity affected (e.g., ACCOUNT, TRANSACTION)';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID of the affected entity';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (e.g., CREATE, UPDATE, DELETE)';
COMMENT ON COLUMN audit_logs.description IS 'Human-readable description of the event';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous value (for updates)';
COMMENT ON COLUMN audit_logs.new_value IS 'New value (for creates/updates)';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP address of the request origin';
COMMENT ON COLUMN audit_logs.metadata IS 'Additional context in JSON format';
COMMENT ON COLUMN audit_logs.is_sensitive IS 'Flag for sensitive data (PII, financial info)';
COMMENT ON COLUMN audit_logs.compliance_flag IS 'Flag for events requiring compliance review';
COMMENT ON COLUMN audit_logs.created_at IS 'When the audit log was created in the database';

-- Grant permissions (adjust based on your security model)
-- GRANT SELECT ON audit_logs TO audit_reader;
-- GRANT INSERT ON audit_logs TO audit_writer;
-- No UPDATE or DELETE permissions should be granted

-- Create a view for high-severity events
CREATE OR REPLACE VIEW high_severity_audit_logs AS
SELECT *
FROM audit_logs
WHERE severity IN ('ERROR', 'CRITICAL')
ORDER BY timestamp DESC;

-- Create a view for compliance review
CREATE OR REPLACE VIEW compliance_audit_logs AS
SELECT *
FROM audit_logs
WHERE compliance_flag = TRUE
ORDER BY timestamp DESC;

-- Create a view for recent activity (last 24 hours)
CREATE OR REPLACE VIEW recent_audit_activity AS
SELECT *
FROM audit_logs
WHERE timestamp >= (CURRENT_TIMESTAMP - INTERVAL '24 hours')
ORDER BY timestamp DESC;

-- Statistics function for audit logs
CREATE OR REPLACE FUNCTION get_audit_stats()
RETURNS TABLE (
    total_logs BIGINT,
    info_count BIGINT,
    warning_count BIGINT,
    error_count BIGINT,
    critical_count BIGINT,
    compliance_count BIGINT,
    sensitive_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT as total_logs,
        COUNT(*) FILTER (WHERE severity = 'INFO')::BIGINT as info_count,
        COUNT(*) FILTER (WHERE severity = 'WARNING')::BIGINT as warning_count,
        COUNT(*) FILTER (WHERE severity = 'ERROR')::BIGINT as error_count,
        COUNT(*) FILTER (WHERE severity = 'CRITICAL')::BIGINT as critical_count,
        COUNT(*) FILTER (WHERE compliance_flag = TRUE)::BIGINT as compliance_count,
        COUNT(*) FILTER (WHERE is_sensitive = TRUE)::BIGINT as sensitive_count
    FROM audit_logs;
END;
$$ LANGUAGE plpgsql;

-- Create a function to prevent updates and deletes (enforcement at DB level)
CREATE OR REPLACE FUNCTION prevent_audit_log_modifications()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Updates are not allowed on audit_logs table. This is an immutable audit trail.';
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Deletes are not allowed on audit_logs table. This is an immutable audit trail.';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to prevent modifications
CREATE TRIGGER prevent_audit_log_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modifications();

CREATE TRIGGER prevent_audit_log_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_log_modifications();

-- Add table constraints for data integrity
ALTER TABLE audit_logs
    ADD CONSTRAINT chk_event_type_not_empty CHECK (event_type <> ''),
    ADD CONSTRAINT chk_event_source_not_empty CHECK (event_source <> ''),
    ADD CONSTRAINT chk_action_not_empty CHECK (action <> ''),
    ADD CONSTRAINT chk_description_not_empty CHECK (description <> '');

-- Create a materialized view for audit statistics (refreshed periodically)
CREATE MATERIALIZED VIEW audit_stats_summary AS
SELECT
    DATE_TRUNC('day', timestamp) as date,
    event_type,
    event_source,
    severity,
    COUNT(*) as event_count,
    COUNT(*) FILTER (WHERE compliance_flag = TRUE) as compliance_count,
    COUNT(*) FILTER (WHERE is_sensitive = TRUE) as sensitive_count
FROM audit_logs
GROUP BY DATE_TRUNC('day', timestamp), event_type, event_source, severity
ORDER BY date DESC, event_count DESC;

-- Create index on materialized view
CREATE INDEX idx_audit_stats_date ON audit_stats_summary(date);
CREATE INDEX idx_audit_stats_event_type ON audit_stats_summary(event_type);

-- Add comment
COMMENT ON MATERIALIZED VIEW audit_stats_summary IS 'Daily statistics for audit logs. Refresh periodically with: REFRESH MATERIALIZED VIEW audit_stats_summary;';

-- Sample data retention policy (commented out - implement based on requirements)
-- -- Archive old audit logs (older than 7 years)
-- CREATE OR REPLACE FUNCTION archive_old_audit_logs()
-- RETURNS void AS $$
-- BEGIN
--     -- Move old logs to archive table
--     INSERT INTO audit_logs_archive
--     SELECT * FROM audit_logs
--     WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '7 years');
--     
--     -- Note: In production, you might not delete even after archiving
--     -- DELETE FROM audit_logs WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '7 years');
-- END;
-- $$ LANGUAGE plpgsql;

-- Performance monitoring query
-- Use this to identify slow queries and optimize indexes
COMMENT ON TABLE audit_logs IS 'Monitor query performance with: EXPLAIN ANALYZE SELECT * FROM audit_logs WHERE customer_id = $1 ORDER BY timestamp DESC LIMIT 20;';

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Audit Logs table created successfully with immutable constraints';
    RAISE NOTICE 'Views created: high_severity_audit_logs, compliance_audit_logs, recent_audit_activity';
    RAISE NOTICE 'Functions created: get_audit_stats(), prevent_audit_log_modifications()';
    RAISE NOTICE 'Triggers created: prevent_audit_log_update, prevent_audit_log_delete';
    RAISE NOTICE 'Materialized view created: audit_stats_summary (refresh periodically)';
END $$;
