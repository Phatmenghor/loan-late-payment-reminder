package com.backend.features.sms.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class PhoneValidator {

    private static final Pattern CAMBODIA_PHONE_PATTERN = Pattern.compile(
            "^(0|\\+855)?[0-9]{7,11}$"
    );

    private static final Pattern VALID_PHONE_PATTERN = Pattern.compile(
            "^[0-9]{7,15}$"
    );

    /**
     * Validate and format Cambodia phone number
     * Accepts: 0123456789, +855123456789, 123456789
     * Converts to: 0123456789 format
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("Phone number is empty");
            return false;
        }

        String cleanPhone = phone.trim();

        // Check if it matches Cambodia phone pattern
        if (CAMBODIA_PHONE_PATTERN.matcher(cleanPhone).matches()) {
            log.debug("Valid Cambodia phone number: {}", cleanPhone);
            return true;
        }

        // Check if it's a generic valid phone number
        if (VALID_PHONE_PATTERN.matcher(cleanPhone.replaceAll("[^0-9]", "")).matches()) {
            log.debug("Valid generic phone number: {}", cleanPhone);
            return true;
        }

        log.warn("Invalid phone number format: {}", phone);
        return false;
    }

    /**
     * Format phone number to standard Cambodia format: 0xxxxxxxxx
     */
    public String formatPhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        String cleaned = phone.trim().replaceAll("[^0-9+]", "");

        // Remove +855 and replace with 0
        if (cleaned.startsWith("+855")) {
            cleaned = "0" + cleaned.substring(4);
        }
        // Remove leading + if any
        else if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        // Add 0 prefix if missing and starts with 8 or 9 (Cambodia country code 855)
        else if (!cleaned.startsWith("0") && (cleaned.startsWith("8") || cleaned.startsWith("9"))) {
            cleaned = "0" + cleaned;
        }

        log.debug("Formatted phone number from {} to {}", phone, cleaned);
        return cleaned;
    }

    /**
     * Get phone number error message
     */
    public String getErrorMessage(String phone) {
        return "Invalid phone number format: " + phone +
               ". Expected formats: 0123456789, +855123456789, or 123456789";
    }
}
