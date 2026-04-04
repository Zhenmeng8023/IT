package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSED = "processed";
    private static final String STATUS_IGNORED = "ignored";
    private static final String ACTION_APPROVED = "approved";
    private static final String ACTION_REJECTED = "rejected";
    private static final String TARGET_TYPE_BLOG = "blog";

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Override
    @Transactional
    public Report submitReport(Long reporterId, ReportRequest request) {
        if (reporterId == null) {
            throw badRequest("举报人 ID 不能为空");
        }
        if (request.getTargetId() == null) {
            throw badRequest("被举报目标 ID 不能为空");
        }
        if (request.getTargetType() == null || request.getTargetType().trim().isEmpty()) {
            throw badRequest("被举报目标类型不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw badRequest("举报原因不能为空");
        }

        UserInfo reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> notFound("举报人不存在，ID: " + reporterId));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus(STATUS_PENDING);
        report.setCreatedAt(Instant.now());

        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByTarget(String targetType, Long targetId) {
        if (targetType == null || targetType.trim().isEmpty()) {
            throw badRequest("目标类型不能为空");
        }
        if (targetId == null) {
            throw badRequest("目标 ID 不能为空");
        }
        return reportRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getAllPendingReports() {
        return reportRepository.findByStatus(STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> getReportsPage(String targetType, Long targetId, String status, Pageable pageable) {
        String normalizedTargetType = normalizeNullable(targetType);
        String normalizedStatus = normalizeNullable(status);
        return reportRepository.findPageByConditions(normalizedTargetType, targetId, normalizedStatus, pageable);
    }

    @Override
    @Transactional
    public Report processReport(Long reportId, Long processorId, String status) {
        if (reportId == null) {
            throw badRequest("举报 ID 不能为空");
        }
        if (processorId == null) {
            throw badRequest("处理人 ID 不能为空");
        }

        String normalizedStatus = normalizeHandleStatus(status);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> notFound("举报记录不存在，ID: " + reportId));
        if (!STATUS_PENDING.equalsIgnoreCase(normalizeNullable(report.getStatus()))) {
            throw badRequest("该举报已处理，无需重复操作");
        }

        UserInfo processor = userRepository.findById(processorId)
                .orElseThrow(() -> notFound("处理人不存在，ID: " + processorId));

        report.setProcessor(processor);
        report.setProcessedAt(Instant.now());
        report.setStatus(normalizedStatus);

        if (STATUS_PROCESSED.equals(normalizedStatus) && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizeNullable(report.getTargetType()))) {
            Blog blog = blogRepository.findById(report.getTargetId())
                    .orElseThrow(() -> notFound("被举报博客不存在，ID: " + report.getTargetId()));
            if (!ACTION_REJECTED.equalsIgnoreCase(normalizeNullable(blog.getStatus()))) {
                blog.setStatus(ACTION_REJECTED);
                blog.setUpdatedAt(Instant.now());
                blogRepository.save(blog);
            }
        }

        return reportRepository.save(report);
    }

    @Override
    @Transactional
    public List<Report> processTargetReports(String targetType, Long targetId, Long processorId, String status) {
        String normalizedTargetType = normalizeNullable(targetType);
        if (normalizedTargetType == null) {
            throw badRequest("目标类型不能为空");
        }
        if (targetId == null) {
            throw badRequest("目标 ID 不能为空");
        }
        if (processorId == null) {
            throw badRequest("处理人 ID 不能为空");
        }

        String normalizedStatus = normalizeHandleStatus(status);
        UserInfo processor = userRepository.findById(processorId)
                .orElseThrow(() -> notFound("处理人不存在，ID: " + processorId));

        List<Report> reports = reportRepository.findByTargetTypeAndTargetIdAndStatus(
                normalizedTargetType,
                targetId,
                STATUS_PENDING
        );
        if (reports.isEmpty()) {
            throw badRequest("当前目标没有待处理举报");
        }

        Instant processedAt = Instant.now();
        reports.forEach(report -> {
            report.setProcessor(processor);
            report.setProcessedAt(processedAt);
            report.setStatus(normalizedStatus);
        });
        List<Report> savedReports = reportRepository.saveAll(reports);

        if (STATUS_PROCESSED.equals(normalizedStatus) && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizedTargetType)) {
            Blog blog = blogRepository.findById(targetId)
                    .orElseThrow(() -> notFound("被举报博客不存在，ID: " + targetId));
            if (!ACTION_REJECTED.equalsIgnoreCase(normalizeNullable(blog.getStatus()))) {
                blog.setStatus(ACTION_REJECTED);
                blog.setUpdatedAt(Instant.now());
                blogRepository.save(blog);
            }
        }

        return savedReports;
    }

    private String normalizeHandleStatus(String status) {
        String normalizedStatus = normalizeNullable(status);
        if (normalizedStatus == null) {
            throw badRequest("处理结果不能为空");
        }
        if (ACTION_APPROVED.equals(normalizedStatus) || STATUS_PROCESSED.equals(normalizedStatus)) {
            return STATUS_PROCESSED;
        }
        if (ACTION_REJECTED.equals(normalizedStatus) || STATUS_IGNORED.equals(normalizedStatus)) {
            return STATUS_IGNORED;
        }
        throw badRequest("处理结果必须是 approved/rejected 或 processed/ignored");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
