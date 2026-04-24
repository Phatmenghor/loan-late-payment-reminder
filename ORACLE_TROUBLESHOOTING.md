# Oracle Troubleshooting Guide

If you encounter Oracle errors when running the application, follow these steps.

## Step 1: Verify Connection

Test basic Oracle connectivity:

```bash
sqlplus stg/Bnk$$444@192.168.127.88:1521:stg
```

If this fails:
- Check Oracle server is running
- Verify IP address and port (192.168.127.88:1521)
- Check firewall rules
- Verify credentials in `application-local.yaml`

## Step 2: List Available Objects

Once connected via SQLPlus, find your actual view and tables:

```sql
-- Find all views you have access to
SELECT VIEW_NAME FROM USER_VIEWS WHERE VIEW_NAME LIKE '%LOAN%' OR VIEW_NAME LIKE '%REMINDER%';

-- Find all tables you have access to
SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME LIKE '%CBS%' OR TABLE_NAME LIKE '%SETTING%';

-- List columns in your view
DESC view_loan_late_reminder;
-- or (if schema-qualified)
DESC stg.view_loan_late_reminder;
```

## Step 3: Check View Columns

The view MUST have these columns (exact names):
```sql
SELECT * FROM stg.view_loan_late_reminder WHERE ROWNUM <= 1;
```

Expected columns (copy column names exactly as shown):
- reportdate (or REPORTDATE)
- customerid (or CUSTOMERID)
- mbapp_phone (or MBAPP_PHONE)
- arrangement_id (or ARRANGEMENT_ID)
- **day_due** (or DAYDUE if not aliased)

If your view has `daydue` (no underscore) instead of `day_due`:
```sql
-- Create a new view with proper aliasing:
CREATE OR REPLACE VIEW stg.view_loan_late_reminder_v2 AS
SELECT
    reportdate,
    customerid,
    mbapp_phone,
    arrangement_id,
    daydue AS day_due  -- Add this alias!
FROM your_original_view;
```

## Step 4: Check PostgreSQL SMS Configuration

The SMS message is now stored in PostgreSQL, not Oracle. It's automatically created on first run:

```sql
-- Verify the SMS config exists
SELECT * FROM sms_config WHERE config_type = 'SMS_LOAN_LATE';

-- Update the message if needed
UPDATE sms_config 
SET config_value = 'Your new SMS message here'
WHERE config_type = 'SMS_LOAN_LATE';
```

## Step 5: Update Java Code if Schemas Differ

If your objects are in different schemas than expected:

### For OracleHelper.java:
Change this line:
```java
String query = "SELECT reportdate, customerid, mbapp_phone, arrangement_id, day_due " +
        "FROM stg.view_loan_late_reminder " +
        "WHERE TRUNC(reportdate) >= TRUNC(SYSDATE) - 1";
```

To match your schema:
```java
// If the view is in STG schema
// No change needed

// If view is in different schema (e.g., STAGING):
String query = "SELECT reportdate, customerid, mbapp_phone, arrangement_id, day_due " +
        "FROM STAGING.view_loan_late_reminder " +
        "WHERE TRUNC(reportdate) >= TRUNC(SYSDATE) - 1";
```

## Step 6: Common Errors and Fixes

| Error | Cause | Solution |
|-------|-------|----------|
| ORA-00942: table or view does not exist | Object doesn't exist or wrong schema | Run Step 2 to find actual object name |
| ORA-00904: invalid identifier | Column name is wrong | Run Step 3 to find actual column names |
| ORA-01031: insufficient privileges | User can't access the object | Grant SELECT privileges: `GRANT SELECT ON object TO stg;` |
| Connection refused | Network issue | Check IP, port, firewall |
| Bad username/password | Wrong credentials | Verify in application-local.yaml |

## Step 7: Test End-to-End

Once objects are set up correctly, test the API:

```bash
# Start the application
mvn spring-boot:run

# In another terminal, trigger SMS processing
curl -X POST http://localhost:8080/api/notification/process-sms
```

Monitor logs for:
- "BEGIN: SMS notification processing cycle"
- "View fetch successful: X records retrieved"
- "Queued: X records added with status=PENDING"
- "Delivery results:" with success count

## Getting Help

If you're still stuck:
1. Check application logs for the exact error message
2. Run the SQLPlus commands above to verify your schema
3. Ensure all objects use lowercase names (stg, view_loan_late_reminder, etc.)
4. Update Java code to match your actual schema names
5. Rebuild and test: `mvn clean package -DskipTests`
