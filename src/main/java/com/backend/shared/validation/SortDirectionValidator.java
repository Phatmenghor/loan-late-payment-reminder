package com.backend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SortDirectionValidator implements ConstraintValidator<ValidSortDirection, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Let @NotNull handle null checks
        }
        return "ASC".equalsIgnoreCase(value) || "DESC".equalsIgnoreCase(value);
    }
}
