-- Flyway Migration: Create SMS Log Table
-- V1__create_sms_log_table.sql

CREATE TABLE d_cbs_sms_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(20) NOT NULL,
    sms_status VARCHAR(100),
    sms_log_date TIMESTAMP,
    description TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    CONSTRAINT uq_sms_log_phone_date UNIQUE (phone_number, sms_log_date)
);

CREATE INDEX idx_sms_log_phone ON d_cbs_sms_log(phone_number);
CREATE INDEX idx_sms_log_status ON d_cbs_sms_log(sms_status);
CREATE INDEX idx_sms_log_deleted ON d_cbs_sms_log(is_deleted);
