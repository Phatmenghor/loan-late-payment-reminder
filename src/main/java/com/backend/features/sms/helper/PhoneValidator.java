package com.backend.features.sms.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhoneValidator {

    /**
     * Validate phone number - accept any non-empty phone number
     * Validation is lenient to accept all Cambodia formats
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("Phone number is empty");
            return false;
        }

        String cleanPhone = phone.trim().replaceAll("[^0-9+]", "");

        // Accept if has at least 7 digits
        if (cleanPhone.replaceAll("[^0-9]", "").length() >= 7) {
            log.debug("Valid phone number: {}", phone);
            return true;
        }

        log.warn("Phone number too short: {}", phone);
        return false;
    }

    /**
     * Format phone number to standard: remove spaces, dashes, etc
     * Keep leading 0 or + as is
     */
    public String formatPhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        String formatted = phone.trim()
                .replaceAll("\\s+", "")     // Remove spaces
                .replaceAll("-", "")         // Remove dashes
                .replaceAll("\\(", "")       // Remove parentheses
                .replaceAll("\\)", "");

        log.debug("Formatted phone number from '{}' to '{}'", phone, formatted);
        return formatted;
    }

    /**
     * Get phone number error message
     */
    public String getErrorMessage(String phone) {
        return "Invalid phone number: " + phone + " (must have at least 7 digits)";
    }
}
