package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogAdminStatsVO;
import com.alikeyou.itmoduleblog.dto.BlogDashboardActivityDTO;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogDashboardRankVO;
import com.alikeyou.itmoduleblog.dto.BlogDashboardTrendPointDTO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "博客管理", description = "博客文章的增删改查及互动操作")
@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private static final Set<Integer> ADMIN_REVIEWER_ROLE_IDS = Set.of(1, 2);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

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

    @GetMapping("/admin/stats/trend")
    public ResponseEntity<List<BlogDashboardTrendPointDTO>> getAdminTrend(
            @RequestParam(defaultValue = "7") int days) {
        requireAdminOrReviewer();

        int safeDays = Math.max(3, Math.min(days, 30));
        LocalDate endDate = LocalDate.now(ZoneId.systemDefault());
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Map<String, BlogDashboardTrendPointDTO> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate statDate = startDate.plusDays(i);
            BlogDashboardTrendPointDTO point = new BlogDashboardTrendPointDTO();
            point.setStatDate(statDate.format(DATE_FORMATTER));
            point.setCreatedCount(0L);
            point.setPublishedCount(0L);
            point.setViewCount(0L);
            point.setReportCount(0L);
            trendMap.put(point.getStatDate(), point);
        }

        blogRepository.countCreatedDailySince(startInstant).forEach(row -> {
            BlogDashboardTrendPointDTO point = trendMap.get(row.getStatDate());
            if (point != null) {
                point.setCreatedCount(safeLong(row.getTotal()));
            }
        });
        blogRepository.countPublishedDailySince(startInstant).forEach(row -> {
            BlogDashboardTrendPointDTO point = trendMap.get(row.getStatDate());
            if (point != null) {
                point.setPublishedCount(safeLong(row.getTotal()));
            }
        });
        viewLogRepository.countByTargetTypeDailySince("blog", startInstant).forEach(row -> {
            BlogDashboardTrendPointDTO point = trendMap.get(row.getStatDate());
            if (point != null) {
                point.setViewCount(safeLong(row.getTotal()));
            }
        });
        reportRepository.countByTargetTypeDailySince("blog", startInstant).forEach(row -> {
            BlogDashboardTrendPointDTO point = trendMap.get(row.getStatDate());
            if (point != null) {
                point.setReportCount(safeLong(row.getTotal()));
            }
        });

        return ResponseEntity.ok(List.copyOf(trendMap.values()));
    }

    @GetMapping("/admin/rank/overview")
    public ResponseEntity<BlogDashboardRankVO> getAdminRankOverview(
            @RequestParam(defaultValue = "8") int top) {
        requireAdminOrReviewer();

        int safeTop = Math.max(3, Math.min(top, 20));
        List<Blog> hotBlogs = blogService.getBlogsByHotness();
        BlogDashboardRankVO result = new BlogDashboardRankVO();
        result.setHotBlogs(
                blogService.convertToSecurePreviewResponseList(
                        hotBlogs.stream().limit(safeTop).toList(),
                        requireCurrentUserId(),
                        true
                )
        );
        result.setHotAuthors(buildAuthorRanks(hotBlogs, safeTop));
       result.setHotTags(buildTagRanks(hotBlogs, safeTop));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/activity/recent")
    public ResponseEntity<List<BlogDashboardActivityDTO>> getAdminRecentActivity(
            @RequestParam(defaultValue = "6") int limit) {
        requireAdminOrReviewer();

        int safeLimit = Math.max(4, Math.min(limit, 20));
        int fetchSize = Math.max(safeLimit, 6);
        var pageable = org.springframework.data.domain.PageRequest.of(0, fetchSize);
        List<BlogDashboardActivityDTO> activities = new ArrayList<>();

        blogRepository.findRecentByStatus("published", pageable)
                .forEach(blog -> activities.add(buildBlogActivity(
                        blog,
                        "内容发布上线",
                        "内容发布",
                        "success",
                        "《%s》已发布上线，作者 %s。"
                )));

        blogRepository.findRecentByStatus("pending", pageable)
                .forEach(blog -> activities.add(buildBlogActivity(
                        blog,
                        "博客进入待审",
                        "内容审核",
                        "warning",
                        "《%s》进入审核队列，作者 %s。"
                )));

        blogRepository.findRecentByStatus("rejected", pageable)
                .forEach(blog -> activities.add(buildBlogActivity(
                        blog,
                        "博客驳回复核",
                        "内容复核",
                        "warning",
                        "《%s》被驳回，建议复核驳回原因与内容质量。"
                )));

        reportRepository.findPageByConditions(
                        "blog",
                        null,
                        "pending",
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                fetchSize,
                                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                        )
                )
                .getContent()
                .forEach(report -> activities.add(buildReportActivity(report)));

        List<BlogDashboardActivityDTO> sorted = activities.stream()
                .sorted(Comparator.comparing(BlogDashboardActivityDTO::getTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(sorted);
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

    private BlogDashboardActivityDTO buildBlogActivity(Blog blog,
                                                       String title,
                                                       String category,
                                                       String level,
                                                       String template) {
        BlogDashboardActivityDTO activity = new BlogDashboardActivityDTO();
        activity.setTitle(title);
        activity.setCategory(category);
        activity.setLevel(level);
        activity.setTime(blog != null ? firstNonNull(blog.getPublishTime(), blog.getUpdatedAt(), blog.getCreatedAt()) : null);

        String blogTitle = blog != null && StringUtils.hasText(blog.getTitle()) ? blog.getTitle() : "未命名博客";
        String authorName = resolveDisplayName(blog != null ? blog.getAuthor() : null);
        String desc = String.format(template, blogTitle, authorName);
        activity.setDesc(desc);
        return activity;
    }

    private BlogDashboardActivityDTO buildReportActivity(Report report) {
        BlogDashboardActivityDTO activity = new BlogDashboardActivityDTO();
        activity.setTitle("举报进入待处理");
        activity.setCategory("风险处置");
        activity.setLevel("danger");
        activity.setTime(report != null ? report.getCreatedAt() : null);

        String targetTitle = resolveTargetTitle(report);
        String reporterName = report != null && report.getReporter() != null
                ? resolveDisplayName(report.getReporter())
                : "匿名用户";
        String reason = report != null && StringUtils.hasText(report.getReason())
                ? report.getReason().trim()
                : "未填写举报原因";

        activity.setDesc(String.format("《%s》收到举报，举报人 %s，原因：%s。",
                StringUtils.hasText(targetTitle) ? targetTitle : "未命名内容",
                reporterName,
                reason));
        return activity;
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

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
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

    private List<BlogDashboardRankVO.RankItem> buildAuthorRanks(List<Blog> hotBlogs, int limit) {
        Map<Long, BlogDashboardRankVO.RankItem> authorMap = new LinkedHashMap<>();
        for (Blog blog : hotBlogs) {
            if (blog == null || blog.getAuthor() == null || blog.getAuthor().getId() == null) {
                continue;
            }
            Long authorId = blog.getAuthor().getId();
            BlogDashboardRankVO.RankItem item = authorMap.computeIfAbsent(authorId, key -> {
                BlogDashboardRankVO.RankItem created = new BlogDashboardRankVO.RankItem();
                created.setId(authorId);
                created.setName(resolveDisplayName(blog.getAuthor()));
                created.setCount(0L);
                created.setHeat(0L);
                created.setExtra(blog.getAuthor().getUsername());
                return created;
            });
            item.setCount(item.getCount() + 1L);
            item.setHeat(item.getHeat() + calculateBlogHeat(blog));
        }
        return authorMap.values().stream()
                .sorted(Comparator.comparing(BlogDashboardRankVO.RankItem::getHeat, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BlogDashboardRankVO.RankItem::getCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<BlogDashboardRankVO.RankItem> buildTagRanks(List<Blog> hotBlogs, int limit) {
        Map<String, BlogDashboardRankVO.RankItem> tagMap = new LinkedHashMap<>();
        for (Blog blog : hotBlogs) {
            if (blog == null || blog.getTags() == null || blog.getTags().isEmpty()) {
                continue;
            }
            long heat = calculateBlogHeat(blog);
            blog.getTags().forEach((tagId, tagName) -> {
                String normalizedName = StringUtils.hasText(tagName) ? tagName.trim() : null;
                if (!StringUtils.hasText(normalizedName)) {
                    return;
                }
                BlogDashboardRankVO.RankItem item = tagMap.computeIfAbsent(normalizedName, key -> {
                    BlogDashboardRankVO.RankItem created = new BlogDashboardRankVO.RankItem();
                    created.setId(parseLongOrNull(tagId));
                    created.setName(normalizedName);
                    created.setCount(0L);
                    created.setHeat(0L);
                    created.setExtra("标签热度");
                    return created;
                });
                item.setCount(item.getCount() + 1L);
                item.setHeat(item.getHeat() + heat);
            });
        }
        return tagMap.values().stream()
                .sorted(Comparator.comparing(BlogDashboardRankVO.RankItem::getHeat, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BlogDashboardRankVO.RankItem::getCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private long calculateBlogHeat(Blog blog) {
        if (blog == null) {
            return 0L;
        }
        return safeLong(blog.getViewCount())
                + safeLong(blog.getLikeCount()) * 5L
                + safeLong(blog.getCollectCount()) * 10L
                + safeLong(blog.getDownloadCount()) * 8L;
    }

    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveDisplayName(UserInfo user) {
        if (user == null) {
            return "未知作者";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "未知作者";
    }
}
