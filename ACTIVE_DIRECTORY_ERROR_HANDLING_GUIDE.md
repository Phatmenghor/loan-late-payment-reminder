# Active Directory Authentication & LDAP Error Handling Guide

This document provides a comprehensive reference for the Active Directory (AD) authentication service, LDAP error code mappings, user-friendly error message resolution, UTF-8 NUL byte sanitization, and Real Client IP resolution.

---

## 1. Executive Summary & Features

The authentication system integrates Spring Boot with Active Directory via Java Naming and Directory Interface (JNDI / LDAP). It provides:
- **Comprehensive AD & LDAP Error Mapping**: All 23 common Active Directory Win32 data sub-codes, SSPI/Kerberos codes, and standard LDAP result codes are translated into long, user-friendly, professional messages.
- **Security Policy Enforcement**: Accounts flagged with critical AD security status (e.g. Account Lockout `775`, Account Disabled `533`, Password Expired `532`/`773`, Account Expired `701`) immediately return friendly lockout messages without bypassing AD policies through local cache fallback.
- **PostgreSQL UTF-8 NUL Byte (`0x00`) Protection**: Automatic string sanitization (`sanitizeUtf8`) strips `\u0000` / `0x00` characters from error strings and JSON attributes before database persistence into `ad_log` and `ad_user_cache`.
- **Real Client IP Resolution**: Multi-pass proxy header scanning in `ClientIpUtils` automatically filters out local loopback (`127.0.0.1`) and internal Docker container bridge IPs (`172.17.0.1`) to resolve actual caller IP addresses across reverse proxies (Nginx, Traefik, HAProxy), Cloudflare, and load balancers.
- **Clean Single-Line Logging & MDC Trace Propagation**: Trace IDs (`traceId`) are automatically propagated into background `@Async` threads, ensuring log lines display matching `traceId` values without duplicate parameter outputs.

---

## 2. Complete Active Directory & LDAP Error Code Reference

| AD / LDAP Code | Error Category | Technical Meaning | Root Cause / Context | User-Friendly Response Message |
| :---: | :---: | :--- | :--- | :--- |
| **775** | AD Security | Account Locked | Too many failed login attempts | `"Your Active Directory account is locked due to multiple failed login attempts. Please contact IT Support to unlock your account."` |
| **533** | AD Security | Account Disabled | Account disabled by AD administrator | `"Your Active Directory account has been disabled by an administrator. Please contact IT Support for assistance."` |
| **532** | AD Security | Password Expired | Account password has expired | `"Your Active Directory password has expired or must be reset before logging in. Please update your password and try again."` |
| **773** | AD Security | User Must Reset Password | Password reset required by AD policy | `"Your Active Directory password has expired or must be reset before logging in. Please update your password and try again."` |
| **701** | AD Security | Account Expired | Account expiration date reached | `"Your Active Directory account has expired. Please contact IT Support or your system administrator to extend your account access."` |
| **525** | AD Account | User Not Found | sAMAccountName / UPN does not exist | `"The specified Active Directory username was not found. Please verify your username or User Principal Name (UPN) and try again."` |
| **52e** | AD Account | Invalid Credentials | Incorrect password or username | `"Invalid Active Directory credentials. Please double-check your username and password and try again."` |
| **530** | AD Policy | Logon Hours Restricted | Attempted login outside allowed hours | `"Active Directory login is currently restricted at this time based on account logon hours policy. Please contact your system administrator."` |
| **531** | AD Policy | Workstation Restricted | Attempted login from unauthorized PC | `"Active Directory login is restricted from this workstation or IP address. Please log in from an authorized workstation or contact IT Support."` |
| **568** | AD Policy | Too Many Security Identifiers | Group membership limit exceeded | `"Active Directory login failed due to too many security identifiers (group memberships). Please contact IT Support to review your group memberships."` |
| **5320** | AD Policy | Smart Card Required | Account requires Smart Card auth | `"Smart card authentication is required for this Active Directory account. Please log in using your smart card."` |
| **80090324** | Kerberos / SSPI | Clock Skew Too Great | Client/Server time mismatch > 5 min | `"System clock skew between client and Active Directory Domain Controller is too great. Please synchronize system time and try again."` |
| **80090322** | Kerberos / SSPI | Target Principal Incorrect | SPN / Kerberos mismatch | `"Target principal or SPN mismatch error during Active Directory authentication. Please contact IT Support."` |
| **80090311** | Domain Controller | No Authentication Authority | Domain Controller unreachable | `"Active Directory Domain Controller is currently unavailable. Please verify domain network connectivity or contact IT Support."` |
| **8009030C** | Kerberos / SSPI | Security Package Error | SSPI / Kerberos domain error | `"Security package or Kerberos authentication error occurred during Active Directory login. Please verify domain connectivity."` |
| **80090308** | LDAP | Authentication Failed | Generic LDAP bind failure | `"Active Directory LDAP authentication failed. Please verify your credentials or contact system administrator."` |
| **34** | LDAP Result Code | Invalid DN Syntax | Malformed Distinguished Name | `"Invalid Distinguished Name (DN) syntax in Active Directory request. Please contact system administrator."` |
| **32** | LDAP Result Code | No Such Object | DN or Base DN not found | `"The requested Active Directory object or Base DN was not found. Please contact system administrator."` |
| **50** | LDAP Result Code | Insufficient Access | Permission denied for operation | `"Insufficient permissions to perform Active Directory operation. Please contact system administrator."` |
| **53** | LDAP Result Code | Unwilling to Perform | AD policy restriction | `"Active Directory server is unwilling to perform the requested operation due to policy restrictions."` |
| **68** | LDAP Result Code | Entry Already Exists | Duplicate object entry | `"The Active Directory entry already exists. Please contact system administrator."` |
| **19** | LDAP Result Code | Constraint Violation | Password / attribute policy violation | `"Active Directory constraint violation occurred. Please check account attribute policy requirements."` |
| **49** | LDAP Result Code | Invalid Credentials | Generic LDAP Code 49 failure | `"Invalid Active Directory username or password. Please verify your credentials."` |

