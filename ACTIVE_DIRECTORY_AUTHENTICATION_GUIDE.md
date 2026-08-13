# Active Directory Authentication, API Key Management & Error Handling Guide

This guide is the authoritative technical reference for the **Internal Active Directory (AD) Authentication & System Integration Service**. It details the complete authentication lifecycle, system API Key management, Active Directory LDAP bind flows, error message resolution for all 23 AD/LDAP error codes, database audit logging, UTF-8 NUL byte protection, and Real Client IP resolution.

---

## 1. End-to-End Authentication Architecture & Lifecycle

```
+------------------+         +--------------------------+         +---------------------------+         +------------------------------+
| Client App / API |         | Spring Boot REST Filter  |         |   AdAuthServiceImpl       |         | Active Directory DC          |
| (e.g. DWH_BI)    |         | (RequestIdFilter)        |         |                           |         | (ldap://adcpbank.com)        |
+--------+---------+         +------------+-------------+         +-------------+-------------+         +--------------+---------------+
         |                                |                                     |                                      |
         | POST /api/v1/auth/login        |                                     |                                      |
         | Headers: X-API-Key: ad_sk_...  |                                     |                                      |
         | Body: {username, password}     |                                     |                                      |
         +------------------------------->|                                     |                                      |
         |                                | Resolves traceId & Real Client IP   |                                      |
         |                                | Sets SLF4J MDC Context              |                                      |
         |                                +------------------------------------>|                                      |
         |                                                                      | 1. Validate X-API-Key Header         |
         |                                                                      | 2. Check API Key Status (ACTIVE)     |
         |                                                                      | 3. Check AD Enabled Status Flag      |
         |                                                                      | 4. Attempt LDAP Bind & Search        |
         |                                                                      +------------------------------------->|
         |                                                                      |                                      | LDAP Bind (username@adcpbank.com)
         |                                                                      |                                      | Search sAMAccountName
         |                                                                      |<-------------------------------------+
         |                                                                      | Returns LDAP Context / SearchResult  |
         |                                                                      | (or NamingException with Data Code)  |
         |                                                                      |                                      |
         |                                                                      | 5. If Success:                       |
         |                                                                      |    Trigger Async User Cache Update   |
         |                                                                      |    Trigger Async Audit Log (ad_log)  |
         |                                                                      |                                      |
         |                                                                      | 6. If NamingException:               |
         |                                                                      |    Pass raw error to LdapAdErrorUtils |
         |                                                                      |    Map to Long Friendly Message      |
         |                                                                      |    Sanitize UTF-8 0x00 NUL Bytes     |
         |                                                                      |    Trigger Async Audit Failure Log   |
         |                                                                      |                                      |
         | HTTP 200 OK (Success) / 401 Unauthorized (Failure Message)          |                                      |
         |<---------------------------------------------------------------------+                                      |
```

---

## 2. AD System API Key Management (`X-API-Key`)

To call the Active Directory Authentication endpoint, calling systems (such as `dwh_bi` or `LPC_SYSTEM`) must provide a valid system API Key in the `X-API-Key` HTTP header.

### 2.1. API Key Endpoints Summary (`/api/v1/auth/api-keys`)

> [!NOTE]
> All API Key management endpoints are protected by **HTTP Basic Authentication** (Admin level access).

| Method | Endpoint | Description | Request Body / Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/api-keys` | Generate a new AD System API Key | `{ "label": "dwh_bi" }` (Basic Auth) |
| `GET` | `/api/v1/auth/api-keys` | List all active AD System API Keys | None (Basic Auth) |
| `GET` | `/api/v1/auth/api-keys/{id}` | Get API Key details by UUID | None (Basic Auth) |
| `PATCH` | `/api/v1/auth/api-keys/{id}/status` | Lock / Unlock an API Key (`ACTIVE` / `INACTIVE`) | `{ "status": "INACTIVE" }` (Basic Auth) |
| `DELETE` | `/api/v1/auth/api-keys/{id}` | Revoke / Soft-delete an API Key | None (Basic Auth) |
| `GET` | `/api/v1/auth/api-keys/{id}/users` | List all AD users mapped to an API Key | Query params: `status`, `search`, `page`, `size` (Basic Auth) |
| `GET` | `/api/v1/auth/api-keys/{id}/users/summary` | Get user count statistics (Total, Active, Locked, Deleted) | None (Basic Auth) |
| `PATCH` | `/api/v1/auth/api-keys/users/{id}/status` | Admin update mapped AD user status (`ACTIVE`/`LOCKED`/`DELETED`) | `{ "status": "ACTIVE", "reason": "..." }` (Basic Auth) |
| `DELETE` | `/api/v1/auth/api-keys/users/{id}` | Soft-delete mapped AD user | None (Basic Auth) |
| `POST` | `/api/v1/auth/users/unlock` | **External API**: Unlock a locked AD user on an API Key | `{ "username": "..." }` + `X-API-Key` |
| `PATCH` | `/api/v1/auth/users/status` | **External API**: Update user status on an API Key | `{ "username": "...", "status": "ACTIVE" }` + `X-API-Key` |
| `GET` | `/api/v1/auth/api-keys/ad-config` | Get current AD configuration (`adEnabled` flag) | None (Basic Auth) |
| `PUT` | `/api/v1/auth/api-keys/ad-config` | Update AD configuration (`adEnabled: true/false`) | `{ "adEnabled": true }` (Basic Auth) |


---

### 2.2. Generating a New API Key

**Request**:
```http
POST /api/v1/auth/api-keys
Authorization: Basic YWRtaW46YWRtaW4xMjM=
Content-Type: application/json

