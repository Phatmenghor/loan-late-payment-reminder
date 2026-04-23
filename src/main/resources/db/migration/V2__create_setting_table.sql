-- Flyway Migration: Create Setting Table
-- V2__create_setting_table.sql

CREATE TABLE d_cbs_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    set_desc TEXT,
    set_key VARCHAR(255),
    set_value TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255)
);

CREATE INDEX idx_setting_key ON d_cbs_setting(set_key);
CREATE INDEX idx_setting_deleted ON d_cbs_setting(is_deleted);
