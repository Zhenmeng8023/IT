package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Report submitReport(Long reporterId, ReportRequest request) {
        if (reporterId == null) {
            throw new IllegalArgumentException("举报人 ID 不能为空");
        }
        if (request.getTargetId() == null) {
            throw new IllegalArgumentException("被举报目标 ID 不能为空");
        }
        if (request.getTargetType() == null || request.getTargetType().trim().isEmpty()) {
            throw new IllegalArgumentException("被举报目标类型不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("举报原因不能为空");
        }

        UserInfo reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("举报人不存在，ID: " + reporterId));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus("pending");
        report.setCreatedAt(Instant.now());

        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByTarget(String targetType, Long targetId) {
        if (targetType == null || targetType.trim().isEmpty()) {
            throw new IllegalArgumentException("目标类型不能为空");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("目标 ID 不能为空");
        }
        return reportRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getAllPendingReports() {
        return reportRepository.findByStatus("pending");
    }

    @Override
    @Transactional
    public Report processReport(Long reportId, Long processorId) {
        if (reportId == null) {
            throw new IllegalArgumentException("举报 ID 不能为空");
        }
        if (processorId == null) {
            throw new IllegalArgumentException("处理人 ID 不能为空");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报记录不存在，ID: " + reportId));

        UserInfo processor = userRepository.findById(processorId)
                .orElseThrow(() -> new IllegalArgumentException("处理人不存在，ID: " + processorId));

        report.setProcessor(processor);
        report.setProcessedAt(Instant.now());
        report.setStatus("processed");

        return reportRepository.save(report);
    }
}
