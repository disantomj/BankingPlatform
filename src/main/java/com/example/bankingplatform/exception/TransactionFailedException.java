package com.example.bankingplatform.exception;

/**
 * Exception thrown when a transaction fails to process
 */
public class TransactionFailedException extends BankingException {

    private final String transactionType;
    private final String reason;

    public TransactionFailedException(String transactionType, String reason) {
        super(
            "TRANSACTION_FAILED",
            String.format("%s transaction failed: %s", transactionType, reason),
            "Your transaction could not be processed at this time. Please try again or contact customer service."
        );
        this.transactionType = transactionType;
        this.reason = reason;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getReason() {
        return reason;
    }
}