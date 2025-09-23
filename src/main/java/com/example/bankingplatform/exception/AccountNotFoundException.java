package com.example.bankingplatform.exception;

/**
 * Exception thrown when an account is not found
 */
public class AccountNotFoundException extends BankingException {

    private final String accountIdentifier;

    public AccountNotFoundException(String accountIdentifier) {
        super(
            "ACCOUNT_NOT_FOUND",
            String.format("Account not found: %s", accountIdentifier),
            "The requested account could not be found. Please verify the account details."
        );
        this.accountIdentifier = accountIdentifier;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }
}