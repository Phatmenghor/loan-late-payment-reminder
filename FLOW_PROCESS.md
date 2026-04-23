# SMS Loan Late Payment Reminder - Flow Process

## 📋 Overview

This is a **single-endpoint SMS notification system** that processes pending SMS requests from Oracle database, sends them to CPB API gateway, and logs results to PostgreSQL for audit trail.

---

## 🔄 Complete Process Flow

### **Single Endpoint**
```
POST /api/v1/notifications/sms/process-pending
```

---

## 📊 Step-by-Step Flow

### **Step 1: SELECT Pending SMS from Oracle**
```
Database: Oracle (D_Cbs_Sms_Log_Test)
Query:    SELECT Tell FROM D_Cbs_Sms_Log_Test 
          WHERE Sms_Status != 'SVC-SUCCESS-00'
Result:   List of phone numbers pending SMS delivery
```

**What happens:**
- System retrieves all phone numbers that haven't received successful SMS yet
- Records with `Sms_Status = 'SVC-SUCCESS-00'` are skipped (already processed)

---

### **Step 2: SEND SMS to CPB API**
```
API Endpoint: http://10.18.1.47:9393/SendOTT
Method:       POST
Payload:      {
                "phone": "0855123456",
                "content": "Loan payment reminder",
                "signKey": "<generated_key>"
              }
Response:     SVC-SUCCESS-00 or SVC-FAILED
```

**What happens:**
- For each phone number, system builds JSON payload
- Generates signature key using encryption
- Sends POST request to CPB API
- Receives status response (success or failure)

---

### **Step 3: UPDATE Status in Oracle**
```
Database: Oracle (D_Cbs_Sms_Log_Test)
Query:    UPDATE D_Cbs_Sms_Log_Test 
          SET Sms_Status = ?, 
              Sms_Log_Dt = NOW()
          WHERE Tell = ?
Result:   Oracle record updated with API response status
```

**What happens:**
- Updates the SMS status in Oracle with the API response
- Records the exact timestamp of processing
- Marks as success or failed based on API response

---

### **Step 4: LOG to PostgreSQL (Audit Trail)**
```
Database: PostgreSQL (loan_sms_log)
Record:   {
            id: <UUID>,
            phoneNumber: "0855123456",
            messageContent: "Loan payment reminder",
            smsStatus: "SVC-SUCCESS-00",
            smsLogDate: "2026-04-23 10:30:45"
          }
Result:   Audit trail created for monitoring
```

**What happens:**
- Creates permanent audit record in PostgreSQL
- Tracks which phones received SMS (success/failure)
- Enables reporting and monitoring

---

## 🗄️ Database Architecture

### **Oracle Database** (D_Cbs_Sms_Log_Test)
- **Purpose:** Production SMS record source of truth
- **Operations:** SELECT & UPDATE
- **Data:** Phone numbers, SMS status, timestamps

### **PostgreSQL Database** (loan_sms_log)
- **Purpose:** Audit trail and monitoring
- **Operations:** INSERT only (logs)
- **Data:** Success/failure records, timestamps, phone numbers

---

## 🔄 Complete Example Flow

```
CALL: POST /api/v1/notifications/sms/process-pending

1. ORACLE SELECT
   ├─ Phone: 0855123456
   ├─ Phone: 0855789012
   └─ Phone: 0855456789

2. For each phone → SEND to API
   
   Phone 0855123456:
   ├─ Build JSON payload
   ├─ Send to CPB API
   ├─ Response: SVC-SUCCESS-00
   ├─ ORACLE UPDATE: Status = SVC-SUCCESS-00
   └─ POSTGRES LOG: Created success record
   
   Phone 0855789012:
   ├─ Build JSON payload
   ├─ Send to CPB API
   ├─ Response: SVC-FAILED
   ├─ ORACLE UPDATE: Status = SVC-FAILED
   └─ POSTGRES LOG: Created failure record
   
   Phone 0855456789:
   ├─ Build JSON payload
   ├─ Send to CPB API
   ├─ Response: SVC-SUCCESS-00
   ├─ ORACLE UPDATE: Status = SVC-SUCCESS-00
   └─ POSTGRES LOG: Created success record

3. FINAL RESULT
   ├─ Total: 3 SMS
   ├─ Success: 2
   └─ Failed: 1
```

