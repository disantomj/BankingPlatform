package com.example.bankingplatform.report;

import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final AuditService auditService;

    @Autowired
    public ReportService(ReportRepository reportRepository, AuditService auditService) {
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    public Report requestReport(User requestedBy, ReportType reportType, String title, ReportFormat format,
                               LocalDate startDate, LocalDate endDate, String description, String parameters) {
        String reportReference = generateReportReference();

        Report report = new Report(reportType, requestedBy, title, format, startDate, endDate);
        report.setReportReference(reportReference);
        report.setDescription(description);
        report.setParameters(parameters);

        Report savedReport = reportRepository.save(report);

        auditService.logEntityAction(
            requestedBy.getId(),
            requestedBy.getUsername(),
            AuditAction.REPORT_REQUESTED,
            AuditSeverity.LOW,
            "Report requested - " + reportType + " format: " + format + " (" + title + ")",
            "Report",
            savedReport.getId().toString()
        );

        return savedReport;
    }

    public Report startReportGeneration(Long reportId, String generatedBy) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("Report cannot be started in current status: " + report.getStatus());
        }

        report.markAsStarted();

        Report savedReport = reportRepository.save(report);

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            generatedBy,
            AuditAction.REPORT_GENERATED,
            AuditSeverity.LOW,
            "Report generation started - " + report.getReportReference(),
            "Report",
            savedReport.getId().toString()
        );

        return savedReport;
    }

    public Report completeReportGeneration(Long reportId, String filePath, String fileName,
                                          Long fileSizeBytes, String contentType, Integer recordCount) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.GENERATING) {
            throw new IllegalStateException("Report cannot be completed in current status: " + report.getStatus());
        }

        report.markAsCompleted(filePath, fileName, fileSizeBytes, contentType, recordCount);

        Report savedReport = reportRepository.save(report);

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            "SYSTEM",
            AuditAction.REPORT_GENERATED,
            AuditSeverity.LOW,
            "Report generation completed - " + report.getReportReference() + " (" + recordCount + " records, " + fileSizeBytes + " bytes)",
            "Report",
            savedReport.getId().toString()
        );

        return savedReport;
    }

    public Report failReportGeneration(Long reportId, String errorMessage) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (report.getStatus() != ReportStatus.GENERATING && report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("Report cannot be failed in current status: " + report.getStatus());
        }

        report.markAsFailed(errorMessage);

        Report savedReport = reportRepository.save(report);

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            "SYSTEM",
            AuditAction.REPORT_FAILED,
            AuditSeverity.MEDIUM,
            "Report generation failed - " + report.getReportReference() + ": " + errorMessage,
            "Report",
            savedReport.getId().toString()
        );

        return savedReport;
    }

    public Report cancelReport(Long reportId, String cancelledBy) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (report.getStatus() == ReportStatus.COMPLETED || report.getStatus() == ReportStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel completed or failed reports");
        }

        report.setStatus(ReportStatus.CANCELLED);

        Report savedReport = reportRepository.save(report);

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            cancelledBy,
            AuditAction.REPORT_CANCELLED,
            AuditSeverity.LOW,
            "Report cancelled - " + report.getReportReference(),
            "Report",
            savedReport.getId().toString()
        );

        return savedReport;
    }

    public void logReportDownload(Long reportId, String downloadedBy) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (!report.canBeDownloaded()) {
            throw new IllegalStateException("Report cannot be downloaded");
        }

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            downloadedBy,
            AuditAction.REPORT_DOWNLOADED,
            AuditSeverity.LOW,
            "Report downloaded - " + report.getReportReference() + " (" + report.getFileName() + ")",
            "Report",
            report.getId().toString()
        );
    }

    public void cleanupExpiredReports() {
        List<Report> expiredReports = reportRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                ReportStatus.COMPLETED, LocalDateTime.now());

        for (Report report : expiredReports) {
            report.setStatus(ReportStatus.EXPIRED);
            reportRepository.save(report);

            auditService.logEntityAction(
                report.getRequestedBy().getId(),
                "SYSTEM",
                AuditAction.REPORT_EXPIRED,
                AuditSeverity.LOW,
                "Report expired and marked for cleanup - " + report.getReportReference(),
                "Report",
                report.getId().toString()
            );
        }
    }

    public void deleteReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with ID: " + reportId));

        if (report.getStatus() == ReportStatus.GENERATING) {
            throw new IllegalStateException("Cannot delete reports that are currently generating");
        }

        auditService.logEntityAction(
            report.getRequestedBy().getId(),
            report.getRequestedBy().getUsername(),
            AuditAction.REPORT_CANCELLED,
            AuditSeverity.MEDIUM,
            "Report deleted - " + report.getReportReference(),
            "Report",
            report.getId().toString()
        );

        reportRepository.delete(report);
    }

    // Finder methods
    public Optional<Report> findById(Long id) {
        return reportRepository.findById(id);
    }

    public Optional<Report> findByReference(String reference) {
        return reportRepository.findByReportReference(reference);
    }

    public List<Report> findReportsByUser(User user) {
        return reportRepository.findByRequestedByOrderByCreatedAtDesc(user);
    }

    public List<Report> findReportsByUserAndStatus(User user, ReportStatus status) {
        return reportRepository.findByRequestedByAndStatusOrderByCreatedAtDesc(user, status);
    }

    public List<Report> findReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Report> findReportsByType(ReportType reportType) {
        return reportRepository.findByReportTypeOrderByCreatedAtDesc(reportType);
    }

    public List<Report> findReportsByFormat(ReportFormat format) {
        return reportRepository.findByFormatOrderByCreatedAtDesc(format);
    }

    public List<Report> findRecentReportsByUser(User user) {
        return reportRepository.findTop10ByRequestedByOrderByCreatedAtDesc(user);
    }

    public List<Report> findPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING);
    }

    public List<Report> findStuckReports(int minutesAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesAgo);
        return reportRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(ReportStatus.GENERATING, cutoffTime);
    }

    public List<Report> findExpiredReports() {
        return reportRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                ReportStatus.COMPLETED, LocalDateTime.now());
    }

    public List<Report> findReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    // Helper methods
    private String generateReportReference() {
        String reference;
        do {
            reference = "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (reportRepository.existsByReportReference(reference));
        return reference;
    }
}