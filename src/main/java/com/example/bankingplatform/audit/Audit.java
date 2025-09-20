package com.example.bankingplatform.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "audits", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_severity", columnList = "severity")
})
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username")
    private String username; // Store username for reference even if user is deleted

    // What action was performed
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private AuditAction action;

    // Severity level
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private AuditSeverity severity;

    // Details about the action
    @Column(nullable = false)
    @NotBlank
    @Size(max = 500)
    private String description;

    // What entity was affected (optional)
    @Column(name = "entity_type")
    private String entityType; // "Account", "Transaction", "User", etc.

    @Column(name = "entity_id")
    private String entityId; // ID of the affected entity

    // Context information
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    // Additional metadata (JSON format for flexibility)
    @Column(name = "metadata", length = 1000)
    private String metadata;

    // Before and after values for changes
    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    // Success/failure status
    @Column(nullable = false)
    private Boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    // Timestamp (immutable)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Risk score (for fraud detection)
    @Column(name = "risk_score")
    @Min(0)
    @Max(100)
    private Integer riskScore = 0;

    // Default constructor
    public Audit() {}

    // Constructor for basic audit
    public Audit(Integer userId, String username, AuditAction action,
                AuditSeverity severity, String description) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.severity = severity;
        this.description = description;
    }

    // Constructor with entity information
    public Audit(Integer userId, String username, AuditAction action,
                AuditSeverity severity, String description,
                String entityType, String entityId) {
        this(userId, username, action, severity, description);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Audit audit = (Audit) o;
        return Objects.equals(id, audit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Audit{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", action=" + action +
                ", severity=" + severity +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}