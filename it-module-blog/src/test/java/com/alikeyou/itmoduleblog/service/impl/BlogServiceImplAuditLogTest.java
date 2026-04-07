package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.AuthorInfo;
import com.alikeyou.itmoduleblog.dto.BlogCreateRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.BlogAuditLog;
import com.alikeyou.itmoduleblog.repository.BlogAuditLogRepository;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogAutoAuditService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.ReportRepository;
import com.alikeyou.itmodulecommon.repository.TagRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.UserPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplAuditLogTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private BlogAuditLogRepository blogAuditLogRepository;
    @Mock
    private PaidContentRepository paidContentRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private MembershipLevelRepository membershipLevelRepository;
    @Mock
    private UserPurchaseRepository userPurchaseRepository;

    private BlogServiceImpl service;
    private final BlogAutoAuditService blogAutoAuditService = new BlogAutoAuditService();

    @BeforeEach
    void setUp() {
        service = new BlogServiceImpl();
        ReflectionTestUtils.setField(service, "blogRepository", blogRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "tagRepository", tagRepository);
        ReflectionTestUtils.setField(service, "reportRepository", reportRepository);
        ReflectionTestUtils.setField(service, "blogAuditLogRepository", blogAuditLogRepository);
        ReflectionTestUtils.setField(service, "paidContentRepository", paidContentRepository);
        ReflectionTestUtils.setField(service, "membershipRepository", membershipRepository);
        ReflectionTestUtils.setField(service, "membershipLevelRepository", membershipLevelRepository);
        ReflectionTestUtils.setField(service, "userPurchaseRepository", userPurchaseRepository);
        ReflectionTestUtils.setField(service, "blogAutoAuditService", blogAutoAuditService);
    }

    @Test
    void createBlogShouldPersistAutoAuditLogWhenSubmittingForPublish() {
        BlogCreateRequest request = new BlogCreateRequest();
        request.setTitle("Spring Boot 审核");
        request.setSummary("自动审核测试");
        request.setContent("<p>这是一篇正常的技术博客内容。</p>");
        request.setStatus("published");
        request.setPrice(0);

        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setId(1L);

        UserInfo author = new UserInfo();
        author.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> {
            Blog blog = invocation.getArgument(0);
            blog.setId(100L);
            return blog;
        });

        Blog created = service.createBlog(request, authorInfo);

        ArgumentCaptor<BlogAuditLog> captor = ArgumentCaptor.forClass(BlogAuditLog.class);
        verify(blogAuditLogRepository).save(captor.capture());

        BlogAuditLog savedLog = captor.getValue();
        assertEquals(100L, created.getId());
        assertEquals("published", created.getStatus());
        assertEquals("AUTO", savedLog.getAuditType());
        assertEquals("APPROVED", savedLog.getAuditStatus());
        assertEquals(new BigDecimal("8.00"), savedLog.getAuditScore());
        assertEquals("自动审核通过，未发现明显风险内容。", savedLog.getAuditReason());
        assertEquals("可直接发布。", savedLog.getAutoReviewSuggestion());
        assertEquals(Boolean.FALSE, savedLog.getRequiresManualReview());
        assertNull(savedLog.getAuditor());
        assertNotNull(savedLog.getCreatedAt());
        assertNotNull(savedLog.getReviewedAt());
    }

    @Test
    void rejectBlogShouldPersistManualAuditLog() {
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setStatus("pending");

        UserInfo auditor = new UserInfo();
        auditor.setId(9L);
        auditor.setUsername("admin");

        when(blogRepository.findById(200L)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(9L)).thenReturn(Optional.of(auditor));

        Optional<Blog> result = service.rejectBlog(200L, "命中违规导流内容", 9L);

        ArgumentCaptor<BlogAuditLog> captor = ArgumentCaptor.forClass(BlogAuditLog.class);
        verify(blogAuditLogRepository).save(captor.capture());

        BlogAuditLog savedLog = captor.getValue();
        assertTrue(result.isPresent());
        assertEquals("rejected", result.get().getStatus());
        assertEquals("MANUAL", savedLog.getAuditType());
        assertEquals("REJECTED", savedLog.getAuditStatus());
        assertEquals("命中违规导流内容", savedLog.getAuditReason());
        assertEquals("命中违规导流内容", savedLog.getAuditComment());
        assertEquals(Boolean.FALSE, savedLog.getRequiresManualReview());
        assertEquals(auditor, savedLog.getAuditor());
        assertNull(savedLog.getAuditScore());
        assertNotNull(savedLog.getCreatedAt());
        assertNotNull(savedLog.getReviewedAt());
    }

    @Test
    void convertToResponseShouldPreferPersistedAuditReason() {
        Blog blog = new Blog();
        blog.setId(300L);
        blog.setTitle("旧内容");
        blog.setContent("<p>当前内容已经被修改成正常文本。</p>");
        blog.setStatus("rejected");

        BlogAuditLog auditLog = new BlogAuditLog();
        auditLog.setAuditReason("人工审核未通过，请删除违规导流信息后再提交。");

        when(blogAuditLogRepository.findTopByBlogIdAndAuditStatusOrderByIdDesc(300L, "REJECTED")).thenReturn(auditLog);

        BlogResponse response = service.convertToResponse(blog);

        assertEquals("人工审核未通过，请删除违规导流信息后再提交。", response.getAuditReason());
        assertEquals("人工审核未通过，请删除违规导流信息后再提交。", response.getRejectReason());
    }
}
