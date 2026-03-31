package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.entity.Report;

import java.util.List;

public interface ReportService {

    Report submitReport(Long reporterId, ReportRequest request);

    List<Report> getReportsByTarget(String targetType, Long targetId);

    List<Report> getAllPendingReports();

    Report processReport(Long reportId, Long processorId);
}
