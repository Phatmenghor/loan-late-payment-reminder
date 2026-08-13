package com.backend.features.auth.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for Active Directory & LDAP error code parsing,
 * text sanitization (UTF-8 0x00 removal), error code mapping, and user-friendly message resolution.
 */
@UtilityClass
public class LdapAdErrorUtils {

    /**
     * Removes NUL bytes (\u0000 / 0x00) and normalizes line breaks to prevent
     * PostgreSQL "invalid byte sequence for encoding UTF8: 0x00" exceptions.
     *
     * @param input Raw input string
     * @return Cleaned string safe for database persistence and logging
     */
    public static String sanitizeUtf8(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\u0000", "")
                .replace("\0", "")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    /**
     * Resolves a machine-readable error code for frontend programmatic handling.
     *
     * @param rawErrorMessage Raw exception message from LDAP / JNDI
     * @return Standardized error code string
     */
    public static String resolveErrorCode(String rawErrorMessage) {
        if (rawErrorMessage == null || rawErrorMessage.isBlank()) {
            return "AD_AUTH_FAILED";
        }

        String cleaned = sanitizeUtf8(rawErrorMessage);
        String errUpper = cleaned.toUpperCase();

        if (containsCode(errUpper, "775")) return "AD_ACCOUNT_LOCKED_ATTEMPTS";
        if (containsCode(errUpper, "533")) return "AD_ACCOUNT_DISABLED_ADMIN";
        if (containsCode(errUpper, "532") || containsCode(errUpper, "773")) return "AD_PASSWORD_EXPIRED";
        if (containsCode(errUpper, "701")) return "AD_ACCOUNT_EXPIRED";
        if (containsCode(errUpper, "525")) return "AD_USER_NOT_FOUND";
        if (containsCode(errUpper, "52E")) return "AD_INVALID_CREDENTIALS";
        if (containsCode(errUpper, "530")) return "AD_LOGON_HOURS_RESTRICTED";
        if (containsCode(errUpper, "531")) return "AD_WORKSTATION_RESTRICTED";
        if (containsCode(errUpper, "568")) return "AD_TOO_MANY_GROUPS";
        if (containsCode(errUpper, "5320")) return "AD_SMART_CARD_REQUIRED";

        if (containsCode(errUpper, "80090324")) return "AD_CLOCK_SKEW_TOO_GREAT";
        if (containsCode(errUpper, "80090322")) return "AD_SPN_MISMATCH";
        if (containsCode(errUpper, "80090311")) return "AD_DOMAIN_CONTROLLER_UNAVAILABLE";
        if (containsCode(errUpper, "8009030C")) return "AD_KERBEROS_AUTH_FAILED";
        if (containsCode(errUpper, "80090308")) return "AD_LDAP_AUTH_FAILED";

        if (errUpper.contains("INVALID DN SYNTAX") || containsCode(errUpper, "34")) return "AD_INVALID_DN_SYNTAX";
        if (errUpper.contains("NO SUCH OBJECT") || containsCode(errUpper, "32")) return "AD_OBJECT_NOT_FOUND";
        if (errUpper.contains("INSUFFICIENT ACCESS") || containsCode(errUpper, "50")) return "AD_INSUFFICIENT_ACCESS";
        if (errUpper.contains("UNWILLING TO PERFORM") || containsCode(errUpper, "53")) return "AD_UNWILLING_TO_PERFORM";
        if (errUpper.contains("CONSTRAINT VIOLATION") || containsCode(errUpper, "19")) return "AD_CONSTRAINT_VIOLATION";
        if (errUpper.contains("ERROR CODE 49") || containsCode(errUpper, "49")) return "AD_INVALID_CREDENTIALS";

        return "AD_AUTH_FAILED";
    }

    /**
     * Parses LDAP / Active Directory exception messages or error codes and returns
     * a detailed, professional, user-friendly error message.
     *
     * @param rawErrorMessage Raw exception message from LDAP / JNDI
     * @return Friendly error message for end-users
     */
    public static String resolveFriendlyErrorMessage(String rawErrorMessage) {
        if (rawErrorMessage == null || rawErrorMessage.isBlank()) {
            return "Active Directory authentication failed. Please check your credentials or contact IT Support.";
        }

        String cleaned = sanitizeUtf8(rawErrorMessage);
        String errUpper = cleaned.toUpperCase();

        // 1. AD Specific Win32 Sub-error codes (Data XXXX / XXX)
        if (containsCode(errUpper, "775")) {
            return "Your Active Directory account is locked due to multiple failed login attempts. Please contact IT Support to unlock your account.";
        }
        if (containsCode(errUpper, "533")) {
            return "Your Active Directory account has been disabled by an administrator. Please contact IT Support for assistance.";
        }
        if (containsCode(errUpper, "532") || containsCode(errUpper, "773")) {
            return "Your Active Directory password has expired or must be reset before logging in. Please update your password and try again.";
        }
        if (containsCode(errUpper, "701")) {
            return "Your Active Directory account has expired. Please contact IT Support or your system administrator to extend your account access.";
        }
        if (containsCode(errUpper, "525")) {
            return "The specified Active Directory username was not found. Please verify your username or User Principal Name (UPN) and try again.";
        }
        if (containsCode(errUpper, "52E")) {
            return "Invalid Active Directory credentials. Please double-check your username and password and try again.";
        }
        if (containsCode(errUpper, "530")) {
            return "Active Directory login is currently restricted at this time based on account logon hours policy. Please contact your system administrator.";
        }
        if (containsCode(errUpper, "531")) {
            return "Active Directory login is restricted from this workstation or IP address. Please log in from an authorized workstation or contact IT Support.";
        }
        if (containsCode(errUpper, "568")) {
            return "Active Directory login failed due to too many security identifiers (group memberships). Please contact IT Support to review your group memberships.";
        }
        if (containsCode(errUpper, "5320")) {
            return "Smart card authentication is required for this Active Directory account. Please log in using your smart card.";
        }

        // 2. SSPI / Kerberos / System Error codes
        if (containsCode(errUpper, "80090324")) {
            return "System clock skew between client and Active Directory Domain Controller is too great. Please synchronize system time and try again.";
        }
        if (containsCode(errUpper, "80090322")) {
            return "Target principal or SPN mismatch error during Active Directory authentication. Please contact IT Support.";
        }
        if (containsCode(errUpper, "80090311")) {
            return "Active Directory Domain Controller is currently unavailable. Please verify domain network connectivity or contact IT Support.";
        }
        if (containsCode(errUpper, "8009030C")) {
            return "Security package or Kerberos authentication error occurred during Active Directory login. Please verify domain connectivity.";
        }
        if (containsCode(errUpper, "80090308")) {
            return "Active Directory LDAP authentication failed. Please verify your credentials or contact system administrator.";
        }

        // 3. Standard LDAP Result Codes & Errors
        if (errUpper.contains("INVALID DN SYNTAX") || containsCode(errUpper, "34")) {
            return "Invalid Distinguished Name (DN) syntax in Active Directory request. Please contact system administrator.";
        }
        if (errUpper.contains("NO SUCH OBJECT") || containsCode(errUpper, "32")) {
            return "The requested Active Directory object or Base DN was not found. Please contact system administrator.";
        }
        if (errUpper.contains("INSUFFICIENT ACCESS") || containsCode(errUpper, "50")) {
            return "Insufficient permissions to perform Active Directory operation. Please contact system administrator.";
        }
        if (errUpper.contains("UNWILLING TO PERFORM") || containsCode(errUpper, "53")) {
            return "Active Directory server is unwilling to perform the requested operation due to policy restrictions.";
        }
        if (errUpper.contains("ENTRY ALREADY EXISTS") || containsCode(errUpper, "68")) {
            return "The Active Directory entry already exists. Please contact system administrator.";
        }
        if (errUpper.contains("CONSTRAINT VIOLATION") || containsCode(errUpper, "19")) {
            return "Active Directory constraint violation occurred. Please check account attribute policy requirements.";
        }
        if (errUpper.contains("ERROR CODE 49") || containsCode(errUpper, "49")) {
            return "Invalid Active Directory username or password. Please verify your credentials.";
        }

        return "Active Directory authentication failed: " + cleaned;
    }

    private static boolean containsCode(String text, String code) {
        if (text == null || code == null) {
            return false;
        }
        return text.contains("DATA " + code) ||
                text.contains("DATA: " + code) ||
                text.contains("DATA " + code + ",") ||
                text.contains("CODE " + code) ||
                text.contains("ERR: " + code) ||
                text.contains(" " + code + ",") ||
                text.contains(" " + code + " ") ||
                text.endsWith(" " + code);
    }
}
