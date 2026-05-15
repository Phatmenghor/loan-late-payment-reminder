-- Insert SMS configuration for batch SMS sending
INSERT INTO notification_config (
    id,
    config_type,
    config_value,
    description,
    is_active,
    created_by,
    created_date
) VALUES (
    gen_random_uuid(),
    'NOTIFICATION_SMS',
    'ធនាគារប្រៃសណីយ៍កម្ពុជា ក.អ សូមស្វាគមន៍! សូមលោកអ្នកអញ្ជើញមកបង់ប្រាក់ឲ្យបានទាន់ពេលតាមតារាងសងប្រាក់របស់លោកអ្នក។ សូមអរគុណ 070 200 002',
    'SMS message for loan payment reminder',
    true,
    'SYSTEM',
    CURRENT_TIMESTAMP
) ON CONFLICT (config_type) DO NOTHING;
