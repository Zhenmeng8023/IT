package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogAdminStatsVO;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogRecommendationResult;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogSearchRequest;
import com.alikeyou.itmoduleblog.dto.BlogSearchResult;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogRecommendationService;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmoduleblog.service.ViewLogService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.dto.ReportRequest;
import com.alikeyou.itmodulecommon.dto.ReportResponse;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "博客管理", description = "博客文章的增删改查及互动操作")
@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private static final Set<Integer> ADMIN_REVIEWER_ROLE_IDS = Set.of(1, 2);

    @Autowired
    private BlogService blogService;
    @Autowired
    private BlogRecommendationService blogRecommendationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ViewLogService viewLogService;
    
    @Autowired
    private com.alikeyou.itmoduleblog.repository.ViewLogRepository viewLogRepository;
    @Autowired
    private com.alikeyou.itmoduleblog.repository.BlogRepository blogRepository;
    @Autowired
    private ReportRepository reportRepository;

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(@RequestBody BlogCreateRequest request) {
        AuthorInfo authorInfo = getCurrentUserInfo();
        Blog createdBlog = blogService.createBlog(request, authorInfo);
        return new ResponseEntity<>(blogService.convertToResponse(createdBlog), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id, HttpServletRequest request) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return blogService.getBlogByIdVisible(id, viewerId, adminReviewer)
                .map(blog -> {
                    boolean acceptedView = viewLogService.recordBlogView(
                            id,
                            viewerId,
                            resolveClientIp(request),
                            request == null ? null : request.getHeader("User-Agent")
                    );
                    if (acceptedView) {
                        blogService.incrementViewCount(id);
                    }
                    return ResponseEntity.ok(blogService.convertToSecureResponse(blog, viewerId, adminReviewer));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<BlogRecommendationResult> getBlogRecommendations(@PathVariable Long id,
                                                                           @RequestParam(defaultValue = "6") int size,
                                                                           @RequestParam(defaultValue = "false") boolean forceRefresh) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return blogService.getBlogByIdVisible(id, viewerId, adminReviewer)
                .map(blog -> ResponseEntity.ok(blogRecommendationService.getRecommendations(id, viewerId, size, forceRefresh)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecurePreviewResponseList(blogService.getAllBlogs(), viewerId, adminReviewer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(@PathVariable Long id, @RequestBody BlogUpdateRequest request) {
        Long operatorId = requireCurrentUserId();
        boolean adminReviewer = isAdminReviewer(operatorId);
        return blogService.updateBlog(id, request, operatorId, adminReviewer)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        Long operatorId = requireCurrentUserId();
        boolean adminReviewer = isAdminReviewer(operatorId);
        blogService.deleteBlog(id, operatorId, adminReviewer);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<Void> downloadBlog(@PathVariable Long id) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return blogService.getBlogByIdVisible(id, viewerId, adminReviewer)
                .map(blog -> {
                    BlogResponse response = blogService.convertToSecureResponse(blog, viewerId, adminReviewer);
                    if (!Boolean.TRUE.equals(response.getCanDownload())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
                    blogService.incrementDownloadCount(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BlogResponse>> searchBlogs(@RequestParam String keyword,
                                                          @RequestParam(required = false) String sort,
                                                          @RequestParam(required = false) String status) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        BlogSearchRequest request = new BlogSearchRequest();
        request.setKeyword(keyword);
        request.setScope("all");
        request.setSort(sort);
        request.setStatus(status);
        BlogSearchResult result = blogService.searchBlogs(request, viewerId, adminReviewer);
        return ResponseEntity.ok(result.getItems());
    }

    @GetMapping("/search/query")
    public ResponseEntity<BlogSearchResult> queryBlogs(@ModelAttribute BlogSearchRequest request) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.searchBlogs(request, viewerId, adminReviewer));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BlogResponse>> getBlogsByAuthorId(@PathVariable Long authorId) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecurePreviewResponseList(
                blogService.findByAuthorIdVisible(authorId, viewerId, adminReviewer),
                viewerId,
                adminReviewer
        ));
    }

    /**
     * 获取当前用户的知识产品（付费博客）
     * 包括所有状态的博客（草稿、已发布等）
     */
    @GetMapping("/my/knowledge-products")
    public ResponseEntity<List<BlogResponse>> getMyKnowledgeProducts() {
        Long currentUserId = requireCurrentUserId();
        List<Blog> blogs = blogService.findByAuthorId(currentUserId);
        return ResponseEntity.ok(blogService.convertToSecureResponseList(blogs, currentUserId, false));
    }
    
    /**
     * 获取用户的浏览历史记录
     */
    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<com.alikeyou.itmoduleblog.entity.ViewLog>> getUserViewLogs(@PathVariable Long userId) {
        requireSelfOrAdmin(userId);
        List<com.alikeyou.itmoduleblog.entity.ViewLog> logs = viewLogRepository.findByUserIdAndCreatedAtBetween(
            userId,
            java.time.Instant.now().minus(java.time.Duration.ofDays(30)),
            java.time.Instant.now()
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search/tag")
    public ResponseEntity<List<BlogResponse>> searchBlogsByTag(@RequestParam String keyword,
                                                               @RequestParam(required = false) String sort,
                                                               @RequestParam(required = false) String status) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        BlogSearchRequest request = new BlogSearchRequest();
        request.setKeyword(keyword);
        request.setScope("tag");
        request.setSort(sort);
        request.setStatus(status);
        BlogSearchResult result = blogService.searchBlogs(request, viewerId, adminReviewer);
        return ResponseEntity.ok(result.getItems());
    }

    @GetMapping("/search/author")
    public ResponseEntity<List<BlogResponse>> searchBlogsByAuthor(@RequestParam String keyword,
                                                                  @RequestParam(required = false) String sort,
                                                                  @RequestParam(required = false) String status) {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        BlogSearchRequest request = new BlogSearchRequest();
        request.setKeyword(keyword);
        request.setScope("author");
        request.setSort(sort);
        request.setStatus(status);
        BlogSearchResult result = blogService.searchBlogs(request, viewerId, adminReviewer);
        return ResponseEntity.ok(result.getItems());
    }

    @GetMapping("/draft")
    public ResponseEntity<List<BlogResponse>> getDraftBlogs() {
        Long currentUserId = requireCurrentUserId();
        return ResponseEntity.ok(blogService.convertToSecureResponseList(blogService.findDraftBlogsByAuthorId(currentUserId), currentUserId, false));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<BlogResponse>> getHotBlogs() {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecurePreviewResponseList(blogService.getBlogsByHotness(), viewerId, adminReviewer));
    }

    @GetMapping("/admin/stats/overview")
    public ResponseEntity<BlogAdminStatsVO> getAdminStatsOverview() {
        requireAdminOrReviewer();

        Instant todayStart = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        BlogAdminStatsVO stats = new BlogAdminStatsVO();
        stats.setTotalBlogs(blogRepository.count());
        stats.setPendingBlogs(blogRepository.countByStatus("pending"));
        stats.setPublishedBlogs(blogRepository.countByStatus("published"));
        stats.setRejectedBlogs(blogRepository.countByStatus("rejected"));
        stats.setPendingReports(reportRepository.countByTargetTypeAndStatus("blog", "pending"));
        stats.setTodayReports(reportRepository.countByTargetTypeAndCreatedAtGreaterThanEqual("blog", todayStart));
        stats.setTodayViews(viewLogRepository.countByTargetTypeAndCreatedAtGreaterThanEqual("blog", todayStart));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/time/newest")
    public ResponseEntity<List<BlogResponse>> getNewestBlogs() {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecurePreviewResponseList(blogService.getBlogsByTimeDesc(), viewerId, adminReviewer));
    }

    @GetMapping("/time/oldest")
    public ResponseEntity<List<BlogResponse>> getOldestBlogs() {
        Long viewerId = getCurrentUserIdOrNull();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecurePreviewResponseList(blogService.getBlogsByTimeAsc(), viewerId, adminReviewer));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<BlogResponse> rejectBlog(@PathVariable Long id, @RequestBody(required = false) RejectBlogRequest request) {
        Long operatorId = requireAdminOrReviewer();
        String reason = request != null ? request.getReason() : null;
        return blogService.rejectBlog(id, reason, operatorId)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}/republish")
    public ResponseEntity<BlogResponse> republishBlog(@PathVariable Long id) {
        Long operatorId = requireCurrentUserId();
        boolean adminReviewer = isAdminReviewer(operatorId);
        ensureAuthorOrAdmin(id, operatorId, adminReviewer);
        return blogService.republishBlog(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<BlogResponse>> getRejectedBlogs() {
        Long viewerId = requireCurrentUserId();
        boolean adminReviewer = isAdminReviewer(viewerId);
        return ResponseEntity.ok(blogService.convertToSecureResponseList(blogService.getRejectedBlogs(viewerId, adminReviewer), viewerId, adminReviewer));
    }

    @GetMapping("/reported")
    public ResponseEntity<List<BlogResponse>> getReportedBlogs() {
        Long viewerId = requireAdminOrReviewer();
        return ResponseEntity.ok(blogService.convertToSecureResponseList(blogService.getReportedBlogs(), viewerId, true));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<BlogResponse>> getPendingBlogs(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        requireAdminOrReviewer();
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(blogService.getPendingBlogs(pageable).map(blogService::convertToResponse));
    }

    @PutMapping("/batch")
    public ResponseEntity<Void> batchReviewBlogs(@RequestBody BatchReviewRequest request) {
        Long operatorId = requireAdminOrReviewer();
        blogService.batchReviewBlogs(request.getBlogIds(), request.getStatus(), request.getReason(), operatorId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<BlogResponse> approveBlog(@PathVariable Long id) {
        Long operatorId = requireAdminOrReviewer();
        return blogService.approveBlog(id, operatorId)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ReportResponse> reportBlog(@PathVariable Long id, @RequestBody ReportRequest request) {
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long currentUserId = requireCurrentUserId();
        Report report = blogService.reportBlog(id, currentUserId, request.getReason());
        return new ResponseEntity<>(convertToReportResponse(report), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/reports")
    public ResponseEntity<List<ReportResponse>> getReportsByBlogId(@PathVariable Long id) {
        requireAdminOrReviewer();
        List<ReportResponse> responses = reportService.getReportsByTarget("blog", id).stream()
                .map(this::convertToReportResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/report/approve")
    public ResponseEntity<BlogResponse> approveBlogReports(@PathVariable Long id) {
        Long operatorId = requireAdminOrReviewer();
        reportService.processTargetReports("blog", id, operatorId, "approved");
        return blogService.getBlogById(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}/report/reject")
    public ResponseEntity<BlogResponse> rejectBlogReports(@PathVariable Long id) {
        Long operatorId = requireAdminOrReviewer();
        reportService.processTargetReports("blog", id, operatorId, "rejected");
        return blogService.getBlogById(id)
                .map(blog -> ResponseEntity.ok(blogService.convertToResponse(blog)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/admin/reports/pending")
    public ResponseEntity<List<ReportResponse>> getAllPendingReports() {
        requireAdminOrReviewer();
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
        response.setReporterId(report.getReporter() != null ? report.getReporter().getId() : null);
        response.setReporterName(report.getReporter() != null
                ? (report.getReporter().getNickname() != null ? report.getReporter().getNickname() : report.getReporter().getUsername())
                : null);
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setTargetTitle(resolveTargetTitle(report));
        response.setReason(report.getReason());
        response.setStatus(normalizeNullable(report.getStatus()));
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
        Long userId = requireCurrentUserId();
        UserInfo user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前登录用户不存在"));
        return convertToAuthorInfo(user);
    }

    private Long requireCurrentUserId() {
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再操作");
        return currentUserId;
    }

    private Long requireAdminOrReviewer() {
        Long currentUserId = requireCurrentUserId();
        if (!isAdminReviewer(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户没有审核或管理权限");
        }
        return currentUserId;
    }

    private void requireSelfOrAdmin(Long targetUserId) {
        Long currentUserId = requireCurrentUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            return;
        }
        if (!isAdminReviewer(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅本人或管理员可查看该资源");
        }
    }

    private boolean isAdminReviewer(Long userId) {
        Integer roleId = LoginConstant.getRoleId();
        if (roleId == null && userId != null) {
            roleId = userRepository.findById(userId).map(UserInfo::getRoleId).orElse(null);
        }
        return roleId != null && ADMIN_REVIEWER_ROLE_IDS.contains(roleId);
    }

    private void ensureAuthorOrAdmin(Long blogId, Long operatorId, boolean adminReviewer) {
        if (adminReviewer) {
            return;
        }
        Blog blog = blogService.getBlogById(blogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "博客不存在"));
        Long authorId = blog.getAuthor() != null ? blog.getAuthor().getId() : null;
        if (authorId == null || !Objects.equals(authorId, operatorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅作者本人或管理员可执行该操作");
        }
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

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
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

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }
}
