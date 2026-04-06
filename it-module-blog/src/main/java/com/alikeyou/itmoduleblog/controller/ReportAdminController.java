
package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.dto.ReportHandleRequest;
import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.dto.ReportResponse;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ReportAdminController {

    private static final String TARGET_TYPE_BLOG = "blog";

    @Autowired
    private ReportService reportService;
    @Autowired
    private BlogService blogService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/api/reports")
    public ResponseEntity<ReportResponse> createReport(@RequestBody ReportRequest request) {
        Long currentUserId = requireCurrentUserId();
        Report report;
        if (request != null && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizeNullable(request.getTargetType()))) {
            report = blogService.reportBlog(request.getTargetId(), currentUserId, request.getReason());
        } else {
            report = reportService.submitReport(currentUserId, request);
        }
        return new ResponseEntity<>(convertToReportResponse(report), HttpStatus.CREATED);
    }

    @GetMapping("/api/admin/reports/page")
    public ResponseEntity<Page<ReportResponse>> getReportsPage(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String targetType,
                                                               @RequestParam(required = false) Long targetId,
                                                               @RequestParam(required = false) String status) {
        requireAdmin();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportResponse> reportPage = reportService.getReportsPage(normalizeNullable(targetType), targetId, normalizeNullable(status), pageable)
                .map(this::convertToReportResponse);
        return ResponseEntity.ok(reportPage);
    }

    @GetMapping("/api/admin/reports/pending")
    public ResponseEntity<List<ReportResponse>> getPendingReports() {
        requireAdmin();
        List<ReportResponse> responses = reportService.getAllPendingReports().stream()
                .map(this::convertToReportResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/api/reports/{id}/handle")
    public ResponseEntity<ReportResponse> handleReport(@PathVariable Long id, @RequestBody ReportHandleRequest request) {
        Long currentUserId = requireAdmin();
        Report report = reportService.processReport(id, currentUserId, request != null ? request.getStatus() : null);
        return ResponseEntity.ok(convertToReportResponse(report));
    }

    private ReportResponse convertToReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporter() != null ? report.getReporter().getId() : null);
        response.setReporterName(resolveDisplayName(report.getReporter()));
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setTargetTitle(resolveTargetTitle(report));
        response.setReason(report.getReason());
        response.setStatus(report.getStatus());
        response.setCreatedAt(report.getCreatedAt());
        response.setProcessedAt(report.getProcessedAt());
        if (report.getProcessor() != null) {
            response.setProcessorId(report.getProcessor().getId());
            response.setProcessorName(resolveDisplayName(report.getProcessor()));
        }
        return response;
    }

    private String resolveTargetTitle(Report report) {
        if (report == null || report.getTargetType() == null || report.getTargetId() == null) return null;
        if (TARGET_TYPE_BLOG.equalsIgnoreCase(report.getTargetType())) {
            return blogService.getBlogById(report.getTargetId()).map(Blog::getTitle).orElse(null);
        }
        return null;
    }

    private Long requireAdmin() {
        Long currentUserId = requireCurrentUserId();
        Integer roleId = LoginConstant.getRoleId();
        if (roleId == null) {
            roleId = userRepository.findById(currentUserId).map(UserInfo::getRoleId).orElse(null);
        }
        if (roleId == null || (roleId != 1 && roleId != 2)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限处理举报");
        }
        return currentUserId;
    }

    private Long requireCurrentUserId() {
        Long currentUserId = LoginConstant.getUserId();
        if (currentUserId != null) return currentUserId;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再操作");
        }

        Long fromPrincipal = tryExtractUserId(authentication.getPrincipal());
        if (fromPrincipal != null) return fromPrincipal;
        Long fromDetails = tryExtractUserId(authentication.getDetails());
        if (fromDetails != null) return fromDetails;
        Long fromName = tryExtractUserId(authentication.getName());
        if (fromName != null) return fromName;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再操作");
    }

    private Long tryExtractUserId(Object source) {
        if (source == null) return null;
        if (source instanceof Number number) return number.longValue();
        if (source instanceof CharSequence text) {
            String value = text.toString().trim();
            if (!StringUtils.hasText(value) || "anonymousUser".equalsIgnoreCase(value)) return null;
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return userRepository.findByUsername(value).map(UserInfo::getId).orElse(null);
            }
        }
        return null;
    }

    private String resolveDisplayName(UserInfo user) {
        if (user == null) return null;
        if (StringUtils.hasText(user.getNickname())) return user.getNickname().trim();
        if (StringUtils.hasText(user.getUsername())) return user.getUsername().trim();
        return null;
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }
}
