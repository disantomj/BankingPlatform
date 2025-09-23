package com.example.bankingplatform.security;

import com.example.bankingplatform.exception.RateLimitExceededException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limit storage: IP -> RequestInfo
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    // Rate limits by endpoint pattern (increased for development)
    private static final int LOGIN_ATTEMPTS_PER_WINDOW = 50; // Increased for dev
    private static final int API_REQUESTS_PER_WINDOW = 1000; // Increased for dev
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(5); // Shorter window for dev

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIP = getClientIP(httpRequest);
        String requestURI = httpRequest.getRequestURI();

        // Apply rate limiting based on endpoint
        if (isRateLimited(clientIP, requestURI)) {
            logger.warn("Rate limit exceeded for IP {} on endpoint {}", clientIP, requestURI);
            // Throw exception to be handled by GlobalExceptionHandler
            throw new RateLimitExceededException(requestURI, getLimit(requestURI));
        }

        // Record this request
        recordRequest(clientIP, requestURI);

        // Continue with the request
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIP, String requestURI) {
        RequestInfo info = requestCounts.get(clientIP);

        if (info == null) {
            return false; // First request from this IP
        }

        // Clean up old entries first
        if (isWindowExpired(info.getLastRequest())) {
            requestCounts.remove(clientIP);
            return false;
        }

        // Check rate limits based on endpoint
        if (requestURI.contains("/login")) {
            return info.getLoginAttempts() >= LOGIN_ATTEMPTS_PER_WINDOW;
        } else if (requestURI.startsWith("/api/")) {
            return info.getApiRequests() >= API_REQUESTS_PER_WINDOW;
        }

        return false;
    }

    private void recordRequest(String clientIP, String requestURI) {
        RequestInfo info = requestCounts.computeIfAbsent(clientIP, k -> new RequestInfo());

        // Reset counters if window has expired
        if (isWindowExpired(info.getLastRequest())) {
            info.reset();
        }

        // Increment appropriate counter
        if (requestURI.contains("/login")) {
            info.incrementLoginAttempts();
        } else if (requestURI.startsWith("/api/")) {
            info.incrementApiRequests();
        }

        info.setLastRequest(LocalDateTime.now());
    }

    private boolean isWindowExpired(LocalDateTime lastRequest) {
        if (lastRequest == null) {
            return true;
        }
        return Duration.between(lastRequest, LocalDateTime.now()).compareTo(RATE_LIMIT_WINDOW) > 0;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private int getLimit(String requestURI) {
        if (requestURI.contains("/login")) {
            return LOGIN_ATTEMPTS_PER_WINDOW;
        } else if (requestURI.startsWith("/api/")) {
            return API_REQUESTS_PER_WINDOW;
        }
        return 0;
    }

    // Development helper method to clear rate limits
    public void clearRateLimits() {
        requestCounts.clear();
        logger.info("Rate limit cache cleared");
    }

    // Inner class to track request information
    private static class RequestInfo {
        private int loginAttempts = 0;
        private int apiRequests = 0;
        private LocalDateTime lastRequest;

        public void incrementLoginAttempts() { this.loginAttempts++; }
        public void incrementApiRequests() { this.apiRequests++; }

        public int getLoginAttempts() { return loginAttempts; }
        public int getApiRequests() { return apiRequests; }
        public LocalDateTime getLastRequest() { return lastRequest; }
        public void setLastRequest(LocalDateTime lastRequest) { this.lastRequest = lastRequest; }

        public void reset() {
            this.loginAttempts = 0;
            this.apiRequests = 0;
            this.lastRequest = LocalDateTime.now();
        }
    }
}