CREATE TABLE IF NOT EXISTS sms_config (
    id UUID PRIMARY KEY,
    config_type VARCHAR(50) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    CONSTRAINT idx_config_type UNIQUE (config_type)
);

CREATE INDEX IF NOT EXISTS idx_sms_config_type ON sms_config(config_type);
