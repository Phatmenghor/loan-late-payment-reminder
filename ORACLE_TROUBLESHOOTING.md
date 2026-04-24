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

## Step 4: Check Settings Table

Find where your SMS message is stored:

```sql
-- Search for settings tables
SELECT TABLE_NAME FROM ALL_TABLES WHERE TABLE_NAME LIKE '%SETTING%' OR TABLE_NAME LIKE '%CBS%';

-- Once found, check its structure
DESC APPS.D_CBS_SETTING;
-- or if in different schema:
DESC other_schema.D_CBS_SETTING;

-- Check if SMS_LOAN_LATE setting exists
SELECT * FROM APPS.D_CBS_SETTING WHERE SET_CODE = 'SMS_LOAN_LATE';
```

If the table/setting doesn't exist:

```sql
-- Create the table if it doesn't exist
CREATE TABLE APPS.D_CBS_SETTING (
    SET_CODE VARCHAR2(50) PRIMARY KEY,
    SET_DESC VARCHAR2(500),
    CREATED_DATE DATE DEFAULT SYSDATE
);

-- Insert the SMS message setting
INSERT INTO APPS.D_CBS_SETTING (SET_CODE, SET_DESC) 
VALUES ('SMS_LOAN_LATE', 'Your loan payment is overdue. Please contact your bank.');
COMMIT;
```

## Step 5: Update Java Code if Schemas Differ

If your objects are in different schemas than expected:

### For CpbHelper.java:
Change this line:
```java
PreparedStatement ps = con.prepareStatement("SELECT SET_DESC FROM APPS.D_CBS_SETTING WHERE SET_CODE='SMS_LOAN_LATE'")
```

To match your schema:
```java
// If APPS.D_CBS_SETTING
// No change needed

// If CFS.D_CBS_SETTING
PreparedStatement ps = con.prepareStatement("SELECT SET_DESC FROM CFS.D_CBS_SETTING WHERE SET_CODE='SMS_LOAN_LATE'")

// If in user schema (just D_CBS_SETTING)
PreparedStatement ps = con.prepareStatement("SELECT SET_DESC FROM D_CBS_SETTING WHERE SET_CODE='SMS_LOAN_LATE'")
```

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
