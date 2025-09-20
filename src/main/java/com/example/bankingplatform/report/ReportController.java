package com.example.bankingplatform.report;

import com.example.bankingplatform.user.User;
import com.example.bankingplatform.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @Autowired
    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> requestReport(@Valid @RequestBody RequestReportRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            Report report = reportService.requestReport(
                user,
                request.getReportType(),
                request.getTitle(),
                request.getFormat(),
                request.getStartDate(),
                request.getEndDate(),
                request.getDescription(),
                request.getParameters()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReport(@PathVariable Long id) {
        return reportService.findById(id)
            .map(report -> ResponseEntity.ok(report))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<Report> getReportByReference(@PathVariable String reference) {
        return reportService.findByReference(reference)
            .map(report -> ResponseEntity.ok(report))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Report> reports = reportService.findReportsByUser(user);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Report>> getRecentReportsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Report> reports = reportService.findRecentReportsByUser(user);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Report>> getReportsByStatus(@PathVariable ReportStatus status) {
        List<Report> reports = reportService.findReportsByStatus(status);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/type/{reportType}")
    public ResponseEntity<List<Report>> getReportsByType(@PathVariable ReportType reportType) {
        List<Report> reports = reportService.findReportsByType(reportType);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/format/{format}")
    public ResponseEntity<List<Report>> getReportsByFormat(@PathVariable ReportFormat format) {
        List<Report> reports = reportService.findReportsByFormat(format);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Report>> getPendingReports() {
        List<Report> reports = reportService.findPendingReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Report>> getExpiredReports() {
        List<Report> reports = reportService.findExpiredReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/stuck")
    public ResponseEntity<List<Report>> getStuckReports(
            @RequestParam(defaultValue = "30") int minutesAgo) {
        List<Report> reports = reportService.findStuckReports(minutesAgo);
        return ResponseEntity.ok(reports);
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<?> startReportGeneration(@PathVariable Long id, @RequestBody GenerationRequest request) {
        try {
            Report report = reportService.startReportGeneration(id, request.getGeneratedBy());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReportGeneration(@PathVariable Long id, @RequestBody CompleteReportRequest request) {
        try {
            Report report = reportService.completeReportGeneration(
                id,
                request.getFilePath(),
                request.getFileName(),
                request.getFileSizeBytes(),
                request.getContentType(),
                request.getRecordCount()
            );
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/fail")
    public ResponseEntity<?> failReportGeneration(@PathVariable Long id, @RequestBody FailReportRequest request) {
        try {
            Report report = reportService.failReportGeneration(id, request.getErrorMessage());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReport(@PathVariable Long id, @RequestBody CancelRequest request) {
        try {
            Report report = reportService.cancelReport(id, request.getCancelledBy());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<?> logReportDownload(@PathVariable Long id, @RequestBody DownloadRequest request) {
        try {
            reportService.logReportDownload(id, request.getDownloadedBy());
            return ResponseEntity.ok().body("Download logged successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/cleanup-expired")
    public ResponseEntity<?> cleanupExpiredReports() {
        try {
            reportService.cleanupExpiredReports();
            return ResponseEntity.ok().body("Expired reports cleanup completed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Request DTOs
    public static class RequestReportRequest {
        private Integer userId;
        private ReportType reportType;
        private String title;
        private ReportFormat format;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private String parameters;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public ReportType getReportType() { return reportType; }
        public void setReportType(ReportType reportType) { this.reportType = reportType; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public ReportFormat getFormat() { return format; }
        public void setFormat(ReportFormat format) { this.format = format; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
    }

    public static class GenerationRequest {
        private String generatedBy;

        public String getGeneratedBy() { return generatedBy; }
        public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    }

    public static class CompleteReportRequest {
        private String filePath;
        private String fileName;
        private Long fileSizeBytes;
        private String contentType;
        private Integer recordCount;

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
    }

    public static class FailReportRequest {
        private String errorMessage;

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class CancelRequest {
        private String cancelledBy;

        public String getCancelledBy() { return cancelledBy; }
        public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    }

    public static class DownloadRequest {
        private String downloadedBy;

        public String getDownloadedBy() { return downloadedBy; }
        public void setDownloadedBy(String downloadedBy) { this.downloadedBy = downloadedBy; }
    }
}