---

## 📈 Monitoring & Reporting

### **Query Success Rate**
```sql
SELECT 
  COUNT(*) as total_sms,
  SUM(CASE WHEN sms_status = 'SVC-SUCCESS-00' THEN 1 ELSE 0 END) as success,
  SUM(CASE WHEN sms_status != 'SVC-SUCCESS-00' THEN 1 ELSE 0 END) as failed,
  ROUND(100.0 * SUM(CASE WHEN sms_status = 'SVC-SUCCESS-00' THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate
FROM loan_sms_log
WHERE sms_log_date >= NOW() - INTERVAL '1 day';
```

### **View Failed SMS**
```sql
SELECT phone_number, sms_status, sms_log_date, message_content
FROM loan_sms_log
WHERE sms_status != 'SVC-SUCCESS-00'
ORDER BY sms_log_date DESC
LIMIT 50;
```

### **Track by Phone Number**
```sql
SELECT 
  phone_number,
  COUNT(*) as total_attempts,
  SUM(CASE WHEN sms_status = 'SVC-SUCCESS-00' THEN 1 ELSE 0 END) as success_count,
  MAX(sms_log_date) as last_attempt
FROM loan_sms_log
GROUP BY phone_number
ORDER BY last_attempt DESC;
```

---

## 🛠️ Configuration

### **Environment Variables**
```
SERVER_PORT=9393
API_URL=http://10.18.1.47:9393
ENCRYPTION_KEY=OTTCPB!!BBDUNG#LOGFH$837990
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### **API Configuration** (application.yaml)
```yaml
spring:
  application:
    name: sms-loan-late
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

server:
  port: 9393

cpb:
  api:
    url: ${API_URL}
    encryption-key: ${ENCRYPTION_KEY}

datasource:
  oracle:
    url: jdbc:oracle:thin:@192.168.102.5:1521:dwh
    username: dwh
    password: Bnk$$444
```

---

## 📱 API Usage

### **Request**
```bash
curl -X POST http://10.18.1.47:9393/api/v1/notifications/sms/process-pending \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic admin:admin123"
```

### **Response**
```json
{
  "success": true,
  "message": "Pending SMS notifications are being processed",
  "data": "ACCEPTED",
  "timestamp": "2026-04-23T10:30:45.123Z"
}
```

---

## 🔐 Security

- **Authentication:** HTTP Basic Auth (username/password)
- **Encryption:** SHA-256 sign key generation
- **Database:** Separate Oracle and PostgreSQL connections
- **Validation:** Phone number format validation (10-20 digits)

---

## 📊 Logging

All operations are logged with timestamps and status:

```
✓ Found 3 pending SMS notifications to process
========== START: Processing SMS for phone: 0855123456 ==========
API: Sending request to: http://10.18.1.47:9393/SendOTT
API: Received response status: SVC-SUCCESS-00
✓ SMS sent successfully | Phone: 0855123456 | Status: SVC-SUCCESS-00
PostgreSQL: Audit log created for phone: 0855123456 | Status: SVC-SUCCESS-00
========== END: SMS processing completed for phone: 0855123456 ==========

========== RESULT: Success: 2, Failed: 1 ==========
========== END: Processing pending SMS notifications ==========
```

---

## 🔧 Technology Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.4.1
- **Primary Database:** PostgreSQL (audit logs)
- **Secondary Database:** Oracle (SMS operations)
- **API Gateway:** CPB SMS Gateway
- **Logging:** SLF4J/Logback
- **Build:** Maven 3.9

---

## 📞 Support

### **Issues to Check**
1. **Oracle connectivity:** Verify D_Cbs_Sms_Log_Test table exists
2. **API connectivity:** Test CPB API endpoint manually
3. **PostgreSQL:** Verify loan_sms_log table exists
4. **Authentication:** Check basic auth credentials

### **Troubleshooting**
```
Error: "No pending SMS found"
→ Check Oracle database for records with Sms_Status != 'SVC-SUCCESS-00'

Error: "API Request failed"
→ Verify API URL and encryption key configuration

Error: "Failed to create audit log"
→ Check PostgreSQL connection and loan_sms_log table permissions
```

---

## 📝 Version

- **Version:** 1.0.0
- **Last Updated:** 2026-04-23
- **Status:** Production Ready
