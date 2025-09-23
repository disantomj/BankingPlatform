package com.example.bankingplatform.exception;

/**
 * Exception thrown when trying to operate on a closed account
 */
public class AccountClosedException extends BankingException {

    private final String accountNumber;

    public AccountClosedException(String accountNumber) {
        super(
            "ACCOUNT_CLOSED",
            String.format("Account %s is closed", accountNumber),
            "This account is closed and cannot be used for transactions. Please contact customer service."
        );
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}