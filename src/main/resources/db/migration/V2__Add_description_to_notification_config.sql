-- Add description column to notification_config table for SMS templates
ALTER TABLE notification_config ADD COLUMN description TEXT;