{
  "label": "dwh_bi"
}
```

**Response (HTTP 200 OK)**:
```json
{
  "status": "success",
  "message": "AD API Key generated successfully",
  "data": {
    "id": "3a7b91c2-9e4f-4a12-b893-112233445566",
    "apiKey": "ad_sk_dwhbi_9k4nEadAsvH_4YQTw9B8u76dJxYavumd",
    "label": "dwh_bi",
    "status": "ACTIVE",
    "createdAt": "2026-08-05T11:00:00"
  }
}
```

---

### 2.3. Locking / Unlocking an API Key

If an application's access needs to be temporarily revoked or locked, update its status to `INACTIVE`.

**Request**:
```http
PATCH /api/v1/auth/api-keys/3a7b91c2-9e4f-4a12-b893-112233445566/status
Authorization: Basic YWRtaW46YWRtaW4xMjM=
Content-Type: application/json

{
  "status": "INACTIVE"
}
```

**Response (HTTP 200 OK)**:
```json
{
  "status": "success",
  "message": "AD API Key status updated successfully",
  "data": {
    "id": "3a7b91c2-9e4f-4a12-b893-112233445566",
    "label": "dwh_bi",
    "status": "INACTIVE"
  }
}
```

When an application calls login using an `INACTIVE` key, the service immediately rejects authentication:
```json
HTTP/1.1 401 Unauthorized

{
  "success": false,
  "message": "API Key for application [dwh_bi] is inactive or locked"
}
```

---

---

### 2.4. Mapped AD User Bucket (`ad_system_users`) & Statistics

Every time an Active Directory user logs in using an `X-API-Key`, a unique user mapping unit is automatically created or updated in the `ad_system_users` table (unique per `apiKey` + `username`). This allows administrators to track:
- Which users belong to which API Key / application.
- Total users per API key, active users, locked users, and deleted users.
- Exact `lastLoginAt` timestamp for each user on each API key.
- Previous `lastLoginAt` returned upon login.

---

### 2.5. 90-Day Inactivity Auto-Lock Rule & Policy

To comply with banking security standards:
1. **Login-Time Detection**: When a user attempts to log in, if their last login was more than 90 days ago (or created > 90 days ago without logging in), their status is automatically set to `LOCKED` with reason `"Inactive for more than 90 days"`, and login is rejected:
   ```json
   HTTP/1.1 401 Unauthorized

   {
     "success": false,
     "message": "Your account has been locked for application [dwh_bi] due to inactivity exceeding 90 days. Please unlock your account to proceed."
   }
   ```
2. **Daily Background Cron Job**: Every day at 1:00 AM (Cambodia Time), `AdUserInactivitySchedulerService` automatically scans all active mapped users in `ad_system_users` and transitions inactive accounts (> 90 days) to `LOCKED`.

---

### 2.6. External User Unlock API (`POST /api/v1/auth/users/unlock`)

External client applications (such as DWH, LPC, etc.) can unlock their own users using their system `X-API-Key` header.

**Request**:
```http
POST /api/v1/auth/users/unlock
X-API-Key: ad_sk_dwhbi_9k4nEadAsvH_4YQTw9B8u76dJxYavumd
Content-Type: application/json

