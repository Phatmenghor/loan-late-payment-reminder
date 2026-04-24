-- Create sms_pending_queue table for SMS notification processing
CREATE TABLE IF NOT EXISTS sms_pending_queue (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    arrangement_id VARCHAR(100),
    day_due INTEGER,
    report_date DATE NOT NULL,
    message_content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    retry_count INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create indexes for query performance
CREATE INDEX IF NOT EXISTS idx_queue_status ON sms_pending_queue(status);
CREATE INDEX IF NOT EXISTS idx_queue_report_date ON sms_pending_queue(report_date);
CREATE INDEX IF NOT EXISTS idx_queue_phone ON sms_pending_queue(phone_number);
CREATE INDEX IF NOT EXISTS idx_queue_status_date ON sms_pending_queue(status, report_date);
CREATE INDEX IF NOT EXISTS idx_queue_customer ON sms_pending_queue(customer_id);
CREATE INDEX IF NOT EXISTS idx_queue_is_deleted ON sms_pending_queue(is_deleted);
