package com.example.bankingplatform.exception;

/**
 * Base exception class for all banking-related exceptions
 */
public abstract class BankingException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;

    public BankingException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public BankingException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}