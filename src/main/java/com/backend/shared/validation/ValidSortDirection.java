package com.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SortDirectionValidator.class)
@Documented
public @interface ValidSortDirection {
    String message() default "Sort direction must be ASC or DESC";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
