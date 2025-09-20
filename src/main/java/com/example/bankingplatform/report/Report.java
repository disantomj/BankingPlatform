package com.example.bankingplatform.report;

import com.example.bankingplatform.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_user", columnList = "user_id"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_reference", columnList = "report_reference"),
    @Index(name = "idx_report_created", columnList = "created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reportReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ReportFormat format;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt", "updatedAt"})
    private User requestedBy;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    // Date range parameters for report generation
    private LocalDate startDate;
    private LocalDate endDate;

    // Filter parameters (stored as JSON or key-value pairs)
    @Column(length = 2000)
    private String parameters; // JSON string of additional parameters

    // Report generation details
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // File storage information
    @Size(max = 500)
    private String filePath; // Path to generated report file

    @Size(max = 100)
    private String fileName;

    @PositiveOrZero
    private Long fileSizeBytes;

    @Size(max = 50)
    private String contentType; // MIME type

    // Report metadata
    @PositiveOrZero
    private Integer recordCount; // Number of records in report

    @Size(max = 1000)
    private String errorMessage; // Error details if generation failed

    // Expiration and cleanup
    private LocalDateTime expiresAt;

    @NotNull
    @Min(1)
    @Max(365)
    private Integer retentionDays = 30; // How long to keep the report

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Report() {}

    public Report(ReportType reportType, User requestedBy, String title, ReportFormat format,
                  LocalDate startDate, LocalDate endDate) {
        this.reportType = reportType;
        this.requestedBy = requestedBy;
        this.title = title;
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ReportStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.retentionDays = 30;
    }

    // Business logic methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == ReportStatus.COMPLETED;
    }

    public boolean canBeDownloaded() {
        return isCompleted() && !isExpired() && filePath != null;
    }

    public void markAsStarted() {
        this.status = ReportStatus.GENERATING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String filePath, String fileName, Long fileSizeBytes, String contentType, Integer recordCount) {
        this.status = ReportStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.contentType = contentType;
        this.recordCount = recordCount;
        this.expiresAt = LocalDateTime.now().plusDays(retentionDays);
    }

    public void markAsFailed(String errorMessage) {
        this.status = ReportStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public long getGenerationTimeMillis() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        }
        return 0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportReference() { return reportReference; }
    public void setReportReference(String reportReference) { this.reportReference = reportReference; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public ReportFormat getFormat() { return format; }
    public void setFormat(ReportFormat format) { this.format = format; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getRetentionDays() { return retentionDays; }
    public void setRetentionDays(Integer retentionDays) { this.retentionDays = retentionDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return Objects.equals(id, report.id) &&
               Objects.equals(reportReference, report.reportReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reportReference);
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", reportReference='" + reportReference + '\'' +
                ", reportType=" + reportType +
                ", status=" + status +
                ", format=" + format +
                ", title='" + title + '\'' +
                ", requestedAt=" + requestedAt +
                '}';
    }
}