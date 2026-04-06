
package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.dto.ReportResponse;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.service.TagService;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "博客管理", description = "博客文章的增删改查及互动操作")
@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;
    @Autowired
    private TagService tagService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogCreateRequest request) {
        AuthorInfo authorInfo = getCurrentUserInfo();
        Blog createdBlog = blogService.createBlog(request, authorInfo);
        return new ResponseEntity<>(blogService.convertToResponse(createdBlog), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id) {
        blogService.incrementViewCount(id);
        return blogService.getBlogById(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getAllBlogs()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(@PathVariable Long id, @RequestBody BlogUpdateRequest request) {
        return blogService.updateBlog(id, request)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(@PathVariable Long id) {
        blogService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BlogResponse>> searchBlogs(@RequestParam String keyword) {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.searchBlogs(keyword)));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BlogResponse>> getBlogsByAuthorId(@PathVariable Long authorId) {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.findByAuthorId(authorId)));
    }

    @GetMapping("/search/tag")
    public ResponseEntity<List<BlogResponse>> searchBlogsByTag(@RequestParam String keyword) {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.searchBlogsByTag(keyword)));
    }

    @GetMapping("/search/author")
    public ResponseEntity<List<BlogResponse>> searchBlogsByAuthor(@RequestParam String keyword) {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.searchBlogsByAuthor(keyword)));
    }

    @GetMapping("/draft")
    public ResponseEntity<List<BlogResponse>> getDraftBlogs() {
        AuthorInfo authorInfo = getCurrentUserInfo();
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.findDraftBlogsByAuthorId(authorInfo.getId())));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<BlogResponse>> getHotBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getBlogsByHotness()));
    }

    @GetMapping("/time/newest")
    public ResponseEntity<List<BlogResponse>> getNewestBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getBlogsByTimeDesc()));
    }

    @GetMapping("/time/oldest")
    public ResponseEntity<List<BlogResponse>> getOldestBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getBlogsByTimeAsc()));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<BlogResponse> rejectBlog(@PathVariable Long id, @RequestBody(required = false) RejectBlogRequest request) {
        String reason = request != null ? request.getReason() : null;
        return blogService.rejectBlog(id, reason, getCurrentUserId())
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}/republish")
    public ResponseEntity<BlogResponse> republishBlog(@PathVariable Long id) {
        return blogService.republishBlog(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<BlogResponse>> getRejectedBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getRejectedBlogs()));
    }

    @GetMapping("/reported")
    public ResponseEntity<List<BlogResponse>> getReportedBlogs() {
        return ResponseEntity.ok(blogService.convertToResponseList(blogService.getReportedBlogs()));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<BlogResponse>> getPendingBlogs(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(blogService.getPendingBlogs(pageable).map(blogService::convertToResponse));
    }

    @PutMapping("/batch")
    public ResponseEntity<Void> batchReviewBlogs(@RequestBody BatchReviewRequest request) {
        blogService.batchReviewBlogs(request.getBlogIds(), request.getStatus(), request.getReason(), getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<BlogResponse> approveBlog(@PathVariable Long id) {
        return blogService.approveBlog(id, getCurrentUserId())
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ReportResponse> reportBlog(@PathVariable Long id, @RequestBody ReportRequest request) {
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Report report = blogService.reportBlog(id, getCurrentUserId(), request.getReason());
        return new ResponseEntity<>(convertToReportResponse(report), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/reports")
    public ResponseEntity<List<ReportResponse>> getReportsByBlogId(@PathVariable Long id) {
        List<ReportResponse> responses = reportService.getReportsByTarget("blog", id).stream()
                .map(this::convertToReportResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/report/approve")
    public ResponseEntity<BlogResponse> approveBlogReports(@PathVariable Long id) {
        reportService.processTargetReports("blog", id, getCurrentUserId(), "approved");
        return blogService.getBlogById(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}/report/reject")
    public ResponseEntity<BlogResponse> rejectBlogReports(@PathVariable Long id) {
        reportService.processTargetReports("blog", id, getCurrentUserId(), "rejected");
        return blogService.getBlogById(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/admin/reports/pending")
    public ResponseEntity<List<ReportResponse>> getAllPendingReports() {
        List<ReportResponse> responses = reportService.getAllPendingReports().stream()
                .map(this::convertToReportResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    static class BatchReviewRequest {
        private List<Long> blogIds;
        private String status;
        private String reason;
        public List<Long> getBlogIds() { return blogIds; }
        public void setBlogIds(List<Long> blogIds) { this.blogIds = blogIds; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    static class RejectBlogRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private ReportResponse convertToReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporter().getId());
        response.setReporterName(report.getReporter().getNickname() != null ? report.getReporter().getNickname() : report.getReporter().getUsername());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setTargetTitle(resolveTargetTitle(report));
        response.setReason(report.getReason());
        response.setStatus(report.getStatus());
        response.setCreatedAt(report.getCreatedAt());
        response.setProcessedAt(report.getProcessedAt());
        if (report.getProcessor() != null) {
            response.setProcessorId(report.getProcessor().getId());
            response.setProcessorName(report.getProcessor().getNickname() != null ? report.getProcessor().getNickname() : report.getProcessor().getUsername());
        }
        return response;
    }

    private String resolveTargetTitle(Report report) {
        if (report == null || report.getTargetType() == null || report.getTargetId() == null) return null;
        if ("blog".equalsIgnoreCase(report.getTargetType())) {
            return blogService.getBlogById(report.getTargetId()).map(Blog::getTitle).orElse(null);
        }
        return null;
    }

    private AuthorInfo getCurrentUserInfo() {
        try {
            Long userId = getCurrentUserIdOrNull();
            if (userId == null) {
                String username = LoginConstant.getUsername();
                if (username != null && !username.isEmpty()) {
                    UserInfo user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new IllegalStateException("用户不存在：" + username));
                    return convertToAuthorInfo(user);
                } else {
                    throw new IllegalStateException("用户未登录");
                }
            }
            UserInfo user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("用户不存在，ID: " + userId));
            return convertToAuthorInfo(user);
        } catch (Exception e) {
            throw new IllegalStateException("获取当前用户信息失败：" + e.getMessage(), e);
        }
    }

    private Long getCurrentUserId() {
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) throw new IllegalStateException("用户未登录");
        return currentUserId;
    }

    private Long getCurrentUserIdOrNull() {
        Long currentUserId = LoginConstant.getUserId();
        if (currentUserId != null) return currentUserId;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) return null;

        Long fromPrincipal = tryExtractUserId(authentication.getPrincipal());
        if (fromPrincipal != null) return fromPrincipal;

        Long fromDetails = tryExtractUserId(authentication.getDetails());
        if (fromDetails != null) return fromDetails;

        return tryExtractUserId(authentication.getName());
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

    private AuthorInfo convertToAuthorInfo(UserInfo userInfo) {
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setId(userInfo.getId());
        authorInfo.setUsername(userInfo.getUsername());
        authorInfo.setNickname(userInfo.getNickname());
        authorInfo.setAvatar(userInfo.getAvatarUrl());
        authorInfo.setEmail(userInfo.getEmail());
        authorInfo.setDisplayName(userInfo.getNickname() != null ? userInfo.getNickname() : userInfo.getUsername());
        return authorInfo;
    }
}
