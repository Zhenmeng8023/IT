package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.BlogSearchRequest;
import com.alikeyou.itmoduleblog.dto.BlogSearchResult;
import com.alikeyou.itmoduleblog.dto.BlogUpdateRequest;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.exception.BlogException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplGuardrailTest {

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
        ReflectionTestUtils.setField(service, "blogAutoAuditService", new BlogAutoAuditService());
    }

    @Test
    void updateBlogShouldRejectNonAuthorWhenNotAdmin() {
        Blog blog = buildBlog(10L, "draft", 2L, "旧标题", "旧内容");
        BlogUpdateRequest request = new BlogUpdateRequest();
        request.setTitle("新标题");
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog));

        BlogException exception = assertThrows(BlogException.class, () -> service.updateBlog(10L, request, 1L, false));

        assertTrue(exception.getMessage().contains("仅作者本人或管理员"));
    }

    @Test
    void approveBlogShouldOnlyAllowPendingStatus() {
        Blog blog = buildBlog(11L, "draft", 2L, "草稿标题", "草稿内容");
        when(blogRepository.findById(11L)).thenReturn(Optional.of(blog));

        BlogException exception = assertThrows(BlogException.class, () -> service.approveBlog(11L, 9L));

        assertTrue(exception.getMessage().contains("仅待审核博客可执行通过操作"));
    }

    @Test
    void reportBlogShouldOnlyAllowPublishedStatus() {
        Blog blog = buildBlog(12L, "pending", 2L, "待审核标题", "待审核内容");
        when(blogRepository.findById(12L)).thenReturn(Optional.of(blog));

        BlogException exception = assertThrows(BlogException.class, () -> service.reportBlog(12L, 8L, "违规内容"));

        assertTrue(exception.getMessage().contains("仅已发布博客支持举报"));
    }

    @Test
    void searchBlogsShouldForcePublishedStatusForNormalUser() {
        Blog published = buildBlog(21L, "published", 3L, "Java 搜索实战", "内容");
        when(blogRepository.findForSearch("published")).thenReturn(List.of(published));
        when(tagRepository.findAll()).thenReturn(List.of());

        List<Blog> blogs = service.searchBlogs("Java", 8L, false);

        assertEquals(1, blogs.size());
        assertEquals(21L, blogs.get(0).getId());
        verify(blogRepository).findForSearch("published");
    }

    @Test
    void searchBlogsQueryShouldRespectAdminStatusFilter() {
        Blog rejected = buildBlog(31L, "rejected", 3L, "Java 审核未通过", "内容");
        BlogSearchRequest request = new BlogSearchRequest();
        request.setKeyword("Java");
        request.setScope("all");
        request.setStatus("rejected");
        request.setSort("newest");
        when(blogRepository.findForSearch("rejected")).thenReturn(List.of(rejected));
        when(tagRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findTargetReportStatsByTargetTypeAndTargetIdsAndStatus(anyString(), anyList(), anyString()))
                .thenReturn(List.of());

        BlogSearchResult result = service.searchBlogs(request, 9L, true);

        assertEquals("rejected", result.getStatus());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getItems().size());
        verify(blogRepository).findForSearch("rejected");
    }

    private Blog buildBlog(Long id, String status, Long authorId, String title, String content) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setStatus(status);
        blog.setTitle(title);
        blog.setContent(content);
        UserInfo author = new UserInfo();
        author.setId(authorId);
        author.setUsername("author" + authorId);
        blog.setAuthor(author);
        return blog;
    }
}
