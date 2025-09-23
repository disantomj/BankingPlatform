package com.example.bankingplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        logger.warn("Validation failed with {} field errors: {}", fieldErrors.size(), fieldErrors.keySet());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDATION_FAILED");
        response.put("message", getValidationSummaryMessage(fieldErrors));
        response.put("fieldErrors", fieldErrors);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private String getValidationSummaryMessage(Map<String, String> fieldErrors) {
        if (fieldErrors.size() == 1) {
            return "Please fix the validation error and try again.";
        } else if (fieldErrors.size() <= 3) {
            return String.format("Please fix %d validation errors and try again.", fieldErrors.size());
        } else {
            return "Please check your input data and fix all validation errors.";
        }
    }

    /**
     * Handle custom banking exceptions
     */
    @ExceptionHandler(BankingException.class)
    public ResponseEntity<Map<String, Object>> handleBankingException(BankingException ex) {
        // Log banking exceptions with appropriate level
        if (ex instanceof RateLimitExceededException) {
            logger.warn("Rate limit exceeded: {}", ex.getMessage());
        } else if (ex instanceof AccountNotFoundException) {
            logger.info("Account not found: {}", ex.getMessage());
        } else if (ex instanceof InsufficientFundsException) {
            logger.info("Insufficient funds: {}", ex.getMessage());
        } else {
            logger.error("Banking exception occurred: {}", ex.getMessage(), ex);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getErrorCode());
        response.put("message", ex.getUserMessage());
        response.put("timestamp", LocalDateTime.now());

        // Determine HTTP status based on exception type
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof InsufficientFundsException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (ex instanceof AccountNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof AccountClosedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof TransactionFailedException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (ex instanceof RateLimitExceededException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        }

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle user already exists exception
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_ALREADY_EXISTS");
        response.put("message", "An account with this username or email already exists");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle invalid credentials exception
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_CREDENTIALS");
        response.put("message", "Invalid username or password");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "ACCESS_DENIED");
        response.put("message", "You don't have permission to access this resource");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle rate limiting exceptions
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "RATE_LIMIT_EXCEEDED");
        response.put("message", "Too many requests. Please try again later.");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "MISSING_PARAMETER");
        response.put("message", String.format("Required parameter '%s' is missing", ex.getParameterName()));
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_PARAMETER_TYPE");
        response.put("message", String.format("Invalid value for parameter '%s'. Expected %s but received '%s'",
            ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue()));
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "DATA_INTEGRITY_VIOLATION");

        String message = "A data constraint was violated. Please check your input.";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique") || ex.getMessage().contains("duplicate")) {
                message = "This value already exists. Please use a different value.";
            } else if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("reference")) {
                message = "Referenced data does not exist. Please check your input.";
            }
        }

        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log the full exception with stack trace for debugging
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", "An unexpected error occurred. Please try again later.");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}