{
  "username": "bunleng.kong",
  "reason": "User returned from leave - unlocked by DWH Admin"
}
```

**Response (HTTP 200 OK)**:
```json
{
  "status": "success",
  "message": "User account unlocked successfully to ACTIVE status",
  "data": {
    "id": "7b8c9d0e-1f2a-3b4c-5d6e-7f8a9b0c1d2e",
    "apiKey": "ad_sk_dwhbi_9k4nEadAsvH_4YQTw9B8u76dJxYavumd",
    "username": "bunleng.kong",
    "displayName": "Bunleng Kong",
    "status": "ACTIVE",
    "lastLoginAt": "2026-08-13T10:00:00",
    "lockReason": "Unlocked: User returned from leave - unlocked by DWH Admin",
    "lockedAt": null
  }
}
```

---

## 3. Active Directory User Login Flow (`/api/v1/auth/login`)

**Endpoint**: `POST /api/v1/auth/login`  
**Required Header**: `X-API-Key: ad_sk_dwhbi_9k4nEadAsvH_4YQTw9B8u76dJxYavumd`

### 3.1. Successful Login Request

**Request**:
```http
POST /api/v1/auth/login HTTP/1.1
Host: internal-service.cpbank.com
X-API-Key: ad_sk_dwhbi_9k4nEadAsvH_4YQTw9B8u76dJxYavumd
Content-Type: application/json

