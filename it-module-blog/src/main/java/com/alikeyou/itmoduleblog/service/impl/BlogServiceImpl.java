package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.dto.BlogSearchRequest;
import com.alikeyou.itmoduleblog.dto.BlogSearchResult;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.BlogAuditLog;
import com.alikeyou.itmoduleblog.exception.BlogException;
import com.alikeyou.itmoduleblog.repository.BlogAuditLogRepository;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogAutoAuditService;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmodulecommon.entity.Report;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulepayment.entity.Membership;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.UserPurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private static final String BLOG_TARGET_TYPE = "blog";
    private static final String BLOG_STATUS_DRAFT = "draft";
    private static final String BLOG_STATUS_PENDING = "pending";
    private static final String BLOG_STATUS_PUBLISHED = "published";
    private static final String BLOG_STATUS_REJECTED = "rejected";
    private static final String AUDIT_TYPE_AUTO = "AUTO";
    private static final String AUDIT_TYPE_MANUAL = "MANUAL";
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final String AUDIT_STATUS_APPROVED = "APPROVED";
    private static final String AUDIT_STATUS_REJECTED = "REJECTED";
    private static final String REPORT_STATUS_PENDING = "pending";
    private static final String REPORT_STATUS_PROCESSED = "processed";
    private static final String REPORT_STATUS_IGNORED = "ignored";
    private static final long REPORTED_BLOG_MIN_COUNT = 3L;
    private static final int PREVIEW_LENGTH = 220;
    private static final Set<String> BLOG_EDITABLE_STATUSES = Set.of(BLOG_STATUS_DRAFT, BLOG_STATUS_PENDING, BLOG_STATUS_REJECTED, BLOG_STATUS_PUBLISHED);
    private static final Set<String> BLOG_WRITABLE_STATUSES = Set.of(BLOG_STATUS_DRAFT, BLOG_STATUS_PENDING);
    private static final Set<String> BLOG_SEARCHABLE_STATUSES = Set.of(BLOG_STATUS_DRAFT, BLOG_STATUS_PENDING, BLOG_STATUS_PUBLISHED, BLOG_STATUS_REJECTED);
    private static final String SEARCH_SCOPE_ALL = "all";
    private static final String SEARCH_SCOPE_TAG = "tag";
    private static final String SEARCH_SCOPE_AUTHOR = "author";
    private static final String SEARCH_SCOPE_CONTENT = "content";
    private static final String SEARCH_SORT_RELEVANCE = "relevance";
    private static final String SEARCH_SORT_NEWEST = "newest";
    private static final String SEARCH_SORT_HOT = "hot";
    private static final String LOCK_TYPE_NONE = "none";
    private static final String LOCK_TYPE_VIP = "vip";
    private static final String LOCK_TYPE_PAID = "paid";
    private static final String LOCK_TYPE_HIDDEN = "hidden";
    private static final String LOCK_REASON_NONE = "none";
    private static final String LOCK_REASON_VIP_REQUIRED = "vip_required";
    private static final String LOCK_REASON_PAYMENT_REQUIRED = "payment_required";
    private static final String LOCK_REASON_STATUS_HIDDEN = "status_hidden";

    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private BlogAuditLogRepository blogAuditLogRepository;
    @Autowired
    private PaidContentRepository paidContentRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private MembershipLevelRepository membershipLevelRepository;
    @Autowired
    private UserPurchaseRepository userPurchaseRepository;
    @Autowired
    private BlogAutoAuditService blogAutoAuditService;
    
    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public Blog createBlog(BlogCreateRequest request, AuthorInfo authorInfo) {
        if (request == null) throw new BlogException("创建博客的请求参数不能为空");
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) throw new BlogException("博客标题不能为空");
        if (request.getContent() == null || request.getContent().trim().isEmpty()) throw new BlogException("博客内容不能为空");
        if (authorInfo == null || authorInfo.getId() == null) throw new BlogException("作者信息不能为空");

        Blog blog = new Blog();
        blog.setTitle(request.getTitle().trim());
        if (hasSummaryGetter(request)) {
            blog.setSummary(readSummary(request));
        }
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            if (tags.size() != request.getTagIds().size()) {
                Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                Set<Long> invalidTagIds = request.getTagIds().stream().filter(id -> !validTagIds.contains(id)).collect(Collectors.toSet());
                throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
            }
            blog.setTags(buildTagsMap(tags));
        }

        UserInfo author = userRepository.findById(authorInfo.getId()).orElseThrow(() -> new BlogException("用户不存在，ID: " + authorInfo.getId()));
        blog.setAuthor(author);

        String requestedStatus = normalizeCreatableStatus(request.getStatus(), BLOG_STATUS_DRAFT);
        BlogAutoAuditService.AuditDecision auditDecision = applyRequestedStatus(blog, requestedStatus);

        blog.setPrice(request.getPrice() != null ? request.getPrice() : 0);
        blog.setViewCount(0);
        blog.setLikeCount(0);
        blog.setCollectCount(0);
        blog.setDownloadCount(0);
        blog.setCreatedAt(Instant.now());
        blog.setUpdatedAt(Instant.now());

        Blog saved = blogRepository.save(blog);
        recordAutoAuditIfNeeded(saved, auditDecision);
        syncPaidContent(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Blog> getBlogById(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        return blogRepository.findWithAssociationsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Blog> getBlogByIdVisible(Long id, Long viewerId, boolean adminReviewer) {
        BlogViewerContext viewerContext = buildViewerContext(viewerId, adminReviewer);
        return getBlogById(id).filter(blog -> canPreviewBlog(blog, viewerContext));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getAllBlogs() {
        return blogRepository.findPublishedBlogs();
    }

    @Override
    @Transactional
    public Optional<Blog> updateBlog(Long blogId, BlogUpdateRequest request) {
        return updateBlog(blogId, request, null, false);
    }

    @Override
    @Transactional
    public Optional<Blog> updateBlog(Long blogId, BlogUpdateRequest request, Long operatorId, boolean adminReviewer) {
        if (blogId == null) throw new BlogException("博客 ID 不能为空");
        if (request == null) throw new BlogException("更新请求参数不能为空");
        return blogRepository.findById(blogId).map(blog -> {
            ensureManagePermission(blog, operatorId, adminReviewer, "修改博客");
            if (request.getTitle() != null && request.getTitle().trim().isEmpty()) throw new BlogException("博客标题不能为空");
            if (request.getContent() != null && request.getContent().trim().isEmpty()) throw new BlogException("博客内容不能为空");
            if (request.getTitle() != null) blog.setTitle(request.getTitle().trim());
            if (hasSummaryGetter(request)) {
                String summary = readSummary(request);
                if (summary != null) blog.setSummary(summary);
            }
            if (request.getContent() != null) blog.setContent(request.getContent());
            if (request.getCoverImageUrl() != null) blog.setCoverImageUrl(request.getCoverImageUrl());
            if (request.getTagIds() != null) {
                List<Tag> tags = tagRepository.findAllById(request.getTagIds());
                if (tags.size() != request.getTagIds().size()) {
                    Set<Long> validTagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
                    Set<Long> invalidTagIds = request.getTagIds().stream().filter(id -> !validTagIds.contains(id)).collect(Collectors.toSet());
                    throw new BlogException("以下标签 ID 不存在：" + invalidTagIds);
                }
                blog.setTags(buildTagsMap(tags));
            }
            BlogAutoAuditService.AuditDecision auditDecision = null;
            if (request.getStatus() != null) {
                String requestedStatus = normalizeWritableStatus(request.getStatus(), blog.getStatus(), adminReviewer);
                auditDecision = applyRequestedStatus(blog, requestedStatus);
            }
            if (auditDecision == null && request.getIsMarked() != null) blog.setIsMarked(request.getIsMarked());
            if (auditDecision == null && request.getPublishTime() != null) blog.setPublishTime(request.getPublishTime());
            if (request.getPrice() != null) blog.setPrice(request.getPrice());
            blog.setUpdatedAt(Instant.now());
            Blog saved = blogRepository.save(blog);
            recordAutoAuditIfNeeded(saved, auditDecision);
            syncPaidContent(saved);
            return saved;
        });
    }

    @Override
    @Transactional
    public void deleteBlog(Long id) {
        deleteBlog(id, null, false);
    }

    @Override
    @Transactional
    public void deleteBlog(Long id, Long operatorId, boolean adminReviewer) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        Blog blog = blogRepository.findById(id).orElseThrow(() -> new BlogException("博客不存在，ID: " + id));
        ensureManagePermission(blog, operatorId, adminReviewer, "删除博客");
        
        log.info("开始删除博客，ID: {}, 标题: {}", id, blog.getTitle());
        
        // 使用原生SQL删除关联数据，避免循环依赖
        // 1. 删除点赞记录
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM like_record WHERE target_type = 'blog' AND target_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除点赞记录 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除点赞记录失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 2. 删除收藏记录
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM collect_record WHERE target_type = 'blog' AND target_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除收藏记录 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除收藏记录失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 3. 删除评论（先删除子评论，再删除主评论）
        try {
            // 删除子评论
            int childDeleted = entityManager.createNativeQuery(
                "DELETE c FROM comment c " +
                "WHERE c.post_type = 'blog' AND c.post_id = :blogId " +
                "AND c.parent_comment_id IS NOT NULL")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除子评论 {} 条", childDeleted);
            
            // 删除主评论
            int parentDeleted = entityManager.createNativeQuery(
                "DELETE FROM comment WHERE post_type = 'blog' AND post_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除主评论 {} 条", parentDeleted);
        } catch (Exception e) {
            log.warn("删除评论失败，但不影响主流程: {}", e.getMessage(), e);
        }
        
        // 4. 删除举报记录
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM report WHERE target_type = 'blog' AND target_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除举报记录 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除举报记录失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 5. 删除审核日志（数据库有CASCADE，但为了保险也显式删除）
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM blog_audit_log WHERE blog_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除审核日志 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除审核日志失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 6. 删除浏览记录（可选，没有外键约束）
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM view_log WHERE target_type = 'blog' AND target_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除浏览记录 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除浏览记录失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 7. 删除搜索索引（可选，没有外键约束）
        try {
            int deletedCount = entityManager.createNativeQuery(
                "DELETE FROM search_index WHERE doc_type = 'blog' AND doc_id = :blogId")
                .setParameter("blogId", id)
                .executeUpdate();
            log.debug("删除搜索索引 {} 条", deletedCount);
        } catch (Exception e) {
            log.warn("删除搜索索引失败，但不影响主流程: {}", e.getMessage());
        }
        
        // 8. 删除付费内容及支付链路（需要按依赖顺序删除）
        try {
            PaidContent paidContent = paidContentRepository.findByBlogId(id);
            if (paidContent != null) {
                Long paidContentId = paidContent.getId();
                log.debug("找到付费内容，ID: {}", paidContentId);

                int deletedPaymentRecord = executeCriticalDelete(
                        "DELETE pr FROM payment_record pr " +
                                "INNER JOIN payment_order po ON pr.order_id = po.id " +
                                "WHERE po.paid_content_id = :paidContentId",
                        paidContentId,
                        "支付记录"
                );
                int deletedRevenueRecord = executeCriticalDelete(
                        "DELETE rr FROM revenue_record rr " +
                                "INNER JOIN payment_order po ON rr.order_id = po.id " +
                                "WHERE po.paid_content_id = :paidContentId",
                        paidContentId,
                        "收益记录"
                );
                int deletedCouponRedemption = executeCriticalDelete(
                        "DELETE cr FROM coupon_redemption cr " +
                                "INNER JOIN payment_order po ON cr.order_id = po.id " +
                                "WHERE po.paid_content_id = :paidContentId",
                        paidContentId,
                        "优惠券核销记录"
                );
                int deletedOrder = executeCriticalDelete(
                        "DELETE FROM payment_order WHERE paid_content_id = :paidContentId",
                        paidContentId,
                        "支付订单"
                );
                int deletedPurchase = executeCriticalDelete(
                        "DELETE FROM user_purchase WHERE paid_content_id = :paidContentId",
                        paidContentId,
                        "用户购买记录"
                );

                log.info("博客支付链路清理完成，blogId: {}, paidContentId: {}, paymentRecords: {}, revenueRecords: {}, couponRedemptions: {}, orders: {}, purchases: {}",
                        id, paidContentId, deletedPaymentRecord, deletedRevenueRecord, deletedCouponRedemption, deletedOrder, deletedPurchase);

                // 8.6 最后删除付费内容
                log.debug("删除付费内容，ID: {}", paidContentId);
                paidContentRepository.delete(paidContent);
            }
        } catch (Exception e) {
            log.error("删除付费内容失败，将中断删除流程: {}", e.getMessage(), e);
            throw new BlogException("删除付费内容失败，请稍后重试", e);
        }
        
        // 9. 最后删除博客
        log.info("删除博客记录，ID: {}", id);
        blogRepository.delete(blog);
        log.info("博客删除成功，ID: {}", id);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        blogRepository.incrementViewCount(id);
    }

    @Override
    @Transactional
    public void incrementCollectCount(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        blogRepository.incrementCollectCount(id);
    }

    @Override
    @Transactional
    public void incrementDownloadCount(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        blogRepository.incrementDownloadCount(id);
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        blogRepository.incrementLikeCount(id);
    }

    @Override
    public BlogResponse convertToResponse(Blog blog) {
        int reportCount = 0;
        if (blog != null && blog.getId() != null) {
            reportCount = (int) reportRepository.countByTargetTypeAndTargetIdAndStatus(BLOG_TARGET_TYPE, blog.getId(), REPORT_STATUS_PENDING);
        }
        return convertToResponse(blog, reportCount);
    }

    @Override
    public BlogResponse convertToSecureResponse(Blog blog, Long viewerId) {
        return convertToSecureResponse(blog, viewerId, false);
    }

    @Override
    public BlogResponse convertToSecureResponse(Blog blog, Long viewerId, boolean adminReviewer) {
        return convertToSecureResponse(blog, buildViewerContext(viewerId, adminReviewer), false);
    }

    @Override
    public BlogResponse convertToSecurePreviewResponse(Blog blog, Long viewerId, boolean adminReviewer) {
        return convertToSecureResponse(blog, buildViewerContext(viewerId, adminReviewer), true);
    }

    private BlogResponse convertToResponse(Blog blog, int reportCount) {
        if (blog == null) return null;
        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setSummary(blog.getSummary());
        response.setContent(blog.getContent());
        response.setPreviewContent(buildPreviewContent(blog.getContent()));
        response.setCoverImageUrl(blog.getCoverImageUrl());
        if (blog.getTags() != null && !blog.getTags().isEmpty()) {
            List<String> tagNames = new ArrayList<>();
            List<Long> tagIds = new ArrayList<>();
            blog.getTags().forEach((tagId, tagName) -> {
                Long parsedTagId = parseTagId(tagId);
                if (parsedTagId != null) tagIds.add(parsedTagId);
                tagNames.add(tagName);
            });
            response.setTags(tagNames);
            response.setTagIds(tagIds);
        } else {
            response.setTags(Collections.emptyList());
            response.setTagIds(Collections.emptyList());
        }
        String normalizedStatus = normalizeBlogStatus(blog.getStatus(), BLOG_STATUS_DRAFT);
        response.setStatus(normalizedStatus);
        response.setStatusLabel(resolveStatusLabel(normalizedStatus));
        response.setStatusGroup(resolveStatusGroup(normalizedStatus));
        response.setNextStatuses(resolveNextStatuses(normalizedStatus));
        response.setIsMarked(blog.getIsMarked());
        response.setPublishTime(blog.getPublishTime());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        response.setViewCount(blog.getViewCount());
        response.setLikeCount(blog.getLikeCount());
        response.setCollectCount(blog.getCollectCount());
        response.setDownloadCount(blog.getDownloadCount());
        response.setReportCount(reportCount);
        response.setAuditReason(resolveAuditReason(blog));
        response.setPrice(blog.getPrice() != null ? blog.getPrice() : 0);
        response.setRejectReason(resolveRejectReason(blog));
        response.setLocked(false);
        response.setLockType(LOCK_TYPE_NONE);
        response.setLockReason(LOCK_REASON_NONE);
        response.setHasAccess(true);
        response.setCanReadFull(true);
        response.setCanPreview(true);
        response.setCanDownload(true);
        response.setRequiresVip(false);
        response.setRequiresPaid(false);
        response.setHasPurchased(false);
        response.setIsVipUser(false);
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

    private BlogResponse convertToSecureResponse(Blog blog, BlogViewerContext viewerContext, boolean previewOnly) {
        int reportCount = 0;
        if (blog != null && blog.getId() != null) {
            reportCount = (int) reportRepository.countByTargetTypeAndTargetIdAndStatus(BLOG_TARGET_TYPE, blog.getId(), REPORT_STATUS_PENDING);
        }
        return convertToSecureResponse(blog, viewerContext, reportCount, previewOnly);
    }

    private BlogResponse convertToSecureResponse(Blog blog, BlogViewerContext viewerContext, int reportCount, boolean previewOnly) {
        BlogResponse response = convertToResponse(blog, reportCount);
        if (response == null || blog == null) {
            return response;
        }
        applyAccessToResponse(blog, response, resolveBlogAccess(blog, viewerContext), previewOnly);
        return response;
    }

    private List<BlogResponse> convertToSecureResponseList(List<Blog> blogs, BlogViewerContext viewerContext, boolean previewOnly) {
        if (blogs == null || blogs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> reportCountMap = getBlogReportCountMap(blogs.stream().map(Blog::getId).filter(Objects::nonNull).collect(Collectors.toList()));
        return blogs.stream()
                .map(blog -> convertToSecureResponse(blog, viewerContext, reportCountMap.getOrDefault(blog.getId(), 0), previewOnly))
                .collect(Collectors.toList());
    }

    private void applyAccessToResponse(Blog blog, BlogResponse response, BlogAccessResolution access, boolean previewOnly) {
        response.setPreviewContent(buildPreviewContent(blog.getContent()));
        response.setIsVipUser(access.isVipUser());
        response.setHasPurchased(access.hasPurchased());
        response.setHasAccess(access.canReadFull());
        response.setLocked(!access.canReadFull());
        response.setLockType(resolveLockType(access.lockReason()));
        response.setLockReason(access.lockReason());
        response.setCanReadFull(access.canReadFull());
        response.setCanPreview(access.canPreview());
        response.setCanDownload(access.canDownload());
        response.setRequiresVip(access.requiresVip());
        response.setRequiresPaid(access.requiresPaid());
        if (!access.canPreview()) {
            response.setContent(null);
            return;
        }
        if (previewOnly || !access.canReadFull()) {
            response.setContent(response.getPreviewContent());
        }
    }

    private BlogViewerContext buildViewerContext(Long viewerId, boolean adminReviewer) {
        return new BlogViewerContext(viewerId, adminReviewer, isVipUser(viewerId));
    }

    private BlogAccessResolution resolveBlogAccess(Blog blog, BlogViewerContext viewerContext) {
        if (blog == null) {
            return new BlogAccessResolution(false, false, false, false, false, false, viewerContext.vipUser(), false, LOCK_REASON_STATUS_HIDDEN);
        }
        boolean isAuthor = blog.getAuthor() != null
                && viewerContext.viewerId() != null
                && Objects.equals(blog.getAuthor().getId(), viewerContext.viewerId());
        boolean canPreview = canPreviewBlog(blog, viewerContext);
        boolean requiresVip = requiresVip(blog);
        boolean requiresPaid = requiresPaid(blog);
        boolean hasPurchased = false;
        if (canPreview && requiresPaid && viewerContext.viewerId() != null && !viewerContext.adminReviewer() && !isAuthor) {
            PaidContent paidContent = paidContentRepository.findByBlogId(blog.getId());
            if (paidContent != null) {
                hasPurchased = hasPaidAccess(viewerContext.viewerId(), paidContent.getId());
            }
        }

        boolean canReadFull = false;
        String lockReason = LOCK_REASON_NONE;
        if (!canPreview) {
            lockReason = LOCK_REASON_STATUS_HIDDEN;
        } else if (viewerContext.adminReviewer() || isAuthor) {
            canReadFull = true;
        } else if (requiresVip) {
            canReadFull = viewerContext.vipUser();
            if (!canReadFull) {
                lockReason = LOCK_REASON_VIP_REQUIRED;
            }
        } else if (requiresPaid) {
            canReadFull = hasPurchased;
            if (!canReadFull) {
                lockReason = LOCK_REASON_PAYMENT_REQUIRED;
            }
        } else {
            canReadFull = true;
        }

        return new BlogAccessResolution(
                canReadFull,
                canPreview,
                canReadFull,
                requiresVip,
                requiresPaid,
                hasPurchased,
                viewerContext.vipUser(),
                isAuthor,
                lockReason
        );
    }

    private String resolveLockType(String lockReason) {
        if (LOCK_REASON_VIP_REQUIRED.equals(lockReason)) {
            return LOCK_TYPE_VIP;
        }
        if (LOCK_REASON_PAYMENT_REQUIRED.equals(lockReason)) {
            return LOCK_TYPE_PAID;
        }
        if (LOCK_REASON_STATUS_HIDDEN.equals(lockReason)) {
            return LOCK_TYPE_HIDDEN;
        }
        return LOCK_TYPE_NONE;
    }

    private String resolveRejectReason(Blog blog) {
        if (blog == null || blog.getId() == null) return null;
        if (StringUtils.hasText(blog.getTransientRejectReason())) {
            return blog.getTransientRejectReason().trim();
        }
        String rejectReason = readAuditMessage(blogAuditLogRepository.findTopByBlogIdAndAuditStatusOrderByIdDesc(blog.getId(), AUDIT_STATUS_REJECTED));
        if (StringUtils.hasText(rejectReason)) {
            return rejectReason;
        }
        BlogAutoAuditService.AuditDecision decision = resolveCurrentAuditDecision(blog);
        if (decision != null
                && BLOG_STATUS_REJECTED.equalsIgnoreCase(normalizeNullable(blog.getStatus()))
                && BLOG_STATUS_REJECTED.equalsIgnoreCase(decision.blogStatus())
                && StringUtils.hasText(decision.auditReason())) {
            return decision.auditReason().trim();
        }
        List<Report> reports = reportRepository.findByTargetTypeAndTargetId(BLOG_TARGET_TYPE, blog.getId());
        if (reports == null || reports.isEmpty()) return null;
        return reports.stream().filter(item -> item.getReason() != null && !item.getReason().trim().isEmpty())
                .sorted((a, b) -> {
                    Instant ta = a.getProcessedAt() != null ? a.getProcessedAt() : (a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH);
                    Instant tb = b.getProcessedAt() != null ? b.getProcessedAt() : (b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH);
                    return tb.compareTo(ta);
                }).map(Report::getReason).findFirst().orElse(null);
    }

    private String resolveAuditReason(Blog blog) {
        if (blog == null) return null;
        if (StringUtils.hasText(blog.getTransientAuditReason())) {
            return blog.getTransientAuditReason().trim();
        }
        String currentAuditStatus = mapBlogStatusToAuditStatus(blog.getStatus());
        if (currentAuditStatus != null) {
            String auditReason = readAuditMessage(blogAuditLogRepository.findTopByBlogIdAndAuditStatusOrderByIdDesc(blog.getId(), currentAuditStatus));
            if (StringUtils.hasText(auditReason)) {
                return auditReason;
            }
        }
        BlogAutoAuditService.AuditDecision decision = resolveCurrentAuditDecision(blog);
        if (decision == null || !StringUtils.hasText(decision.auditReason())) {
            return null;
        }
        String status = normalizeNullable(blog.getStatus());
        if (BLOG_STATUS_PENDING.equals(status)) {
            return decision.auditReason().trim();
        }
        if (BLOG_STATUS_REJECTED.equals(status) && BLOG_STATUS_REJECTED.equalsIgnoreCase(decision.blogStatus())) {
            return decision.auditReason().trim();
        }
        if (BLOG_STATUS_PUBLISHED.equals(status) && BLOG_STATUS_PUBLISHED.equalsIgnoreCase(decision.blogStatus())) {
            return decision.auditReason().trim();
        }
        return null;
    }

    private BlogAutoAuditService.AuditDecision resolveCurrentAuditDecision(Blog blog) {
        if (blog == null) {
            return null;
        }
        String status = normalizeNullable(blog.getStatus());
        if (!BLOG_STATUS_PENDING.equals(status) && !BLOG_STATUS_PUBLISHED.equals(status) && !BLOG_STATUS_REJECTED.equals(status)) {
            return null;
        }
        if (!StringUtils.hasText(blog.getTitle()) || !StringUtils.hasText(blog.getContent())) {
            return null;
        }
        return blogAutoAuditService.audit(blog.getTitle(), blog.getSummary(), blog.getContent());
    }

    @Override
    public List<BlogResponse> convertToResponseList(List<Blog> blogs) {
        if (blogs == null || blogs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> reportCountMap = getBlogReportCountMap(blogs.stream().map(Blog::getId).filter(Objects::nonNull).collect(Collectors.toList()));
        return blogs.stream().map(blog -> convertToResponse(blog, reportCountMap.getOrDefault(blog.getId(), 0))).collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> convertToSecureResponseList(List<Blog> blogs, Long viewerId, boolean adminReviewer) {
        return convertToSecureResponseList(blogs, buildViewerContext(viewerId, adminReviewer), false);
    }

    @Override
    public List<BlogResponse> convertToSecurePreviewResponseList(List<Blog> blogs, Long viewerId, boolean adminReviewer) {
        return convertToSecureResponseList(blogs, buildViewerContext(viewerId, adminReviewer), true);
    }

    @Override
    public List<Blog> searchBlogs(String keyword) {
        return searchBlogs(keyword, null, false);
    }

    @Override
    public List<Blog> searchBlogs(String keyword, Long viewerId, boolean adminReviewer) {
        return doSearch(keyword, SEARCH_SCOPE_ALL, BLOG_STATUS_PUBLISHED, SEARCH_SORT_RELEVANCE, viewerId, adminReviewer);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogSearchResult searchBlogs(BlogSearchRequest request, Long viewerId, boolean adminReviewer) {
        BlogSearchRequest safeRequest = request == null ? new BlogSearchRequest() : request;
        String scope = normalizeScope(safeRequest.getScope(), SEARCH_SCOPE_ALL);
        String sort = normalizeSort(safeRequest.getSort(), SEARCH_SORT_RELEVANCE);
        String status = normalizeSearchStatus(safeRequest.getStatus(), adminReviewer);
        List<Blog> blogs = doSearch(safeRequest.getKeyword(), scope, status, sort, viewerId, adminReviewer);
        BlogSearchResult result = new BlogSearchResult();
        result.setKeyword(normalizeKeyword(safeRequest.getKeyword(), true));
        result.setScope(scope);
        result.setSort(sort);
        result.setStatus(status == null ? "all" : status);
        result.setTotal(blogs.size());
        result.setItems(convertToSecurePreviewResponseList(blogs, viewerId, adminReviewer));
        return result;
    }

    @Override
    public List<Blog> findByAuthorId(Long authorId) {
        if (authorId == null) throw new BlogException("作者 ID 不能为空");
        return blogRepository.findByAuthorId(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> findByAuthorIdVisible(Long authorId, Long viewerId, boolean adminReviewer) {
        if (authorId == null) throw new BlogException("作者 ID 不能为空");
        if (adminReviewer || (viewerId != null && Objects.equals(authorId, viewerId))) {
            return blogRepository.findByAuthorId(authorId);
        }
        return blogRepository.findByAuthorIdAndStatus(authorId, BLOG_STATUS_PUBLISHED);
    }

    @Override
    public List<Blog> searchBlogsByTag(String keyword) {
        return searchBlogsByTag(keyword, null, false);
    }

    @Override
    public List<Blog> searchBlogsByTag(String keyword, Long viewerId, boolean adminReviewer) {
        return doSearch(keyword, SEARCH_SCOPE_TAG, BLOG_STATUS_PUBLISHED, SEARCH_SORT_RELEVANCE, viewerId, adminReviewer);
    }

    @Override
    public List<Blog> searchBlogsByAuthor(String keyword) {
        return searchBlogsByAuthor(keyword, null, false);
    }

    @Override
    public List<Blog> searchBlogsByAuthor(String keyword, Long viewerId, boolean adminReviewer) {
        return doSearch(keyword, SEARCH_SCOPE_AUTHOR, BLOG_STATUS_PUBLISHED, SEARCH_SORT_RELEVANCE, viewerId, adminReviewer);
    }

    @Override
    public List<Blog> findDraftBlogsByAuthorId(Long authorId) {
        if (authorId == null) throw new BlogException("作者 ID 不能为空");
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
        List<ReportRepository.TargetReportStatsProjection> reportStats = reportRepository.findTargetReportStatsByTargetTypeAndMinCountAndStatus(BLOG_TARGET_TYPE, REPORTED_BLOG_MIN_COUNT, REPORT_STATUS_PENDING);
        if (reportStats.isEmpty()) return List.of();
        List<Long> blogIds = reportStats.stream().map(ReportRepository.TargetReportStatsProjection::getTargetId).collect(Collectors.toList());
        List<Blog> blogs = blogRepository.findByIdIn(blogIds);
        if (blogs.isEmpty()) return List.of();
        Map<Long, Blog> blogMap = blogs.stream().collect(Collectors.toMap(Blog::getId, blog -> blog));
        return blogIds.stream()
                .map(blogMap::get)
                .filter(Objects::nonNull)
                .filter(blog -> BLOG_STATUS_PUBLISHED.equals(normalizeBlogStatus(blog.getStatus(), BLOG_STATUS_DRAFT)))
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
        if (id == null) throw new BlogException("博客 ID 不能为空");
        return blogRepository.findById(id).map(blog -> {
            ensureBlogStatusForReject(blog);
            Instant now = Instant.now();
            String rejectReason = resolveManualRejectReason(reason);
            blog.setStatus(BLOG_STATUS_REJECTED);
            blog.setIsMarked(false);
            blog.setTransientAuditReason(rejectReason);
            blog.setTransientRejectReason(rejectReason);
            blog.setUpdatedAt(now);
            Blog saved = blogRepository.save(blog);
            recordManualAudit(saved, AUDIT_STATUS_REJECTED, rejectReason, normalizeAuditComment(reason), operatorId, now);
            processPendingReportsForBlog(id, REPORT_STATUS_PROCESSED, operatorId, now);
            syncPaidContent(saved);
            return saved;
        });
    }

    @Override
    @Transactional
    public Optional<Blog> republishBlog(Long id) {
        if (id == null) throw new BlogException("博客 ID 不能为空");
        return blogRepository.findById(id).map(blog -> {
            if (!BLOG_STATUS_REJECTED.equalsIgnoreCase(blog.getStatus())) throw new BlogException("只有已下架的博客才能重新发布，当前博客状态：" + blog.getStatus());
            blog.setUpdatedAt(Instant.now());
            BlogAutoAuditService.AuditDecision auditDecision = applyRequestedStatus(blog, BLOG_STATUS_PENDING);
            Blog saved = blogRepository.save(blog);
            recordAutoAuditIfNeeded(saved, auditDecision);
            syncPaidContent(saved);
            return saved;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getRejectedBlogs() {
        return getRejectedBlogs(null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> getRejectedBlogs(Long viewerId, boolean adminReviewer) {
        if (adminReviewer) {
            return blogRepository.findRejectedBlogs();
        }
        if (viewerId == null) {
            return List.of();
        }
        return blogRepository.findByAuthorIdAndStatus(viewerId, BLOG_STATUS_REJECTED);
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
        if (id == null) throw new BlogException("博客 ID 不能为空");
        return blogRepository.findById(id).map(blog -> {
            ensureBlogStatusForApprove(blog);
            Instant now = Instant.now();
            String approveReason = "人工审核通过，博客已发布。";
            blog.setStatus(BLOG_STATUS_PUBLISHED);
            blog.setIsMarked(false);
            blog.setTransientAuditReason(approveReason);
            blog.setTransientRejectReason(null);
            blog.setUpdatedAt(now);
            if (blog.getPublishTime() == null) blog.setPublishTime(now);
            Blog saved = blogRepository.save(blog);
            recordManualAudit(saved, AUDIT_STATUS_APPROVED, approveReason, null, operatorId, now);
            processPendingReportsForBlog(id, REPORT_STATUS_IGNORED, operatorId, now);
            syncPaidContent(saved);
            return saved;
        });
    }

    @Transactional
    public void batchReviewBlogs(List<Long> blogIds, String status, String reason) {
        batchReviewBlogs(blogIds, status, reason, null);
    }

    @Transactional
    public void batchReviewBlogs(List<Long> blogIds, String status, String reason, Long operatorId) {
        if (blogIds == null || blogIds.isEmpty()) throw new BlogException("博客 ID 列表不能为空");
        String normalizedStatus = normalizeNullable(status);
        if (!BLOG_STATUS_PUBLISHED.equalsIgnoreCase(normalizedStatus) && !BLOG_STATUS_REJECTED.equalsIgnoreCase(normalizedStatus)) throw new BlogException("审核状态必须是 published 或 rejected");
        for (Long id : blogIds) {
            if (BLOG_STATUS_PUBLISHED.equalsIgnoreCase(normalizedStatus)) approveBlogInternal(id, operatorId);
            else rejectBlogInternal(id, reason, operatorId);
        }
    }

    @Override
    @Transactional
    public Report reportBlog(Long blogId, Long reporterId, String reason) {
        if (blogId == null) throw new BlogException("博客 ID 不能为空");
        if (reporterId == null) throw new BlogException("举报人 ID 不能为空");
        if (reason == null || reason.trim().isEmpty()) throw new BlogException("举报原因不能为空");
        Blog blog = blogRepository.findById(blogId).orElseThrow(() -> new BlogException("博客不存在，ID: " + blogId));
        String normalizedStatus = normalizeBlogStatus(blog.getStatus(), BLOG_STATUS_DRAFT);
        if (!BLOG_STATUS_PUBLISHED.equals(normalizedStatus)) throw new BlogException("仅已发布博客支持举报，当前状态：" + normalizedStatus);
        if (blog.getAuthor() != null && reporterId.equals(blog.getAuthor().getId())) throw new BlogException("不能举报自己的博客");
        if (reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporterId, BLOG_TARGET_TYPE, blogId, REPORT_STATUS_PENDING)) throw new BlogException("您已经举报过该博客，请勿重复提交");
        UserInfo reporter = userRepository.findById(reporterId).orElseThrow(() -> new BlogException("举报人不存在，ID: " + reporterId));
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(BLOG_TARGET_TYPE);
        report.setTargetId(blogId);
        report.setReason(reason.trim());
        report.setStatus(REPORT_STATUS_PENDING);
        report.setCreatedAt(Instant.now());
        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByBlogId(Long blogId) {
        if (blogId == null) throw new BlogException("博客 ID 不能为空");
        return reportRepository.findByTargetTypeAndTargetId(BLOG_TARGET_TYPE, blogId);
    }

    private List<Blog> doSearch(String keyword,
                                String scope,
                                String status,
                                String sort,
                                Long viewerId,
                                boolean adminReviewer) {
        String normalizedKeyword = normalizeKeyword(keyword, false);
        String normalizedScope = normalizeScope(scope, SEARCH_SCOPE_ALL);
        String normalizedSort = normalizeSort(sort, SEARCH_SORT_RELEVANCE);
        String normalizedStatus = normalizeSearchStatus(status, adminReviewer);

        Set<String> exactMatchedTagIds = resolveExactMatchedTagIds(normalizedKeyword);
        List<Blog> candidates = blogRepository.findForSearch(normalizedStatus);
        BlogViewerContext viewerContext = buildViewerContext(viewerId, adminReviewer);
        Map<Long, SearchMatch> deduplicated = new LinkedHashMap<>();
        for (Blog blog : candidates) {
            if (blog == null || blog.getId() == null || !canPreviewBlog(blog, viewerContext)) {
                continue;
            }
            int score = matchBlogScore(blog, normalizedKeyword, normalizedScope, exactMatchedTagIds);
            if (score <= 0) {
                continue;
            }
            SearchMatch existing = deduplicated.get(blog.getId());
            if (existing == null || score > existing.score()) {
                deduplicated.put(blog.getId(), new SearchMatch(blog, score));
            }
        }
        return deduplicated.values().stream()
                .sorted(resolveSearchComparator(normalizedSort))
                .map(SearchMatch::blog)
                .collect(Collectors.toList());
    }

    private Comparator<SearchMatch> resolveSearchComparator(String sort) {
        Comparator<SearchMatch> timeComparator = Comparator.comparing(
                item -> resolveSortTime(item.blog()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        if (SEARCH_SORT_NEWEST.equals(sort)) {
            return timeComparator.thenComparing(Comparator.comparingInt(SearchMatch::score).reversed());
        }
        if (SEARCH_SORT_HOT.equals(sort)) {
            return Comparator.comparingInt((SearchMatch item) -> hotnessScore(item.blog())).reversed()
                    .thenComparing(timeComparator);
        }
        return Comparator.comparingInt(SearchMatch::score).reversed()
                .thenComparing(timeComparator)
                .thenComparing(item -> item.blog().getId(), Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int hotnessScore(Blog blog) {
        int viewCount = blog != null && blog.getViewCount() != null ? blog.getViewCount() : 0;
        int likeCount = blog != null && blog.getLikeCount() != null ? blog.getLikeCount() : 0;
        int collectCount = blog != null && blog.getCollectCount() != null ? blog.getCollectCount() : 0;
        int downloadCount = blog != null && blog.getDownloadCount() != null ? blog.getDownloadCount() : 0;
        return viewCount + likeCount * 5 + collectCount * 10 + downloadCount * 8;
    }

    private Instant resolveSortTime(Blog blog) {
        if (blog == null) {
            return null;
        }
        return blog.getPublishTime() != null ? blog.getPublishTime() : blog.getCreatedAt();
    }

    private int matchBlogScore(Blog blog, String keyword, String scope, Set<String> exactMatchedTagIds) {
        int score = 0;
        if (blog == null || !StringUtils.hasText(keyword)) {
            return score;
        }
        boolean matchAll = SEARCH_SCOPE_ALL.equals(scope);
        boolean matchContent = SEARCH_SCOPE_CONTENT.equals(scope) || matchAll;
        boolean matchTag = SEARCH_SCOPE_TAG.equals(scope) || matchAll;
        boolean matchAuthor = SEARCH_SCOPE_AUTHOR.equals(scope) || matchAll;

        if (matchContent) {
            if (containsIgnoreCase(blog.getTitle(), keyword)) score += 80;
            if (containsIgnoreCase(blog.getSummary(), keyword)) score += 30;
            if (containsIgnoreCase(blog.getContent(), keyword)) score += 15;
        }
        if (matchTag && matchesTagKeyword(blog, keyword, exactMatchedTagIds)) {
            score += 70;
        }
        if (matchAuthor && blog.getAuthor() != null) {
            if (containsIgnoreCase(blog.getAuthor().getNickname(), keyword)) score += 60;
            if (containsIgnoreCase(blog.getAuthor().getUsername(), keyword)) score += 40;
        }
        return score;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Set<String> resolveExactMatchedTagIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }
        String normalizedKeyword = keyword.trim();
        return tagRepository.findAll().stream()
                .filter(tag -> tag.getName() != null && tag.getName().trim().equalsIgnoreCase(normalizedKeyword))
                .map(tag -> String.valueOf(tag.getId()))
                .collect(Collectors.toSet());
    }

    private String normalizeKeyword(String keyword, boolean allowNull) {
        if (!StringUtils.hasText(keyword)) {
            if (allowNull) {
                return null;
            }
            throw new BlogException("搜索关键词不能为空");
        }
        String normalized = keyword.trim();
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        return normalized;
    }

    private String normalizeScope(String scope, String defaultScope) {
        String normalized = normalizeNullable(scope);
        if (normalized == null) {
            return defaultScope;
        }
        if (SEARCH_SCOPE_ALL.equals(normalized)
                || SEARCH_SCOPE_TAG.equals(normalized)
                || SEARCH_SCOPE_AUTHOR.equals(normalized)
                || SEARCH_SCOPE_CONTENT.equals(normalized)) {
            return normalized;
        }
        return defaultScope;
    }

    private String normalizeSort(String sort, String defaultSort) {
        String normalized = normalizeNullable(sort);
        if (normalized == null) {
            return defaultSort;
        }
        if (SEARCH_SORT_RELEVANCE.equals(normalized)
                || SEARCH_SORT_NEWEST.equals(normalized)
                || SEARCH_SORT_HOT.equals(normalized)) {
            return normalized;
        }
        return defaultSort;
    }

    private String normalizeSearchStatus(String status, boolean adminReviewer) {
        if (!adminReviewer) {
            return BLOG_STATUS_PUBLISHED;
        }
        String normalized = normalizeNullable(status);
        if (normalized == null || "all".equals(normalized)) {
            return null;
        }
        if (!BLOG_SEARCHABLE_STATUSES.contains(normalized)) {
            throw new BlogException("搜索状态必须是 draft/pending/published/rejected/all");
        }
        return normalized;
    }

    private void ensureManagePermission(Blog blog, Long operatorId, boolean adminReviewer, String actionName) {
        if (adminReviewer) {
            return;
        }
        if (operatorId == null) {
            throw new BlogException(actionName + "需要登录");
        }
        Long authorId = blog != null && blog.getAuthor() != null ? blog.getAuthor().getId() : null;
        if (authorId == null || !Objects.equals(authorId, operatorId)) {
            throw new BlogException("仅作者本人或管理员可执行该操作");
        }
    }

    private int executeCriticalDelete(String sql, Long paidContentId, String label) {
        try {
            int deletedCount = entityManager.createNativeQuery(sql)
                    .setParameter("paidContentId", paidContentId)
                    .executeUpdate();
            log.debug("删除{} {} 条，paidContentId: {}", label, deletedCount, paidContentId);
            return deletedCount;
        } catch (Exception e) {
            log.error("删除{}失败，paidContentId: {}", label, paidContentId, e);
            throw new BlogException("删除" + label + "失败，请稍后重试", e);
        }
    }

    private boolean canPreviewBlog(Blog blog, BlogViewerContext viewerContext) {
        if (blog == null) {
            return false;
        }
        String blogStatus = normalizeBlogStatus(blog.getStatus(), BLOG_STATUS_DRAFT);
        if (BLOG_STATUS_PUBLISHED.equals(blogStatus)) {
            return true;
        }
        if (viewerContext.adminReviewer()) {
            return true;
        }
        Long authorId = blog.getAuthor() != null ? blog.getAuthor().getId() : null;
        return authorId != null && viewerContext.viewerId() != null && Objects.equals(authorId, viewerContext.viewerId());
    }

    private record BlogViewerContext(Long viewerId, boolean adminReviewer, boolean vipUser) {}

    private record BlogAccessResolution(boolean canReadFull,
                                        boolean canPreview,
                                        boolean canDownload,
                                        boolean requiresVip,
                                        boolean requiresPaid,
                                        boolean hasPurchased,
                                        boolean isVipUser,
                                        boolean isAuthor,
                                        String lockReason) {}

    private void ensureBlogStatusForApprove(Blog blog) {
        String blogStatus = normalizeBlogStatus(blog == null ? null : blog.getStatus(), BLOG_STATUS_DRAFT);
        if (!BLOG_STATUS_PENDING.equals(blogStatus)) {
            throw new BlogException("仅待审核博客可执行通过操作，当前状态：" + blogStatus);
        }
    }

    private void ensureBlogStatusForReject(Blog blog) {
        String blogStatus = normalizeBlogStatus(blog == null ? null : blog.getStatus(), BLOG_STATUS_DRAFT);
        if (!BLOG_STATUS_PENDING.equals(blogStatus) && !BLOG_STATUS_PUBLISHED.equals(blogStatus)) {
            throw new BlogException("仅待审核或已发布博客可执行驳回操作，当前状态：" + blogStatus);
        }
    }

    private String resolveStatusLabel(String status) {
        if (BLOG_STATUS_DRAFT.equals(status)) return "草稿";
        if (BLOG_STATUS_PENDING.equals(status)) return "待审核";
        if (BLOG_STATUS_PUBLISHED.equals(status)) return "已发布";
        if (BLOG_STATUS_REJECTED.equals(status)) return "已驳回";
        return "未知状态";
    }

    private String resolveStatusGroup(String status) {
        if (BLOG_STATUS_DRAFT.equals(status)) return "editing";
        if (BLOG_STATUS_PENDING.equals(status)) return "review";
        if (BLOG_STATUS_PUBLISHED.equals(status)) return "online";
        if (BLOG_STATUS_REJECTED.equals(status)) return "offline";
        return "unknown";
    }

    private List<String> resolveNextStatuses(String status) {
        if (BLOG_STATUS_DRAFT.equals(status)) return List.of(BLOG_STATUS_PENDING);
        if (BLOG_STATUS_PENDING.equals(status)) return List.of(BLOG_STATUS_DRAFT, BLOG_STATUS_PUBLISHED, BLOG_STATUS_REJECTED);
        if (BLOG_STATUS_PUBLISHED.equals(status)) return List.of(BLOG_STATUS_REJECTED, BLOG_STATUS_DRAFT);
        if (BLOG_STATUS_REJECTED.equals(status)) return List.of(BLOG_STATUS_DRAFT, BLOG_STATUS_PENDING);
        return List.of();
    }

    private String normalizeCreatableStatus(String status, String defaultValue) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return defaultValue;
        }
        if (BLOG_STATUS_DRAFT.equals(normalized)
                || BLOG_STATUS_PENDING.equals(normalized)
                || BLOG_STATUS_PUBLISHED.equals(normalized)) {
            return normalized;
        }
        throw new BlogException("创建博客时状态必须是 draft/pending/published");
    }

    private String normalizeWritableStatus(String status, String defaultValue, boolean adminReviewer) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return normalizeBlogStatus(defaultValue, BLOG_STATUS_DRAFT);
        }
        if (adminReviewer && BLOG_EDITABLE_STATUSES.contains(normalized)) {
            return normalized;
        }
        if (BLOG_WRITABLE_STATUSES.contains(normalized)) {
            return normalized;
        }
        throw new BlogException("更新博客时状态仅支持 draft/pending");
    }

    private record SearchMatch(Blog blog, int score) {}

    private void syncPaidContent(Blog blog) {
        if (blog == null || blog.getId() == null) return;
        int price = blog.getPrice() == null ? 0 : blog.getPrice();
        PaidContent paidContent = paidContentRepository.findByBlogId(blog.getId());
        if (price <= 0) {
            if (paidContent != null) paidContentRepository.delete(paidContent);
            return;
        }
        if (paidContent == null) {
            paidContent = new PaidContent();
            paidContent.setContentType("blog");
            paidContent.setContentId(blog.getId());
            paidContent.setBlogId(blog.getId());
        }
        paidContent.setTitle(blog.getTitle() != null ? blog.getTitle() : "博客内容");
        paidContent.setDescription(blog.getSummary());
        paidContent.setCreatedBy(blog.getAuthor() != null ? blog.getAuthor().getId() : null);
        paidContent.setAccessType("one_time");
        paidContent.setPrice(BigDecimal.valueOf(price));
        paidContent.setRequiredMembershipLevelId(null);
        paidContent.setStatus(mapPaidContentStatus(blog.getStatus()));
        paidContentRepository.save(paidContent);
    }

    private String mapPaidContentStatus(String blogStatus) {
        String normalized = normalizeNullable(blogStatus);
        if (BLOG_STATUS_PUBLISHED.equals(normalized)) return "published";
        if (BLOG_STATUS_DRAFT.equals(normalized) || "pending".equals(normalized)) return "draft";
        return "disabled";
    }

    private boolean requiresVip(Blog blog) {
        return blog != null && blog.getPrice() != null && blog.getPrice() == -1;
    }

    private boolean requiresPaid(Blog blog) {
        return blog != null && blog.getPrice() != null && blog.getPrice() > 0;
    }

    private boolean isVipUser(Long viewerId) {
        if (viewerId == null) return false;
        LocalDateTime now = LocalDateTime.now();
        Optional<Membership> activeOpt = membershipRepository.findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(viewerId, "active", now);
        if (activeOpt.isPresent()) {
            Membership membership = activeOpt.get();
            if (membership.getEndTime() != null && membership.getEndTime().isAfter(now)) {
                return true;
            }
        }
        return userRepository.findById(viewerId)
                .map(user -> Boolean.TRUE.equals(user.getIsPremiumMember()) && user.getPremiumExpiryDate() != null && user.getPremiumExpiryDate().isAfter(Instant.now()))
                .orElse(false);
    }

    private boolean hasPaidAccess(Long viewerId, Long paidContentId) {
        if (viewerId == null || paidContentId == null) return false;
        Optional<UserPurchase> purchaseOpt = userPurchaseRepository.findByUserIdAndPaidContentId(viewerId, paidContentId);
        if (purchaseOpt.isEmpty()) return false;
        UserPurchase purchase = purchaseOpt.get();
        return purchase.getAccessExpiredAt() == null || purchase.getAccessExpiredAt().isAfter(LocalDateTime.now());
    }

    private void processPendingReportsForBlog(Long blogId, String reportStatus, Long operatorId, Instant processedAt) {
        List<Report> reports = reportRepository.findByTargetTypeAndTargetIdAndStatus(BLOG_TARGET_TYPE, blogId, REPORT_STATUS_PENDING);
        if (reports == null || reports.isEmpty()) return;
        UserInfo processor = null;
        if (operatorId != null) processor = userRepository.findById(operatorId).orElse(null);
        for (Report report : reports) {
            report.setStatus(reportStatus);
            report.setProcessedAt(processedAt);
            report.setProcessor(processor);
        }
        reportRepository.saveAll(reports);
    }

    private void recordAutoAuditIfNeeded(Blog blog, BlogAutoAuditService.AuditDecision decision) {
        if (blog == null || blog.getId() == null || decision == null) return;
        Instant now = Instant.now();
        BlogAuditLog log = new BlogAuditLog();
        log.setBlogId(blog.getId());
        log.setAuditType(AUDIT_TYPE_AUTO);
        log.setAuditStatus(decision.auditStatus());
        log.setAuditScore(decision.auditScore());
        log.setAuditReason(decision.auditReason());
        log.setAutoReviewSuggestion(decision.autoReviewSuggestion());
        log.setRequiresManualReview(decision.requiresManualReview());
        log.setAuditor(null);
        log.setAuditComment(null);
        log.setCreatedAt(toLocalDateTime(now));
        log.setReviewedAt(toLocalDateTime(now));
        blogAuditLogRepository.save(log);
    }

    private void recordManualAudit(Blog blog,
                                   String auditStatus,
                                   String auditReason,
                                   String auditComment,
                                   Long operatorId,
                                   Instant reviewedAt) {
        if (blog == null || blog.getId() == null || !StringUtils.hasText(auditStatus)) return;
        BlogAuditLog log = new BlogAuditLog();
        log.setBlogId(blog.getId());
        log.setAuditType(AUDIT_TYPE_MANUAL);
        log.setAuditStatus(auditStatus);
        log.setAuditScore(null);
        log.setAuditReason(auditReason);
        log.setAutoReviewSuggestion(null);
        log.setRequiresManualReview(false);
        log.setAuditor(operatorId == null ? null : userRepository.findById(operatorId).orElse(null));
        log.setAuditComment(auditComment);
        log.setCreatedAt(toLocalDateTime(reviewedAt));
        log.setReviewedAt(toLocalDateTime(reviewedAt));
        blogAuditLogRepository.save(log);
    }

    private String buildPreviewContent(String content) {
        String text = stripHtml(content);
        if (text.length() > PREVIEW_LENGTH) text = text.substring(0, PREVIEW_LENGTH) + "...";
        return "<p>" + escapeHtml(text) + "</p>";
    }

    private String stripHtml(String content) {
        if (content == null || content.trim().isEmpty()) return "";
        String text = content.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return text.replaceAll("\\s+", " ").trim();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private BlogAutoAuditService.AuditDecision applyRequestedStatus(Blog blog, String requestedStatus) {
        if (!BLOG_STATUS_PENDING.equalsIgnoreCase(requestedStatus) && !BLOG_STATUS_PUBLISHED.equalsIgnoreCase(requestedStatus)) {
            blog.setStatus(requestedStatus);
            blog.setIsMarked(false);
            blog.setTransientAuditReason(null);
            blog.setTransientRejectReason(null);
            return null;
        }

        BlogAutoAuditService.AuditDecision decision = blogAutoAuditService.audit(
                blog.getTitle(),
                blog.getSummary(),
                blog.getContent()
        );
        blog.setStatus(decision.blogStatus());
        blog.setIsMarked(decision.requiresManualReview());
        blog.setTransientAuditReason(decision.auditReason());
        blog.setTransientRejectReason(BLOG_STATUS_REJECTED.equalsIgnoreCase(decision.blogStatus()) ? decision.auditReason() : null);
        if (BLOG_STATUS_PUBLISHED.equalsIgnoreCase(decision.blogStatus())) {
            if (blog.getPublishTime() == null) {
                blog.setPublishTime(Instant.now());
            }
        } else if (BLOG_STATUS_PENDING.equalsIgnoreCase(decision.blogStatus()) || BLOG_STATUS_REJECTED.equalsIgnoreCase(decision.blogStatus())) {
            blog.setPublishTime(null);
        }
        return decision;
    }

    private String resolveManualRejectReason(String reason) {
        if (StringUtils.hasText(reason)) {
            return reason.trim();
        }
        return "人工审核未通过，请根据规范修改内容后重新提交。";
    }

    private String normalizeAuditComment(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        return reason.trim();
    }

    private Map<String, String> buildTagsMap(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(tag -> tag.getId().toString(), Tag::getName, (left, right) -> left, LinkedHashMap::new));
    }

    private boolean matchesTagKeyword(Blog blog, String keyword, Set<String> exactMatchedTagIds) {
        if (blog.getTags() == null || blog.getTags().isEmpty()) return false;
        if (!exactMatchedTagIds.isEmpty() && blog.getTags().keySet().stream().anyMatch(exactMatchedTagIds::contains)) return true;
        String normalizedKeyword = keyword.toLowerCase();
        return blog.getTags().values().stream().filter(Objects::nonNull).map(String::trim).anyMatch(tagName -> {
            if (!exactMatchedTagIds.isEmpty()) return tagName.equalsIgnoreCase(keyword);
            return tagName.toLowerCase().contains(normalizedKeyword);
        });
    }

    private Long parseTagId(String rawTagId) {
        if (rawTagId == null || rawTagId.isBlank()) return null;
        try {
            return Long.parseLong(rawTagId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<Long, Integer> getBlogReportCountMap(List<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) return Collections.emptyMap();
        return reportRepository.findTargetReportStatsByTargetTypeAndTargetIdsAndStatus(BLOG_TARGET_TYPE, blogIds, REPORT_STATUS_PENDING).stream().collect(Collectors.toMap(ReportRepository.TargetReportStatsProjection::getTargetId, stats -> stats.getReportCount().intValue()));
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

    private String normalizeBlogStatus(String status, String defaultValue) {
        String normalized = normalizeNullable(status);
        if (normalized == null) {
            return defaultValue;
        }
        if (BLOG_EDITABLE_STATUSES.contains(normalized)) {
            return normalized;
        }
        return defaultValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }

    private String mapBlogStatusToAuditStatus(String blogStatus) {
        String normalized = normalizeNullable(blogStatus);
        if (BLOG_STATUS_PENDING.equals(normalized)) return AUDIT_STATUS_PENDING;
        if (BLOG_STATUS_PUBLISHED.equals(normalized)) return AUDIT_STATUS_APPROVED;
        if (BLOG_STATUS_REJECTED.equals(normalized)) return AUDIT_STATUS_REJECTED;
        return null;
    }

    private String readAuditMessage(BlogAuditLog auditLog) {
        if (auditLog == null) return null;
        if (StringUtils.hasText(auditLog.getAuditReason())) return auditLog.getAuditReason().trim();
        if (StringUtils.hasText(auditLog.getAuditComment())) return auditLog.getAuditComment().trim();
        return null;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }
}
