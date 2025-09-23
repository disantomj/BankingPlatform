package com.example.bankingplatform.exception;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends BankingException {

    private final String endpoint;
    private final int limit;

    public RateLimitExceededException(String endpoint, int limit) {
        super(
            "RATE_LIMIT_EXCEEDED",
            String.format("Rate limit exceeded for %s. Limit: %d requests", endpoint, limit),
            "Too many requests. Please wait a moment before trying again."
        );
        this.endpoint = endpoint;
        this.limit = limit;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getLimit() {
        return limit;
    }
}