-- Create SMS Accepted Log table for audit trail
CREATE TABLE IF NOT EXISTS sms_accepted_log (
    id UUID PRIMARY KEY NOT NULL,
    msg_id VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    sms_content TEXT,
    sms_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    error_details TEXT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    updated_by VARCHAR(255),
    updated_date TIMESTAMP
);

-- Create indexes for query performance
CREATE INDEX idx_sms_accepted_log_msg_id ON sms_accepted_log(msg_id);
CREATE INDEX idx_sms_accepted_log_phone ON sms_accepted_log(phone);
CREATE INDEX idx_sms_accepted_log_status ON sms_accepted_log(sms_status);
CREATE INDEX idx_sms_accepted_log_created_at ON sms_accepted_log(created_at);
