package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportService {

    Report submitReport(Long reporterId, ReportRequest request);

    List<Report> getReportsByTarget(String targetType, Long targetId);

    List<Report> getAllPendingReports();

    Page<Report> getReportsPage(String targetType, Long targetId, String status, Pageable pageable);

    Report processReport(Long reportId, Long processorId, String status);

    List<Report> processTargetReports(String targetType, Long targetId, Long processorId, String status);
}
