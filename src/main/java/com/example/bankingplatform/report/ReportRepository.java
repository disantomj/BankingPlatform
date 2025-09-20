package com.example.bankingplatform.report;

import com.example.bankingplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportReference(String reportReference);

    boolean existsByReportReference(String reportReference);

    List<Report> findByRequestedByOrderByCreatedAtDesc(User requestedBy);

    List<Report> findByRequestedByAndStatusOrderByCreatedAtDesc(User requestedBy, ReportStatus status);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findByReportTypeOrderByCreatedAtDesc(ReportType reportType);

    List<Report> findByReportTypeAndStatusOrderByCreatedAtDesc(ReportType reportType, ReportStatus status);

    List<Report> findByFormatOrderByCreatedAtDesc(ReportFormat format);

    List<Report> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    List<Report> findByRequestedByAndCreatedAtBetweenOrderByCreatedAtDesc(User requestedBy, LocalDateTime startDate, LocalDateTime endDate);

    List<Report> findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(ReportStatus status, LocalDateTime cutoffTime);

    List<Report> findByRequestedByAndReportTypeOrderByCreatedAtDesc(User requestedBy, ReportType reportType);

    List<Report> findTop10ByRequestedByOrderByCreatedAtDesc(User requestedBy);

    List<Report> findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(ReportStatus status, LocalDateTime cutoffTime);
}