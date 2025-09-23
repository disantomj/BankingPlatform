package com.example.bankingplatform.audit;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // Manual audit log creation (for testing or special cases)
    @PostMapping
    public ResponseEntity<?> createAuditLog(@Valid @RequestBody CreateAuditRequest request) {
        try {
            Audit audit = auditService.logAction(
                request.getUserId(),
                request.getUsername(),
                request.getAction(),
                request.getSeverity(),
                request.getDescription()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(audit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get audit by ID
    @GetMapping("/{id}")
    public ResponseEntity<Audit> getAudit(@PathVariable Long id) {
        return auditService.findById(id)
            .map(audit -> ResponseEntity.ok(audit))
            .orElse(ResponseEntity.notFound().build());
    }

    // Get all recent audits
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Audit>> getRecentAudits() {
        List<Audit> audits = auditService.getRecentAudits();
        return ResponseEntity.ok(audits);
    }

    // Get user's audit history
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Audit>> getUserAuditHistory(@PathVariable Integer userId) {
        List<Audit> audits = auditService.getUserAuditHistory(userId);
        return ResponseEntity.ok(audits);
    }

    // Get recent user activity
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Audit>> getRecentUserActivity(@PathVariable Integer userId) {
        List<Audit> audits = auditService.getRecentUserActivity(userId);
        return ResponseEntity.ok(audits);
    }

    // Get audits by action type
    @GetMapping("/action/{action}")
    public ResponseEntity<List<Audit>> getAuditsByAction(@PathVariable AuditAction action) {
        List<Audit> audits = auditService.getAuditsByAction(action);
        return ResponseEntity.ok(audits);
    }

    // Get audits by severity
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<Audit>> getAuditsBySeverity(@PathVariable AuditSeverity severity) {
        List<Audit> audits = auditService.getAuditsBySeverity(severity);
        return ResponseEntity.ok(audits);
    }

    // Get entity audit history
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<Audit>> getEntityAuditHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        List<Audit> audits = auditService.getEntityAuditHistory(entityType, entityId);
        return ResponseEntity.ok(audits);
    }

    // Get audits by date range
    @GetMapping("/date-range")
    public ResponseEntity<List<Audit>> getAuditsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Audit> audits = auditService.getAuditsByDateRange(startDate, endDate);
        return ResponseEntity.ok(audits);
    }

    // Get user audits by date range
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<Audit>> getUserAuditsByDateRange(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Audit> audits = auditService.getUserAuditsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(audits);
    }

    // Security endpoints
    @GetMapping("/security/failed-operations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Audit>> getFailedOperations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Audit> audits = auditService.getFailedOperations(startDate, endDate);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/security/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Audit>> getHighRiskActivities(
            @RequestParam(defaultValue = "70") Integer riskThreshold) {
        List<Audit> audits = auditService.getHighRiskActivities(riskThreshold);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/security/critical-events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Audit>> getCriticalSecurityEvents() {
        List<Audit> audits = auditService.getCriticalSecurityEvents();
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/security/suspicious-user/{userId}")
    public ResponseEntity<List<Audit>> getSuspiciousUserActivity(
            @PathVariable Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        LocalDateTime startDate = since != null ? since : LocalDateTime.now().minusDays(7);
        List<Audit> audits = auditService.getSuspiciousUserActivity(userId, startDate);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/security/failed-attempts/{ipAddress}")
    public ResponseEntity<List<Audit>> getMultipleFailedAttempts(
            @PathVariable String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        LocalDateTime startDate = since != null ? since : LocalDateTime.now().minusHours(1);
        List<Audit> audits = auditService.getMultipleFailedAttempts(ipAddress, startDate);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/security/ip-activity/{ipAddress}")
    public ResponseEntity<List<Audit>> getAuditsByIpAddress(@PathVariable String ipAddress) {
        List<Audit> audits = auditService.getAuditsByIpAddress(ipAddress);
        return ResponseEntity.ok(audits);
    }

    // Analytics endpoints
    @GetMapping("/analytics/count")
    public ResponseEntity<Long> getTotalAuditCount() {
        long count = auditService.getAuditCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/analytics/user/{userId}/action/{action}/count")
    public ResponseEntity<Long> getUserActionCount(
            @PathVariable Integer userId,
            @PathVariable AuditAction action) {
        long count = auditService.getUserActionCount(userId, action);
        return ResponseEntity.ok(count);
    }

    // Request DTOs
    public static class CreateAuditRequest {
        private Integer userId;
        private String username;
        private AuditAction action;
        private AuditSeverity severity;
        private String description;
        private String entityType;
        private String entityId;
        private String ipAddress;
        private String userAgent;
        private String sessionId;

        // Getters and setters
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public AuditAction getAction() { return action; }
        public void setAction(AuditAction action) { this.action = action; }

        public AuditSeverity getSeverity() { return severity; }
        public void setSeverity(AuditSeverity severity) { this.severity = severity; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }

        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}