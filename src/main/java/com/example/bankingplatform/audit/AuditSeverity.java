package com.example.bankingplatform.audit;

public enum AuditSeverity {
    LOW,       // Normal operations (successful login, account view)
    MEDIUM,    // Important operations (account creation, transaction)
    HIGH,      // Sensitive operations (password change, large transactions)
    CRITICAL   // Security events (failed login attempts, unauthorized access)
}