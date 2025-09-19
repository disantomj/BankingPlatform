package com.example.bankingplatform.transaction;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED,
    ON_HOLD,
    REJECTED
}
