package com.example.bankingplatform.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    // Find by user
    List<Audit> findByUserIdOrderByTimestampDesc(Integer userId);

    // Find by action
    List<Audit> findByActionOrderByTimestampDesc(AuditAction action);

    // Find by severity
    List<Audit> findBySeverityOrderByTimestampDesc(AuditSeverity severity);

    // Find by entity
    List<Audit> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    // Find by success status
    List<Audit> findBySuccessOrderByTimestampDesc(Boolean success);

    // Find by date range
    List<Audit> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Find by user and action
    List<Audit> findByUserIdAndActionOrderByTimestampDesc(Integer userId, AuditAction action);

    // Find by user and date range
    List<Audit> findByUserIdAndTimestampBetweenOrderByTimestampDesc(Integer userId, LocalDateTime startDate, LocalDateTime endDate);

    // Find by severity and date range
    List<Audit> findBySeverityAndTimestampBetweenOrderByTimestampDesc(AuditSeverity severity, LocalDateTime startDate, LocalDateTime endDate);

    // Find failed operations
    List<Audit> findBySuccessAndTimestampBetweenOrderByTimestampDesc(Boolean success, LocalDateTime startDate, LocalDateTime endDate);

    // Find high-risk activities
    List<Audit> findByRiskScoreGreaterThanOrderByTimestampDesc(Integer riskScore);

    // Find recent audits (using service to limit)
    List<Audit> findTop50ByOrderByTimestampDesc();

    // Find recent audits for user
    List<Audit> findTop20ByUserIdOrderByTimestampDesc(Integer userId);

    // Find by IP address (for security analysis)
    List<Audit> findByIpAddressOrderByTimestampDesc(String ipAddress);

    // Find critical security events
    List<Audit> findBySeverityAndSuccessOrderByTimestampDesc(AuditSeverity severity, Boolean success);
}