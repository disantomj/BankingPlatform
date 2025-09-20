package com.example.bankingplatform.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditService {

    private final AuditRepository auditRepository;

    @Autowired
    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    // Create audit log entry
    public Audit logAction(Integer userId, String username, AuditAction action,
                          AuditSeverity severity, String description) {
        Audit audit = new Audit(userId, username, action, severity, description);
        return auditRepository.save(audit);
    }

    // Create audit log with entity information
    public Audit logEntityAction(Integer userId, String username, AuditAction action,
                                AuditSeverity severity, String description,
                                String entityType, String entityId) {
        Audit audit = new Audit(userId, username, action, severity, description, entityType, entityId);
        return auditRepository.save(audit);
    }

    // Create audit log with before/after values
    public Audit logChange(Integer userId, String username, AuditAction action,
                          AuditSeverity severity, String description,
                          String entityType, String entityId,
                          String oldValue, String newValue) {
        Audit audit = new Audit(userId, username, action, severity, description, entityType, entityId);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        return auditRepository.save(audit);
    }

    // Create audit log with context information
    public Audit logActionWithContext(Integer userId, String username, AuditAction action,
                                     AuditSeverity severity, String description,
                                     String ipAddress, String userAgent, String sessionId) {
        Audit audit = new Audit(userId, username, action, severity, description);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent);
        audit.setSessionId(sessionId);
        return auditRepository.save(audit);
    }

    // Log failed action
    public Audit logFailedAction(Integer userId, String username, AuditAction action,
                                 AuditSeverity severity, String description,
                                 String errorMessage) {
        Audit audit = new Audit(userId, username, action, severity, description);
        audit.setSuccess(false);
        audit.setErrorMessage(errorMessage);
        return auditRepository.save(audit);
    }

    // Log high-risk action
    public Audit logHighRiskAction(Integer userId, String username, AuditAction action,
                                  String description, Integer riskScore,
                                  String ipAddress, String userAgent) {
        Audit audit = new Audit(userId, username, action, AuditSeverity.HIGH, description);
        audit.setRiskScore(riskScore);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent);
        return auditRepository.save(audit);
    }

    // Retrieval methods
    @Transactional(readOnly = true)
    public Optional<Audit> findById(Long id) {
        return auditRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Audit> getUserAuditHistory(Integer userId) {
        return auditRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Audit> getRecentUserActivity(Integer userId) {
        return auditRepository.findTop20ByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Audit> getAuditsByAction(AuditAction action) {
        return auditRepository.findByActionOrderByTimestampDesc(action);
    }

    @Transactional(readOnly = true)
    public List<Audit> getAuditsBySeverity(AuditSeverity severity) {
        return auditRepository.findBySeverityOrderByTimestampDesc(severity);
    }

    @Transactional(readOnly = true)
    public List<Audit> getEntityAuditHistory(String entityType, String entityId) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<Audit> getAuditsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Audit> getUserAuditsByDateRange(Integer userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Audit> getFailedOperations(LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.findBySuccessAndTimestampBetweenOrderByTimestampDesc(false, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Audit> getHighRiskActivities(Integer riskThreshold) {
        return auditRepository.findByRiskScoreGreaterThanOrderByTimestampDesc(riskThreshold);
    }

    @Transactional(readOnly = true)
    public List<Audit> getCriticalSecurityEvents() {
        return auditRepository.findBySeverityAndSuccessOrderByTimestampDesc(AuditSeverity.CRITICAL, false);
    }

    @Transactional(readOnly = true)
    public List<Audit> getRecentAudits() {
        return auditRepository.findTop50ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<Audit> getAuditsByIpAddress(String ipAddress) {
        return auditRepository.findByIpAddressOrderByTimestampDesc(ipAddress);
    }

    // Security analysis methods
    @Transactional(readOnly = true)
    public List<Audit> getSuspiciousUserActivity(Integer userId, LocalDateTime since) {
        List<Audit> userAudits = getUserAuditsByDateRange(userId, since, LocalDateTime.now());
        return userAudits.stream()
            .filter(audit -> audit.getRiskScore() > 70 ||
                           audit.getSeverity() == AuditSeverity.CRITICAL ||
                           !audit.getSuccess())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Audit> getMultipleFailedAttempts(String ipAddress, LocalDateTime since) {
        return getAuditsByIpAddress(ipAddress).stream()
            .filter(audit -> audit.getTimestamp().isAfter(since) && !audit.getSuccess())
            .collect(Collectors.toList());
    }

    // Count methods for analytics
    @Transactional(readOnly = true)
    public long getAuditCount() {
        return auditRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUserActionCount(Integer userId, AuditAction action) {
        return auditRepository.findByUserIdAndActionOrderByTimestampDesc(userId, action).size();
    }

    // Utility methods for risk scoring
    public Integer calculateRiskScore(AuditAction action, String ipAddress, Integer userId) {
        int riskScore = 0;

        // Base risk by action type
        switch (action) {
            case LOGIN_FAILED, UNAUTHORIZED_ACCESS, SUSPICIOUS_ACTIVITY -> riskScore += 30;
            case PASSWORD_CHANGED, ACCOUNT_DELETED -> riskScore += 20;
            case TRANSACTION_CREATED, BALANCE_UPDATED -> riskScore += 10;
            default -> riskScore += 5;
        }

        // Additional risk factors could be added here
        // (unusual IP, time of day, frequency, etc.)

        return Math.min(riskScore, 100); // Cap at 100
    }
}