{
  "username": "bunleng.kong",
  "password": "CorrectPassword123!"
}
```

**Response (HTTP 200 OK)**:
```json
{
  "success": true,
  "message": "Authentication successful",
  "adUser": {
    "samaccountName": "bunleng.kong",
    "displayName": "Bunleng Kong",
    "cn": "Bunleng Kong",
    "givenName": "Bunleng",
    "sn": "Kong",
    "mail": "bunleng.kong@cpbank.com.kh",
    "department": "Information Technology",
    "title": "Software Engineer",
    "telephoneNumber": "+855 23 999 888",
    "mobile": "+855 12 345 678",
    "company": "Canadia Bank / CP Bank",
    "distinguishedName": "CN=Bunleng Kong,OU=IT,DC=adcpbank,DC=com",
    "memberOf": [
      "CN=IT_Department,OU=Groups,DC=adcpbank,DC=com",
      "CN=Domain Users,CN=Users,DC=adcpbank,DC=com"
    ]
  }
}
```

---

## 4. Comprehensive AD & LDAP Error Message Reference Table

All 23 Active Directory Win32 data sub-codes, SSPI/Kerberos error codes, and standard LDAP result codes are mapped via [`LdapAdErrorUtils.java`](file:///d:/CP%20Bank/loan-late-payment-reminder/src/main/java/com/backend/features/auth/util/LdapAdErrorUtils.java):

| AD / LDAP Code | Error Category | Technical Meaning | Root Cause / Context | Long Friendly User Response Message |
| :---: | :---: | :--- | :--- | :--- |
| **775** | AD Security | Account Locked | Exceeded maximum failed password attempts | `"Your Active Directory account is locked due to multiple failed login attempts. Please contact IT Support to unlock your account."` |
| **533** | AD Security | Account Disabled | Account disabled by AD administrator | `"Your Active Directory account has been disabled by an administrator. Please contact IT Support for assistance."` |
| **532** | AD Security | Password Expired | Account password expiration date passed | `"Your Active Directory password has expired or must be reset before logging in. Please update your password and try again."` |
| **773** | AD Security | Must Reset Password | Password change enforced by AD policy | `"Your Active Directory password has expired or must be reset before logging in. Please update your password and try again."` |
| **701** | AD Security | Account Expired | Account expiration date reached | `"Your Active Directory account has expired. Please contact IT Support or your system administrator to extend your account access."` |
| **525** | AD Account | User Not Found | sAMAccountName / UPN does not exist | `"The specified Active Directory username was not found. Please verify your username or User Principal Name (UPN) and try again."` |
| **52e** | AD Account | Invalid Credentials | Incorrect password or username | `"Invalid Active Directory credentials. Please double-check your username and password and try again."` |
| **530** | AD Policy | Logon Hours Restricted | Attempted login outside permitted schedule | `"Active Directory login is currently restricted at this time based on account logon hours policy. Please contact your system administrator."` |
| **531** | AD Policy | Workstation Restricted | Attempted login from unauthorized PC/IP | `"Active Directory login is restricted from this workstation or IP address. Please log in from an authorized workstation or contact IT Support."` |
| **568** | AD Policy | Too Many SIDs | Exceeded SID / group membership limit | `"Active Directory login failed due to too many security identifiers (group memberships). Please contact IT Support to review your group memberships."` |
| **5320** | AD Policy | Smart Card Required | Smart Card authentication enforced | `"Smart card authentication is required for this Active Directory account. Please log in using your smart card."` |
| **80090324** | Kerberos / SSPI | Clock Skew Too Great | Client and DC time mismatch > 5 min | `"System clock skew between client and Active Directory Domain Controller is too great. Please synchronize system time and try again."` |
| **80090322** | Kerberos / SSPI | Target Principal Mismatch | SPN / Kerberos configuration error | `"Target principal or SPN mismatch error during Active Directory authentication. Please contact IT Support."` |
| **80090311** | Domain Controller | No Auth Authority | Domain Controller unavailable or offline | `"Active Directory Domain Controller is currently unavailable. Please verify domain network connectivity or contact IT Support."` |
| **8009030C** | Kerberos / SSPI | Security Package Error | SSPI / Kerberos package domain error | `"Security package or Kerberos authentication error occurred during Active Directory login. Please verify domain connectivity."` |
| **80090308** | LDAP | Authentication Failed | Generic LDAP bind authentication failure | `"Active Directory LDAP authentication failed. Please verify your credentials or contact system administrator."` |
| **34** | LDAP Result Code | Invalid DN Syntax | Malformed Distinguished Name string | `"Invalid Distinguished Name (DN) syntax in Active Directory request. Please contact system administrator."` |
| **32** | LDAP Result Code | No Such Object | Target object or Base DN not found | `"The requested Active Directory object or Base DN was not found. Please contact system administrator."` |
| **50** | LDAP Result Code | Insufficient Access | Permission denied for operation | `"Insufficient permissions to perform Active Directory operation. Please contact system administrator."` |
| **53** | LDAP Result Code | Unwilling to Perform | AD policy restriction | `"Active Directory server is unwilling to perform the requested operation due to policy restrictions."` |
| **68** | LDAP Result Code | Entry Already Exists | Duplicate object entry | `"The Active Directory entry already exists. Please contact system administrator."` |
| **19** | LDAP Result Code | Constraint Violation | Password / attribute policy violation | `"Active Directory constraint violation occurred. Please check account attribute policy requirements."` |
| **49** | LDAP Result Code | Invalid Credentials | Generic LDAP Code 49 failure | `"Invalid Active Directory username or password. Please verify your credentials."` |

---

## 5. Technical Protection & Utility Implementations

### 5.1. UTF-8 NUL Byte (`0x00`) Sanitization (`LdapAdErrorUtils`)
PostgreSQL text and varchar columns reject NUL bytes (`\u0000` / `0x00`) with:
`ERROR: invalid byte sequence for encoding "UTF8": 0x00`

The system automatically sanitizes all raw exception strings, usernames, trace IDs, and JSON attributes via `LdapAdErrorUtils.sanitizeUtf8`:
```java
public static String sanitizeUtf8(String input) {
    if (input == null) return null;
    return input.replace("\u0000", "")
                .replace("\0", "")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
}
```

### 5.2. Real Client IP Resolution (`ClientIpUtils`)
To capture the actual user client IP across Nginx, HAProxy, Cloudflare, load balancers, and Docker networks, `ClientIpUtils` performs multi-pass header scanning:
- Scans `X-Forwarded-For`, `X-Real-IP`, `CF-Connecting-IP`, `True-Client-IP`, `X-Client-IP`, etc.
- Bypasses local loopback (`127.0.0.1`, `::1`) and internal Docker network IPs (`172.17.0.1`) to resolve true caller IP addresses (e.g. `10.18.1.131`).

### 5.3. MDC Trace ID Correlation (`AdAuthAsyncServiceImpl`)
- `traceId` values are passed into background `@Async` execution pools (`saveAdLogAsync` & `saveOrUpdateUserCacheAsync`).
- Executed via `MDC.put("traceId", traceId)` with `finally { MDC.remove("traceId"); }` cleanup to guarantee log correlation without `[traceId=N/A]` entries.

---

## 6. Verification & Build Status

The complete implementation has been compiled and verified with automated test suites:
- Compilation Command: `.\mvnw.cmd compile` -> **`BUILD SUCCESS`** (116 source files compiled, 0 errors).
- Test Execution Command: `.\mvnw.cmd test` -> **`BUILD SUCCESS`** (10/10 tests passed).
  - `AdApiKeyUserServiceTest`: 4 tests passed (unlocking, manual locking, 90-day inactivity locking, user count summaries).
  - `AdAuthServiceTest`: 3 tests passed (login lock enforcement, 90-day inactivity auto-locking, missing API key rejection).
  - `AdApiKeyUserControllerTest`: 3 tests passed (external unlock API, external status update API, admin user summary API).
