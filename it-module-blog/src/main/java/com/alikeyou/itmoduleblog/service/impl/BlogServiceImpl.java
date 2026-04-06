package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.exception.BlogException;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private static final String BLOG_TARGET_TYPE = "blog";
    private static final String REPORT_STATUS_PENDING = "pending";
    private static final long REPORTED_BLOG_MIN_COUNT = 3L;
    private static final long AUTO_REJECT_REPORT_COUNT = 10L;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Override
    @Transactional
    public Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo) {
        if (request == null) {
            throw new BlogException("创建博客的请求参数不能为空");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BlogException("博客标题不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BlogException("博客内容不能为空");
        }
        if (authorInfo == null || authorInfo.getId() == null) {
            throw new BlogException("作者信息不能为空");
        }

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        if (hasSummaryGetter(request)) {
            blog.setSummary(readSummary(request));
        }
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            if (tags.size() != request.getTagIds().size()) {
                Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                Set<Long> invalidTagIds = request.getTagIds().stream()
                        .filter(id -> !validTagIds.contains(id))
                        .collect(Collectors.toSet());
                throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
            }
            blog.setTags(buildTagsMap(tags));
        }

        UserInfo author = userRepository.findById(authorInfo.getId())
                .orElseThrow(() -> new BlogException("用户不存在，ID: " + authorInfo.getId()));
        blog.setAuthor(author);

        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = "draft";
        }
        blog.setStatus(status);

        if ("published".equalsIgnoreCase(status)) {
            blog.setPublishTime(Instant.now());
        }

        if (request.getPrice() != null) {
            blog.setPrice(request.getPrice());
        } else {
            blog.setPrice(0);
        }

        blog.setIsMarked(false);
        blog.setViewCount(0);
        blog.setLikeCount(0);
        blog.setCollectCount(0);
        blog.setDownloadCount(0);
        blog.setCreatedAt(Instant.now());
        blog.setUpdatedAt(Instant.now());

        return blogRepository.save(blog);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Blog> getBlogById(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return blogRepository.findWithAssociationsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getAllBlogs() {
        return blogRepository.findPublishedBlogs();
    }

    @Override
    @Transactional
    public Optional<Blog> updateBlog(Long blogId, BlogUpdateRequest request) {
        if (blogId == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        if (request == null) {
            throw new BlogException("更新请求参数不能为空");
        }

        return blogRepository.findById(blogId).map(blog -> {
            if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
                throw new BlogException("博客标题不能为空");
            }
            if (request.getContent() != null && request.getContent().trim().isEmpty()) {
                throw new BlogException("博客内容不能为空");
            }

            if (request.getTitle() != null) {
                blog.setTitle(request.getTitle());
            }
            if (hasSummaryGetter(request)) {
                String summary = readSummary(request);
                if (summary != null) {
                    blog.setSummary(summary);
                }
            }
            if (request.getContent() != null) {
                blog.setContent(request.getContent());
            }
            if (request.getCoverImageUrl() != null) {
                blog.setCoverImageUrl(request.getCoverImageUrl());
            }

            if (request.getTagIds() != null) {
                List<Tag> tags = tagRepository.findAllById(request.getTagIds());
                if (tags.size() != request.getTagIds().size()) {
                    Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                    Set<Long> invalidTagIds = request.getTagIds().stream()
                            .filter(id -> !validTagIds.contains(id))
                            .collect(Collectors.toSet());
                    throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
                }
                blog.setTags(buildTagsMap(tags));
            }

            if (request.getStatus() != null) {
                blog.setStatus(request.getStatus());
                if ("published".equalsIgnoreCase(request.getStatus()) && blog.getPublishTime() == null) {
                    blog.setPublishTime(Instant.now());
                }
            }

            if (request.getIsMarked() != null) {
                blog.setIsMarked(request.getIsMarked());
            }

            if (request.getPublishTime() != null) {
                blog.setPublishTime(request.getPublishTime());
            }

            if (request.getPrice() != null) {
                blog.setPrice(request.getPrice());
            }

            blog.setUpdatedAt(Instant.now());
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional
    public void deleteBlog(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.findById(id).orElseThrow(() -> new BlogException("博客不存在，ID: " + id));
        blogRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementViewCount(id);
    }

    @Override
    @Transactional
    public void incrementCollectCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementCollectCount(id);
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementDownloadCount(id);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        blogRepository.incrementLikeCount(id);
    }

    @Override
    public BlogResponse convertToResponse(Blog blog) {
        int reportCount = 0;
        if (blog != null && blog.getId() != null) {
            reportCount = (int) reportRepository.countByTargetTypeAndTargetIdAndStatus(
                    BLOG_TARGET_TYPE,
                    blog.getId(),
                    REPORT_STATUS_PENDING
            );
        }
        return convertToResponse(blog, reportCount);
    }

    private BlogResponse convertToResponse(Blog blog, int reportCount) {
        if (blog == null) {
            return null;
        }

        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setSummary(blog.getSummary());
        response.setContent(blog.getContent());
        response.setCoverImageUrl(blog.getCoverImageUrl());

        if (blog.getTags() != null && !blog.getTags().isEmpty()) {
            List<String> tagNames = new ArrayList<>();
            List<Long> tagIds = new ArrayList<>();
            blog.getTags().forEach((tagId, tagName) -> {
                Long parsedTagId = parseTagId(tagId);
                if (parsedTagId != null) {
                    tagIds.add(parsedTagId);
                }
                tagNames.add(tagName);
            });
            response.setTags(tagNames);
            response.setTagIds(tagIds);
        } else {
            response.setTags(Collections.emptyList());
            response.setTagIds(Collections.emptyList());
        }

        response.setStatus(blog.getStatus());
        response.setIsMarked(blog.getIsMarked());
        response.setPublishTime(blog.getPublishTime());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setViewCount(blog.getViewCount());
        response.setLikeCount(blog.getLikeCount());
        response.setCollectCount(blog.getCollectCount());
        response.setDownloadCount(blog.getDownloadCount());
        response.setReportCount(reportCount);
        response.setPrice(blog.getPrice() != null ? blog.getPrice() : 0);
        response.setRejectReason(resolveRejectReason(blog));

        if (blog.getAuthor() != null) {
            BlogResponse.AuthorInfo authorInfo = new BlogResponse.AuthorInfo();
            authorInfo.setId(blog.getAuthor().getId());
            authorInfo.setUsername(blog.getAuthor().getUsername() != null ? blog.getAuthor().getUsername() : "未知用户");
            authorInfo.setNickname(blog.getAuthor().getNickname());
            authorInfo.setAvatar(blog.getAuthor().getAvatarUrl());
            authorInfo.setDisplayName(blog.getAuthor().getNickname() != null ? blog.getAuthor().getNickname() : blog.getAuthor().getUsername());
            authorInfo.setEmail(blog.getAuthor().getEmail());
            response.setAuthor(authorInfo);
        }

        return response;
    }

    private String resolveRejectReason(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return null;
        }
        List<Report> reports = reportRepository.findByTargetTypeAndTargetId(BLOG_TARGET_TYPE, blog.getId());
        if (reports == null || reports.isEmpty()) {
            return null;
        }
        return reports.stream()
                .filter(item -> item.getReason() != null && !item.getReason().trim().isEmpty())
                .sorted((a, b) -> {
                    Instant ta = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                    Instant tb = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                    return tb.compareTo(ta);
                })
                .map(Report::getReason)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<BlogResponse> convertToResponseList(List<Blog> blogs) {
        Map<Long, Integer> reportCountMap = getBlogReportCountMap(
                blogs.stream().map(Blog::getId).filter(Objects::nonNull).collect(Collectors.toList())
        );
        return blogs.stream()
                .map(blog -> convertToResponse(blog, reportCountMap.getOrDefault(blog.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> searchBlogs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }
        return blogRepository.searchBlogs(keyword);
    }

    @Override
    public List<Blog> findByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new BlogException("作者 ID 不能为空");
        }
        return blogRepository.findByAuthorId(authorId);
    }

    @Override
    public List<Blog> searchBlogsByTag(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }

        String normalizedKeyword = keyword.trim();
        Set<String> exactMatchedTagIds = tagRepository.findAll().stream()
                .filter(tag -> tag.getName() != null && tag.getName().trim().equalsIgnoreCase(normalizedKeyword))
                .map(tag -> String.valueOf(tag.getId()))
                .collect(Collectors.toSet());

        return blogRepository.findPublishedBlogs().stream()
                .filter(blog -> matchesTagKeyword(blog, normalizedKeyword, exactMatchedTagIds))
                .collect(Collectors.toList());
    }

    @Override
    public List<Blog> searchBlogsByAuthor(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BlogException("搜索关键词不能为空");
        }
        return blogRepository.searchBlogsByAuthor(keyword);
    }

    @Override
    public List<Blog> findDraftBlogsByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new BlogException("作者 ID 不能为空");
        }
        return blogRepository.findDraftBlogsByAuthorId(authorId);
    }

    @Override
    public List<Blog> getBlogsByHotness() {
        return blogRepository.findByHotness();
    }

    @Override
    public List<Blog> getBlogsByTimeDesc() {
        return blogRepository.findByTimeDesc();
    }

    @Override
    public List<Blog> getBlogsByTimeAsc() {
        return blogRepository.findByTimeAsc();
    }

    @Override
    @Transactional
    public List<Blog> getReportedBlogs() {
        List<ReportRepository.TargetReportStatsProjection> reportStats =
                reportRepository.findTargetReportStatsByTargetTypeAndMinCountAndStatus(
                        BLOG_TARGET_TYPE,
                        REPORTED_BLOG_MIN_COUNT,
                        REPORT_STATUS_PENDING
                );
        if (reportStats.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> reportCountMap = reportStats.stream()
                .collect(Collectors.toMap(
                        ReportRepository.TargetReportStatsProjection::getTargetId,
                        ReportRepository.TargetReportStatsProjection::getReportCount
                ));

        List<Long> blogIds = reportStats.stream()
                .map(ReportRepository.TargetReportStatsProjection::getTargetId)
                .collect(Collectors.toList());

        List<Blog> blogs = blogRepository.findByIdIn(blogIds);
        if (blogs.isEmpty()) {
            return List.of();
        }

        blogs.stream()
                .filter(blog -> shouldAutoReject(blog, reportCountMap.getOrDefault(blog.getId(), 0L)))
                .forEach(this::markBlogAsRejected);

        Map<Long, Blog> blogMap = blogs.stream().collect(Collectors.toMap(Blog::getId, blog -> blog));

        return blogIds.stream()
                .map(blogMap::get)
                .filter(Objects::nonNull)
                .filter(blog -> !"rejected".equalsIgnoreCase(blog.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<Blog> rejectBlog(Long id) {
        return rejectBlogInternal(id, null, null);
    }

    @Transactional
    public Optional<Blog> rejectBlog(Long id, String reason, Long operatorId) {
        return rejectBlogInternal(id, reason, operatorId);
    }

    private Optional<Blog> rejectBlogInternal(Long id, String reason, Long operatorId) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return blogRepository.findById(id).map(blog -> {
            blog.setStatus("rejected");
            blog.setUpdatedAt(Instant.now());
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional
    public Optional<Blog> republishBlog(Long id) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return blogRepository.findById(id).map(blog -> {
            if (!"rejected".equalsIgnoreCase(blog.getStatus())) {
                throw new BlogException("只有已下架的博客才能重新发布，当前博客状态：" + blog.getStatus());
            }
            blog.setStatus("published");
            blog.setUpdatedAt(Instant.now());
            if (blog.getPublishTime() == null) {
                blog.setPublishTime(Instant.now());
            }
            return blogRepository.save(blog);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getRejectedBlogs() {
        return blogRepository.findRejectedBlogs();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blog> getPendingBlogs(Pageable pageable) {
        return blogRepository.findPendingBlogs(pageable);
    }

    @Transactional
    public Optional<Blog> approveBlog(Long id) {
        return approveBlogInternal(id, null);
    }

    @Transactional
    public Optional<Blog> approveBlog(Long id, Long operatorId) {
        return approveBlogInternal(id, operatorId);
    }

    private Optional<Blog> approveBlogInternal(Long id, Long operatorId) {
        if (id == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return blogRepository.findById(id).map(blog -> {
            blog.setStatus("published");
            blog.setUpdatedAt(Instant.now());
            if (blog.getPublishTime() == null) {
                blog.setPublishTime(Instant.now());
            }
            return blogRepository.save(blog);
        });
    }

    @Transactional
    public void batchReviewBlogs(List<Long> blogIds, String status, String reason) {
        batchReviewBlogs(blogIds, status, reason, null);
    }

    @Transactional
    public void batchReviewBlogs(List<Long> blogIds, String status, String reason, Long operatorId) {
        if (blogIds == null || blogIds.isEmpty()) {
            throw new BlogException("博客 ID 列表不能为空");
        }
        if (status == null || (!"published".equalsIgnoreCase(status) && !"rejected".equalsIgnoreCase(status))) {
            throw new BlogException("审核状态必须是 published 或 rejected");
        }

        for (Long id : blogIds) {
            if ("published".equalsIgnoreCase(status)) {
                approveBlogInternal(id, operatorId);
            } else {
                rejectBlogInternal(id, reason, operatorId);
            }
        }
    }

    @Override
    @Transactional
    public Report reportBlog(Long blogId, Long reporterId, String reason) {
        if (blogId == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        if (reporterId == null) {
            throw new BlogException("举报人 ID 不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BlogException("举报原因不能为空");
        }

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new BlogException("博客不存在，ID: " + blogId));
        if ("rejected".equalsIgnoreCase(blog.getStatus())) {
            throw new BlogException("该博客已下架，无需重复举报");
        }
        if (blog.getAuthor() != null && reporterId.equals(blog.getAuthor().getId())) {
            throw new BlogException("不能举报自己的博客");
        }
        if (reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(
                reporterId, BLOG_TARGET_TYPE, blogId, REPORT_STATUS_PENDING)) {
            throw new BlogException("您已经举报过该博客，请勿重复提交");
        }

        UserInfo reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BlogException("举报人不存在，ID: " + reporterId));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(BLOG_TARGET_TYPE);
        report.setTargetId(blogId);
        report.setReason(reason);
        report.setStatus(REPORT_STATUS_PENDING);
        report.setCreatedAt(Instant.now());

        Report savedReport = reportRepository.save(report);
        long reportCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                BLOG_TARGET_TYPE, blogId, REPORT_STATUS_PENDING);
        if (shouldAutoReject(blog, reportCount)) {
            markBlogAsRejected(blog);
        }
        return savedReport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByBlogId(Long blogId) {
        if (blogId == null) {
            throw new BlogException("博客 ID 不能为空");
        }
        return reportRepository.findByTargetTypeAndTargetId(BLOG_TARGET_TYPE, blogId);
    }

    private Map<String, String> buildTagsMap(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(
                tag -> tag.getId().toString(),
                Tag::getName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private boolean matchesTagKeyword(Blog blog, String keyword, Set<String> exactMatchedTagIds) {
        if (blog.getTags() == null || blog.getTags().isEmpty()) {
            return false;
        }

        if (!exactMatchedTagIds.isEmpty() && blog.getTags().keySet().stream().anyMatch(exactMatchedTagIds::contains)) {
            return true;
        }

        String normalizedKeyword = keyword.toLowerCase();
        return blog.getTags().values().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(tagName -> {
                    if (!exactMatchedTagIds.isEmpty()) {
                        return tagName.equalsIgnoreCase(keyword);
                    }
                    return tagName.toLowerCase().contains(normalizedKeyword);
                });
    }

    private Long parseTagId(String rawTagId) {
        if (rawTagId == null || rawTagId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawTagId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<Long, Integer> getBlogReportCountMap(List<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return reportRepository.findTargetReportStatsByTargetTypeAndTargetIdsAndStatus(
                        BLOG_TARGET_TYPE, blogIds, REPORT_STATUS_PENDING
                ).stream()
                .collect(Collectors.toMap(
                        ReportRepository.TargetReportStatsProjection::getTargetId,
                        stats -> stats.getReportCount().intValue()
                ));
    }

    private boolean shouldAutoReject(Blog blog, long reportCount) {
        return blog != null
                && reportCount >= AUTO_REJECT_REPORT_COUNT
                && !"rejected".equalsIgnoreCase(blog.getStatus());
    }

    private void markBlogAsRejected(Blog blog) {
        blog.setStatus("rejected");
        blog.setUpdatedAt(Instant.now());
        blogRepository.save(blog);
    }

    private boolean hasSummaryGetter(Object source) {
        try {
            source.getClass().getMethod("getSummary");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String readSummary(Object source) {
        try {
            Object value = source.getClass().getMethod("getSummary").invoke(source);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