---

## 3. Architecture & Key Components

### 3.1. Centralized Error Resolver & Sanitizer (`LdapAdErrorUtils`)
Located at [`com.backend.features.auth.util.LdapAdErrorUtils`](file:///d:/CP%20Bank/loan-late-payment-reminder/src/main/java/com/backend/features/auth/util/LdapAdErrorUtils.java):

```java
// Sanitize UTF-8 text to strip NUL bytes (0x00) and normalize line breaks
String cleanMsg = LdapAdErrorUtils.sanitizeUtf8(rawErrorMessage);

// Resolve friendly long error message for end-users
String userMessage = LdapAdErrorUtils.resolveFriendlyErrorMessage(rawErrorMessage);
```

### 3.2. Authentication Service Logic (`AdAuthServiceImpl`)
Located at [`com.backend.features.auth.service.impl.AdAuthServiceImpl`](file:///d:/CP%20Bank/loan-late-payment-reminder/src/main/java/com/backend/features/auth/service/impl/AdAuthServiceImpl.java):
1. **API Key Verification**: Validates `X-API-Key` HTTP header against database records.
2. **AD Connection & Attribute Extraction**: Connects via JNDI `InitialLdapContext` and fetches user attributes.
3. **Security Policy Enforcement**: When LDAP throws `NamingException`, checks for critical security sub-codes (`775`, `533`, `532`, `701`, etc.). If present, returns the friendly lockout message directly without invoking local cache fallback.
4. **Fallback Authentication**: If AD server is offline (`80090311` or connection timeout), attempts fallback to encrypted local user cache (`ad_user_cache`).

### 3.3. Async Audit Logging & MDC Propagation (`AdAuthAsyncServiceImpl`)
Located at [`com.backend.features.auth.service.impl.AdAuthAsyncServiceImpl`](file:///d:/CP%20Bank/loan-late-payment-reminder/src/main/java/com/backend/features/auth/service/impl/AdAuthAsyncServiceImpl.java):
- `@Async` methods (`saveAdLogAsync` & `saveOrUpdateUserCacheAsync`) inherit `traceId` from HTTP thread via `MDC.put("traceId", traceId)`.
- Prevents `[traceId=N/A]` in logs and guarantees trace correlation across async background operations.

### 3.4. Real Client IP Resolution (`ClientIpUtils`)
Located at [`com.backend.shared.utils.ClientIpUtils`](file:///d:/CP%20Bank/loan-late-payment-reminder/src/main/java/com/backend/shared/utils/ClientIpUtils.java):
Scans proxy headers (`X-Forwarded-For`, `X-Real-IP`, `CF-Connecting-IP`, `True-Client-IP`, `X-Client-IP`, etc.). Filters out local loopback (`127.0.0.1`, `::1`) and internal Docker network IPs (`172.17.0.1`) to resolve the true caller IP address.

---

## 4. Example API Response Payloads

### 4.1. Account Locked (`data 775`)
```json
HTTP/1.1 401 Unauthorized
X-Trace-ID: 0558f21b-14bd-4da8-a65d-2c1113508eca
Content-Type: application/json

{
  "success": false,
  "message": "Your Active Directory account is locked due to multiple failed login attempts. Please contact IT Support to unlock your account."
}
```

### 4.2. Account Disabled (`data 533`)
```json
HTTP/1.1 401 Unauthorized
X-Trace-ID: 9e0f9157-9f4f-41ef-99d4-d4d3d9e89d9d
Content-Type: application/json

{
  "success": false,
  "message": "Your Active Directory account has been disabled by an administrator. Please contact IT Support for assistance."
}
```

### 4.3. Password Expired (`data 532` / `data 773`)
```json
HTTP/1.1 401 Unauthorized
X-Trace-ID: 78bfd6fc-13c2-4f24-9e7d-665e7c749fdb
Content-Type: application/json

{
  "success": false,
  "message": "Your Active Directory password has expired or must be reset before logging in. Please update your password and try again."
}
```

---

## 5. Console Log Output Examples

### Successful Login:
```text
11:10:45.123 [http-nio-5050-exec-1] [traceId=4b66a96f-b377-4ba6-b1a5-871e2a48aa68] INFO  c.b.f.a.s.i.AdAuthServiceImpl - Processing AD authentication attempt for user: bo.dpn from IP: 10.18.1.131 [appName key=ad_sk_dwhbi...]
11:10:45.138 [http-nio-5050-exec-1] [traceId=4b66a96f-b377-4ba6-b1a5-871e2a48aa68] INFO  c.b.f.a.s.i.AdAuthServiceImpl - AD authentication successful for user: bo.dpn from system: dwh_bi [IP=10.18.1.131]
11:10:45.138 [test-async-1] [traceId=4b66a96f-b377-4ba6-b1a5-871e2a48aa68] INFO  c.b.f.a.s.i.AdAuthAsyncServiceImpl - ASYNC: Updating local user cache for user: bo.dpn
11:10:45.139 [http-nio-5050-exec-1] [traceId=4b66a96f-b377-4ba6-b1a5-871e2a48aa68] INFO  c.b.s.l.RequestIdFilter - POST /api/v1/auth/login → 200 in 16ms
```

### Account Locked Out (`data 775`):
```text
11:12:01.450 [http-nio-5050-exec-2] [traceId=0558f21b-14bd-4da8-a65d-2c1113508eca] INFO  c.b.f.a.s.i.AdAuthServiceImpl - Processing AD authentication attempt for user: bunleng.kong from IP: 10.18.1.131 [appName key=ad_sk_dwhbi...]
11:12:01.462 [http-nio-5050-exec-2] [traceId=0558f21b-14bd-4da8-a65d-2c1113508eca] WARN  c.b.f.a.s.i.AdAuthServiceImpl - AD authentication LDAP error for user bunleng.kong from system dwh_bi [IP=10.18.1.131]: [LDAP: error code 49 - 80090308: LdapErr: DSID-0C090532, comment: AcceptSecurityContext error, data 775, v4f7c]
11:12:01.463 [http-nio-5050-exec-2] [traceId=0558f21b-14bd-4da8-a65d-2c1113508eca] INFO  c.b.s.l.RequestIdFilter - POST /api/v1/auth/login → 401 in 13ms
```
