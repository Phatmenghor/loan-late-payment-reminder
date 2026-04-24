-- Create notification_log table for SMS delivery audit trail
CREATE TABLE IF NOT EXISTS notification_log (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    customer_id VARCHAR(100),
    report_date DATE,
    arrangement_id VARCHAR(100),
    day_due INTEGER,
    message_content TEXT,
    notification_status VARCHAR(20) NOT NULL,
    notification_log_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create indexes for query performance
CREATE INDEX IF NOT EXISTS idx_notification_log_phone_number ON notification_log(phone_number);
CREATE INDEX IF NOT EXISTS idx_notification_log_customer_id ON notification_log(customer_id);
CREATE INDEX IF NOT EXISTS idx_notification_log_status ON notification_log(notification_status);
CREATE INDEX IF NOT EXISTS idx_notification_log_report_date ON notification_log(report_date);
CREATE INDEX IF NOT EXISTS idx_notification_log_date ON notification_log(notification_log_date);
CREATE INDEX IF NOT EXISTS idx_notification_log_is_deleted ON notification_log(is_deleted);
