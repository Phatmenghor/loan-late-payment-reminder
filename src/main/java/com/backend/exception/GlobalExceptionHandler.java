package com.backend.exception;

import com.backend.exception.custom.CustomException;
import com.backend.exception.custom.NotFoundException;
import com.backend.exception.custom.ValidationException;
import com.backend.security.SecurityUtils;
import com.backend.shared.constants.ErrorCodes;
import com.backend.shared.dto.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final SecurityUtils securityUtils;

    // ================================
    // AUTHENTICATION & AUTHORIZATION ERRORS
    // ================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed - Invalid credentials for path {} [IP={}]", request.getRequestURI(), getClientIP(request));

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INVALID_CREDENTIALS, request);
        errorDetails.put("field", "credentials");
        errorDetails.put("suggestion", "Please verify your email and password");

        ApiResponse<Object> response = new ApiResponse<>("error",
                "Invalid email or password", errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed for path {} [IP={}]: {}", request.getRequestURI(), getClientIP(request), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INVALID_CREDENTIALS, request);
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Authentication failed. Please check your credentials.", errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account disabled for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_DISABLED, request);
        errorDetails.put("accountStatus", "DISABLED");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Your account has been disabled. Please contact support.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(
            LockedException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account locked for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_LOCKED, request);
        
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Your account has been locked. Please contact support.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for request to {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INSUFFICIENT_PERMISSIONS, request);
        errorDetails.put("requiredAction", "Ensure you have the necessary permissions");
        
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Access denied. You don't have permission to perform this action.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ================================
    // VALIDATION ERRORS
    // ================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();

            Map<String, Object> errorInfo = new LinkedHashMap<>();
            errorInfo.put("message", errorMessage != null ? errorMessage : "Validation failed");
            errorInfo.put("rejectedValue", rejectedValue);
            errorInfo.put("field", fieldName);
            fieldErrors.put(fieldName, errorInfo);

            if (errorMessage != null && (errorMessage.toLowerCase().contains("required") ||
                    errorMessage.toLowerCase().contains("must not be null") ||
                    errorMessage.toLowerCase().contains("must not be blank"))) {
                missingFields.add(fieldName);
            } else {
                invalidFields.add(fieldName);
            }
        });

        log.warn("Validation failed for request to {}: {} field error(s)", request.getRequestURI(), fieldErrors.size());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);
        errorDetails.put("fieldErrors", fieldErrors);
        errorDetails.put("missingFields", missingFields);
        errorDetails.put("invalidFields", invalidFields);
        errorDetails.put("totalErrors", fieldErrors.size());

        String message = String.format("Validation failed for %d field(s). Required fields: %s",
                fieldErrors.size(),
                missingFields.isEmpty() ? "none" : String.join(", ", missingFields));

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        log.warn("Constraint violation for request to {}: {}", request.getRequestURI(), violations);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);
        errorDetails.put("violations", violations);

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Data validation failed. Please check the constraints.", errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Validation error for path {}: {}", request.getRequestURI(), ex.getMessage());

        String message = ex.getMessage();
        String errorCode = ErrorCodes.VALIDATION_ERROR;

        if (message.toLowerCase().contains("api key is required")) {
            errorCode = ErrorCodes.API_KEY_REQUIRED;
        } else if (message.toLowerCase().contains("invalid api key")) {
            errorCode = ErrorCodes.API_KEY_INVALID;
        } else if (message.toLowerCase().contains("inactive") || message.toLowerCase().contains("locked")) {
            errorCode = ErrorCodes.API_KEY_INACTIVE;
        } else if (message.toLowerCase().contains("already exists")) {
            errorCode = ErrorCodes.API_KEY_EXISTS;
        }

        Map<String, Object> errorDetails = createErrorDetails(errorCode, request);

        if (message.toLowerCase().contains("email")) {
            errorDetails.put("field", "email");
        } else if (message.toLowerCase().contains("phone")) {
            errorDetails.put("field", "phoneNumber");
        } else if (message.toLowerCase().contains("api key")) {
            errorDetails.put("field", "apiKey");
        } else if (message.toLowerCase().contains("label")) {
            errorDetails.put("field", "label");
        }

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = "An unexpected error occurred while processing your request.";
        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INTERNAL_SERVER_ERROR, request);

        if (ex.getMessage() != null) {
            String exMessage = ex.getMessage().toLowerCase();
            if (exMessage.contains("email")) {
                message = "The email address is already in use. Please use a different email.";
                errorDetails.put("field", "email");
                errorDetails.put("type", "duplicate");
            } else if (exMessage.contains("timeout")) {
                message = "The request timed out. Please try again.";
            } else if (exMessage.contains("connection")) {
                message = "A connection error occurred. Please try again later.";
            } else if (exMessage.contains("not found")) {
                message = "The requested resource could not be found.";
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("error", message, errorDetails));
    }

    // ================================
    // NOT FOUND ERRORS
    // ================================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(
            NotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.USER_NOT_FOUND, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        Map<String, Object> errorDetails = createErrorDetails("ENDPOINT_NOT_FOUND", request);
        errorDetails.put("method", ex.getHttpMethod());

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "The requested endpoint was not found.", errorDetails);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ================================
    // HTTP METHOD & REQUEST ERRORS
    // ================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported: {} for path {}", ex.getMethod(), request.getRequestURI());

        Map<String, Object> errorDetails = createErrorDetails("METHOD_NOT_SUPPORTED", request);
        errorDetails.put("supportedMethods", ex.getSupportedHttpMethods());
        errorDetails.put("requestedMethod", ex.getMethod());

        ApiResponse<Object> response = new ApiResponse<>("error",
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                errorDetails);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid JSON request body for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails("INVALID_REQUEST_BODY", request);
        errorDetails.put("hint", "Please check your JSON format and data types");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Invalid request body. Please check your JSON format.", errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing required parameter: {} for path {}", ex.getParameterName(), request.getRequestURI());

        Map<String, Object> errorDetails = createErrorDetails("MISSING_PARAMETER", request);
        errorDetails.put("field", ex.getParameterName());

        ApiResponse<Object> response = new ApiResponse<>("error",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter: {} for path {}", ex.getName(), request.getRequestURI());

        Map<String, Object> errorDetails = createErrorDetails("TYPE_MISMATCH", request);
        errorDetails.put("parameterName", ex.getName());

        ApiResponse<Object> response = new ApiResponse<>("error", 
            String.format("Invalid value for parameter '%s'.", ex.getName()), 
            errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ================================
    // DATABASE ERRORS
    // ================================

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLockException(
            OptimisticLockException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict for request to {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails("CONFLICT", request);
        ApiResponse<Object> response = new ApiResponse<>("error",
                "The data was modified by another request. Please try again.", errorDetails);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Data validation failed";
        String errorCode = ErrorCodes.VALIDATION_ERROR;
        Map<String, Object> errorDetails = createErrorDetails(errorCode, request);

        String exceptionMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage().toLowerCase() : "";
        String fullMessage = (exceptionMessage + " " + rootCauseMessage).toLowerCase();

        if (fullMessage.contains("label") || fullMessage.contains("uk_ad_api_keys_label") || fullMessage.contains("uk_api_keys_label")) {
            message = "API Key with this label already exists";
            errorDetails.put("field", "label");
            errorDetails.put("type", "duplicate");
        } else if (fullMessage.contains("foreign key")) {
            message = "Referenced data does not exist.";
        }

        log.warn("Data integrity violation for path {}: {}", request.getRequestURI(), message);

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database access error in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.DATABASE_ERROR, request);
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Database operation failed. Please try again.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Object>> handleSQLException(
            SQLException ex, HttpServletRequest request) {
        log.error("SQL error in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.DATABASE_ERROR, request);
        
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Database query failed. Please try again.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(
            CustomException ex, HttpServletRequest request) {
        log.error("Custom exception for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ex.getErrorCode(), request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Invalid argument for path {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ================================
    // GENERIC EXCEPTION HANDLER
    // ================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception in request to {}: {}", 
            request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INTERNAL_SERVER_ERROR, request);

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "An unexpected error occurred.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }

    private Map<String, Object> createErrorDetails(String errorCode, HttpServletRequest request) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("errorCode", errorCode);
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("traceId", getTraceId());
        return errorDetails;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}