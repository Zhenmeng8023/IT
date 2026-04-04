package com.alikeyou.itmoduleblog.controller;

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
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "创建举报", description = "创建一条举报记录；当 targetType=blog 时会复用博客举报的业务校验")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功提交举报",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效", content = @Content),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content)
    })
    @PostMapping("/api/reports")
    public ResponseEntity<ReportResponse> createReport(
            @Parameter(description = "举报请求对象", required = true)
            @RequestBody ReportRequest request) {
        Long currentUserId = requireCurrentUserId();
        Report report;
        if (request != null && TARGET_TYPE_BLOG.equalsIgnoreCase(normalizeNullable(request.getTargetType()))) {
            report = blogService.reportBlog(request.getTargetId(), currentUserId, request.getReason());
        } else {
            report = reportService.submitReport(currentUserId, request);
        }
        return new ResponseEntity<>(convertToReportResponse(report), HttpStatus.CREATED);
    }

    @Operation(summary = "分页获取举报列表", description = "按目标类型、目标 ID、状态分页查询举报记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取举报列表",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限", content = @Content)
    })
    @GetMapping("/api/admin/reports/page")
    public ResponseEntity<Page<ReportResponse>> getReportsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String status) {
        requireAdmin();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReportResponse> reportPage = reportService.getReportsPage(
                normalizeNullable(targetType),
                targetId,
                normalizeNullable(status),
                pageable
        ).map(this::convertToReportResponse);
        return ResponseEntity.ok(reportPage);
    }

    @Operation(summary = "获取待处理举报", description = "获取所有待处理的举报记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取待处理举报",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReportResponse.class)))),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限", content = @Content)
    })
    @GetMapping("/api/admin/reports/pending")
    public ResponseEntity<List<ReportResponse>> getPendingReports() {
        requireAdmin();
        List<ReportResponse> responses = reportService.getAllPendingReports().stream()
                .map(this::convertToReportResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "处理举报", description = "将举报处理为举报成立或举报驳回")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "处理成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效", content = @Content),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content),
            @ApiResponse(responseCode = "403", description = "无权限", content = @Content)
    })
    @PutMapping("/api/reports/{id}/handle")
    public ResponseEntity<ReportResponse> handleReport(
            @PathVariable Long id,
            @RequestBody ReportHandleRequest request) {
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

    private Long requireAdmin() {
        Long currentUserId = requireCurrentUserId();
        Integer roleId = LoginConstant.getRoleId();
        if (roleId == null) {
            roleId = userRepository.findById(currentUserId)
                    .map(UserInfo::getRoleId)
                    .orElse(null);
        }
        if (roleId == null || (roleId != 1 && roleId != 2)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限处理举报");
        }
        return currentUserId;
    }

    private Long requireCurrentUserId() {
        Long currentUserId = LoginConstant.getUserId();
        if (currentUserId != null) {
            return currentUserId;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再操作");
        }

        Long fromPrincipal = tryExtractUserId(authentication.getPrincipal());
        if (fromPrincipal != null) {
            return fromPrincipal;
        }

        Long fromDetails = tryExtractUserId(authentication.getDetails());
        if (fromDetails != null) {
            return fromDetails;
        }

        Long fromName = tryExtractUserId(authentication.getName());
        if (fromName != null) {
            return fromName;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再操作");
    }

    private Long tryExtractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof CharSequence text) {
            String value = text.toString().trim();
            if (!StringUtils.hasText(value) || "anonymousUser".equalsIgnoreCase(value)) {
                return null;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return userRepository.findByUsername(value)
                        .map(UserInfo::getId)
                        .orElse(null);
            }
        }
        return null;
    }

    private String resolveDisplayName(UserInfo user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return null;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }
}
