package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private static final String BLOG_STATUS_PUBLISHED = "published";
    private static final String BLOG_STATUS_PENDING = "pending";
    private static final String REPORT_REJECT_REASON = "举报处理成立，内容已下架";

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogService blogService;

    @Override
    @Transactional
    public Report submitReport(Long reporterId, ReportRequest request) {
        if (reporterId == null) {
            throw badRequest("举报人 ID 不能为空");
        }
        if (request == null || request.getTargetId() == null) {
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

        String normalizedTargetType = request.getTargetType().trim().toLowerCase();
        if (TARGET_TYPE_BLOG.equals(normalizedTargetType)) {
            Blog blog = blogRepository.findById(request.getTargetId())
                    .orElseThrow(() -> notFound("被举报博客不存在，ID: " + request.getTargetId()));
            if (!BLOG_STATUS_PUBLISHED.equals(normalizeNullable(blog.getStatus()))) {
                throw badRequest("仅已发布博客支持举报");
            }
            if (blog.getAuthor() != null && reporterId.equals(blog.getAuthor().getId())) {
                throw badRequest("不能举报自己的博客");
            }
            if (reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporterId, TARGET_TYPE_BLOG, request.getTargetId(), STATUS_PENDING)) {
                throw badRequest("您已经举报过该博客，请勿重复提交");
            }
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(normalizedTargetType);
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setStatus(STATUS_PENDING);
        report.setCreatedAt(Instant.now());
        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByTarget(String targetType, Long targetId) {
        String normalizedTargetType = normalizeNullable(targetType);
        if (normalizedTargetType == null) {
            throw badRequest("目标类型不能为空");
        }
        if (targetId == null) {
            throw badRequest("目标 ID 不能为空");
        }
        return reportRepository.findByTargetTypeAndTargetId(normalizedTargetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getAllPendingReports() {
        return reportRepository.findByStatus(STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> getReportsPage(String targetType, Long targetId, String status, Pageable pageable) {
        return reportRepository.findPageByConditions(
                normalizeNullable(targetType),
                targetId,
                normalizeNullable(status),
                pageable
        );
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
        Report saved = reportRepository.save(report);

        if (STATUS_PROCESSED.equals(normalizedStatus)
                && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizeNullable(saved.getTargetType()))) {
            blogRepository.findById(saved.getTargetId()).ifPresent(blog -> {
                String blogStatus = normalizeNullable(blog.getStatus());
                if (!ACTION_REJECTED.equalsIgnoreCase(blogStatus)
                        && (BLOG_STATUS_PUBLISHED.equals(blogStatus) || BLOG_STATUS_PENDING.equals(blogStatus))) {
                    blogService.rejectBlog(blog.getId(), REPORT_REJECT_REASON, processorId);
                }
            });
        }

        return saved;
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

        if (STATUS_PROCESSED.equals(normalizedStatus)
                && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizedTargetType)) {
            Blog blog = blogRepository.findById(targetId)
                    .orElseThrow(() -> notFound("被举报博客不存在，ID: " + targetId));
            String blogStatus = normalizeNullable(blog.getStatus());
            if (!ACTION_REJECTED.equalsIgnoreCase(blogStatus)
                    && (BLOG_STATUS_PUBLISHED.equals(blogStatus) || BLOG_STATUS_PENDING.equals(blogStatus))) {
                blogService.rejectBlog(targetId, REPORT_REJECT_REASON, processorId);
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
