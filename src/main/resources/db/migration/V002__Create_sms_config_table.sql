CREATE TABLE IF NOT EXISTS sms_config (
    id UUID PRIMARY KEY,
    config_type VARCHAR(50) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_config_type ON sms_config(config_type);

-- Insert default SMS message for loan late payment reminder
INSERT INTO sms_config (id, config_type, config_value, is_active, created_at)
VALUES (
    gen_uuid(),
    'SMS_LOAN_LATE',
    'ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002',
    true,
    NOW()
)
ON CONFLICT (config_type) DO NOTHING;
