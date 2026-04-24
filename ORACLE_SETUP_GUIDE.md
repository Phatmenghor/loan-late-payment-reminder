# Oracle Database Setup Guide

This application requires one specific Oracle database object to function. Follow this guide to set up your Oracle database correctly.

## Required Oracle Objects

### 1. VIEW: stg.view_loan_late_reminder

This view must contain the following columns (case-insensitive in Oracle):
- `reportdate` (DATE) - The report date for the reminder
- `customerid` (VARCHAR2) - Customer ID
- `mbapp_phone` (VARCHAR2) - Mobile phone number
- `arrangement_id` (VARCHAR2) - Loan arrangement ID
- `day_due` (NUMBER) - Number of days overdue

**Important**: The column name must be `day_due` (with underscore). If your view uses `daydue` (no underscore), you need to:
1. Either rename the column in your view, OR
2. Create an alias: `daydue AS day_due`

**Example view creation** (adjust based on your actual tables):
```sql
CREATE OR REPLACE VIEW stg.view_loan_late_reminder AS
SELECT
    a.reportdate,
    a.customerid,
    b.mbapp_phone,
    a.arrangement_id,
    a.daydue AS day_due  -- IMPORTANT: Use alias if actual column is 'daydue'
FROM sg_grid_summary a
INNER JOIN sg_customers b ON a.customerid = b.customerid
WHERE TRUNC(a.reportdate) >= TRUNC(SYSDATE) - 1
  AND a.daydue BETWEEN 3 AND 30
  AND b.mbapp_phone IS NOT NULL
ORDER BY a.reportdate DESC, a.customerid;
```

## PostgreSQL Configuration

**SMS Message**: Configured in PostgreSQL `sms_config` table with default message already set:
- Table: `sms_config`
- Config Type: `SMS_LOAN_LATE`
- Default Value: Khmer loan payment reminder message

The migration automatically creates this table and inserts the default message on first run. You can update the message without code changes by updating the `config_value` in the database.

## Verification

To verify your setup is correct, run these queries in SQLPlus:

```sql
-- Test the view
SELECT * FROM stg.view_loan_late_reminder WHERE ROWNUM <= 5;

-- Test the settings table
SELECT SET_CODE, SET_DESC FROM APPS.D_CBS_SETTING WHERE SET_CODE = 'SMS_LOAN_LATE';
```

## Common Issues

| Issue | Error Message | Solution |
|-------|---------------|----------|
| View doesn't exist | ORA-00942: table or view does not exist | Create the view `stg.view_loan_late_reminder` |
| Column name wrong | ORA-00904: "DAY_DUE": invalid identifier | Check your view column names, use alias if needed |
| Settings table missing | ORA-00942: table or view does not exist | Create `APPS.D_CBS_SETTING` or adjust schema prefix |
| User permissions | ORA-01031: insufficient privileges | Ensure your Oracle user (STG) has SELECT access |

## Application Configuration

The Oracle connection is configured in `application-local.yaml`:

```yaml
datasource:
  oracle:
    enabled: true
    url: ${ORACLE_URL:jdbc:oracle:thin:@192.168.127.88:1521:stg}
    username: ${ORACLE_USERNAME:stg}
    password: ${ORACLE_PASSWORD:Bnk$$444}
    driver-class-name: oracle.jdbc.driver.OracleDriver
```

Ensure:
1. The connection URL, username, and password are correct
2. The user `stg` has SELECT privileges on `stg.view_loan_late_reminder` and `APPS.D_CBS_SETTING`
3. The Oracle database is reachable from your application server

## Notes

- Schema names in Oracle are case-sensitive when quoted but case-insensitive otherwise
- The application uses lowercase unquoted schema and object names (stg, view_loan_late_reminder, etc.)
- If your objects are in different schemas, update the Java helper classes accordingly
