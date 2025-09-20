package com.example.bankingplatform.audit;

public enum AuditAction {
    // Authentication actions
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED,

    // User management
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    // Account management
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,
    ACCOUNT_STATUS_CHANGED,
    ACCOUNT_DELETED,
    BALANCE_UPDATED,

    // Transaction actions
    TRANSACTION_CREATED,
    TRANSACTION_PROCESSED,
    TRANSACTION_CANCELLED,
    TRANSACTION_FAILED,

    // Administrative actions
    ADMIN_ACCESS,
    SYSTEM_CONFIG_CHANGED,
    REPORT_GENERATED,
    DATA_EXPORT,

    // Security events
    SUSPICIOUS_ACTIVITY,
    UNAUTHORIZED_ACCESS,
    API_RATE_LIMITED,
    SESSION_EXPIRED
}