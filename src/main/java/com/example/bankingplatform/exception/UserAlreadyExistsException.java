package com.example.bankingplatform.exception;

public class UserAlreadyExistsException extends BankingException {

    private final String field;
    private final String value;

    public UserAlreadyExistsException(String field, String value) {
        super(
            "USER_ALREADY_EXISTS",
            String.format("User with %s '%s' already exists", field, value),
            "An account with this username or email already exists. Please use different credentials."
        );
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}