-- Create notification_processing_status table to track daily SMS processing results
CREATE TABLE IF NOT EXISTS notification_processing_status (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    report_date DATE NOT NULL UNIQUE,
    total_customers INTEGER NOT NULL,
    success_count INTEGER NOT NULL,
    failure_count INTEGER NOT NULL,
    is_complete BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP
);

-- Create indexes for query performance
CREATE INDEX IF NOT EXISTS idx_notification_processing_status_report_date ON notification_processing_status(report_date);
CREATE INDEX IF NOT EXISTS idx_notification_processing_status_is_complete ON notification_processing_status(is_complete);
