package com.example.bankingplatform.exception;

public class InvalidCredentialsException extends BankingException {

    public InvalidCredentialsException() {
        super(
            "INVALID_CREDENTIALS",
            "Invalid username or password provided",
            "The username or password you entered is incorrect. Please try again."
        );
    }

    public InvalidCredentialsException(String message) {
        super(
            "INVALID_CREDENTIALS",
            message,
            "The username or password you entered is incorrect. Please try again."
        );
    }
}