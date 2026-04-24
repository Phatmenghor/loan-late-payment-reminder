-- Oracle View Diagnostic Script
-- Run this as user: stg
-- This will show you the exact structure of your view

-- Step 1: Check if the view exists
SELECT 'Step 1: View Existence Check' as diagnostic;
SELECT VIEW_NAME, OWNER
FROM ALL_VIEWS
WHERE VIEW_NAME = 'VIEW_LOAN_LATE_REMINDER'
   OR LOWER(VIEW_NAME) = 'view_loan_late_reminder';

-- Step 2: Describe the view structure (actual column names)
DESC view_loan_late_reminder;

-- Step 3: Show column names and types from data dictionary
SELECT 'Step 2: Exact Column Names and Types' as diagnostic;
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH
FROM ALL_TAB_COLUMNS
WHERE TABLE_NAME = 'VIEW_LOAN_LATE_REMINDER'
   OR TABLE_NAME = 'view_loan_late_reminder'
ORDER BY COLUMN_ID;

-- Step 4: Get a sample row with all columns (to see actual data)
SELECT 'Step 3: Sample Data' as diagnostic;
SELECT * FROM view_loan_late_reminder WHERE ROWNUM <= 1;

-- Step 5: Count how many records match criteria
SELECT 'Step 4: Record Count' as diagnostic;
SELECT COUNT(*) as total_records FROM view_loan_late_reminder;

-- Step 6: Show column names in uppercase for verification
SELECT 'Step 5: Column Names in Uppercase' as diagnostic;
SELECT COLUMN_NAME
FROM ALL_TAB_COLUMNS
WHERE UPPER(TABLE_NAME) = 'VIEW_LOAN_LATE_REMINDER'
ORDER BY COLUMN_ID;
