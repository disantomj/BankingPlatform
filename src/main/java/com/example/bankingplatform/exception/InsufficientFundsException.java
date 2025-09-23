package com.example.bankingplatform.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when account has insufficient funds for a transaction
 */
public class InsufficientFundsException extends BankingException {

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(
            "INSUFFICIENT_FUNDS",
            String.format("Insufficient funds. Available: %s, Requested: %s", availableBalance, requestedAmount),
            "Insufficient funds in your account for this transaction. Please check your balance."
        );
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}