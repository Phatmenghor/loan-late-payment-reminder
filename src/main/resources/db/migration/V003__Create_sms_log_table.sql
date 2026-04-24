-- Create sms_log table for SMS delivery audit trail
CREATE TABLE IF NOT EXISTS sms_log (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    customer_id VARCHAR(100),
    report_date DATE,
    arrangement_id VARCHAR(100),
    day_due INTEGER,
    message_content TEXT,
    sms_status VARCHAR(20) NOT NULL,
    sms_log_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create indexes for query performance
CREATE INDEX IF NOT EXISTS idx_sms_log_phone_number ON sms_log(phone_number);
CREATE INDEX IF NOT EXISTS idx_sms_log_customer_id ON sms_log(customer_id);
CREATE INDEX IF NOT EXISTS idx_sms_log_status ON sms_log(sms_status);
CREATE INDEX IF NOT EXISTS idx_sms_log_report_date ON sms_log(report_date);
CREATE INDEX IF NOT EXISTS idx_sms_log_date ON sms_log(sms_log_date);
CREATE INDEX IF NOT EXISTS idx_sms_log_is_deleted ON sms_log(is_deleted);
