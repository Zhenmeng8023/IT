package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.BlogSearchRequest;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
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
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.UserPurchaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @Test
    void convertToSecureResponseShouldExposeUnifiedPaidAccessState() {
        Blog paidBlog = buildBlog(41L, "published", 3L, "付费文章", "<p>完整内容</p>");
        paidBlog.setPrice(99);
        PaidContent paidContent = new PaidContent();
        paidContent.setId(501L);
        when(paidContentRepository.findByBlogId(41L)).thenReturn(paidContent);
        when(userPurchaseRepository.findByUserIdAndPaidContentId(8L, 501L)).thenReturn(Optional.empty());

        BlogResponse response = service.convertToSecureResponse(paidBlog, 8L, false);

        assertEquals(Boolean.TRUE, response.getCanPreview());
        assertEquals(Boolean.FALSE, response.getCanReadFull());
        assertEquals(Boolean.FALSE, response.getCanDownload());
        assertEquals(Boolean.TRUE, response.getRequiresPaid());
        assertEquals(Boolean.FALSE, response.getRequiresVip());
        assertEquals(Boolean.FALSE, response.getHasPurchased());
        assertEquals("payment_required", response.getLockReason());
        assertTrue(Objects.requireNonNull(response.getContent()).contains("完整内容"));
    }

    @Test
    void convertToSecureResponseShouldAllowNormalPublishedBlog() {
        Blog normalBlog = buildBlog(42L, "published", 3L, "普通文章", "<p>完整内容</p>");
        normalBlog.setPrice(0);

        BlogResponse response = service.convertToSecureResponse(normalBlog, 8L, false);

        assertEquals(Boolean.TRUE, response.getCanPreview());
        assertEquals(Boolean.TRUE, response.getCanReadFull());
        assertEquals(Boolean.TRUE, response.getCanDownload());
        assertEquals("none", response.getLockReason());
    }

    @Test
    void convertToSecureResponseShouldGateVipBlogForNonVipUser() {
        Blog vipBlog = buildBlog(43L, "published", 3L, "VIP 文章", "<p>完整内容</p>");
        vipBlog.setPrice(-1);

        BlogResponse response = service.convertToSecureResponse(vipBlog, 8L, false);

        assertEquals(Boolean.TRUE, response.getCanPreview());
        assertEquals(Boolean.FALSE, response.getCanReadFull());
        assertEquals(Boolean.FALSE, response.getCanDownload());
        assertEquals(Boolean.TRUE, response.getRequiresVip());
        assertEquals("vip_required", response.getLockReason());
    }

    @Test
    void convertToSecureResponseShouldAllowAuthorToReadOwnPaidBlog() {
        Blog paidBlog = buildBlog(44L, "published", 8L, "我的付费文章", "<p>完整内容</p>");
        paidBlog.setPrice(199);

        BlogResponse response = service.convertToSecureResponse(paidBlog, 8L, false);

        assertEquals(Boolean.TRUE, response.getCanPreview());
        assertEquals(Boolean.TRUE, response.getCanReadFull());
        assertEquals(Boolean.TRUE, response.getCanDownload());
        assertEquals(Boolean.FALSE, response.getHasPurchased());
    }

    @Test
    void convertToSecureResponseShouldAllowPurchasedUserToReadPaidBlog() {
        Blog paidBlog = buildBlog(45L, "published", 3L, "已购文章", "<p>完整内容</p>");
        paidBlog.setPrice(88);
        PaidContent paidContent = new PaidContent();
        paidContent.setId(601L);
        UserPurchase purchase = new UserPurchase();
        purchase.setAccessExpiredAt(LocalDateTime.now().plusDays(3));

        when(paidContentRepository.findByBlogId(45L)).thenReturn(paidContent);
        when(userPurchaseRepository.findByUserIdAndPaidContentId(9L, 601L)).thenReturn(Optional.of(purchase));

        BlogResponse response = service.convertToSecureResponse(paidBlog, 9L, false);

        assertEquals(Boolean.TRUE, response.getCanReadFull());
        assertEquals(Boolean.TRUE, response.getCanDownload());
        assertEquals(Boolean.TRUE, response.getHasPurchased());
        assertEquals("none", response.getLockReason());
    }

    @Test
    void deleteBlogShouldDeleteOrderDependentsBeforeOrders() {
        Blog blog = buildBlog(51L, "published", 2L, "付费删除", "内容");
        PaidContent paidContent = new PaidContent();
        paidContent.setId(900L);
        List<String> executedSql = new ArrayList<>();
        ReflectionTestUtils.setField(service, "entityManager", buildEntityManagerProxy(executedSql));
        when(blogRepository.findById(51L)).thenReturn(Optional.of(blog));
        when(paidContentRepository.findByBlogId(51L)).thenReturn(paidContent);

        service.deleteBlog(51L, 2L, false);

        assertEquals(13, executedSql.size());
        assertTrue(executedSql.get(8).contains("DELETE pr FROM payment_record"));
        assertTrue(executedSql.get(9).contains("DELETE rr FROM revenue_record"));
        assertTrue(executedSql.get(10).contains("DELETE cr FROM coupon_redemption"));
        assertTrue(executedSql.get(11).contains("DELETE FROM payment_order"));
        assertTrue(executedSql.get(12).contains("DELETE FROM user_purchase"));
        verify(paidContentRepository).delete(paidContent);
        verify(blogRepository).delete(blog);
    }

    @Test
    void deleteBlogShouldDeleteNormalBlogWithoutPaidCleanup() {
        Blog blog = buildBlog(52L, "published", 2L, "普通删除", "内容");
        List<String> executedSql = new ArrayList<>();
        ReflectionTestUtils.setField(service, "entityManager", buildEntityManagerProxy(executedSql));
        when(blogRepository.findById(52L)).thenReturn(Optional.of(blog));
        when(paidContentRepository.findByBlogId(52L)).thenReturn(null);

        service.deleteBlog(52L, 2L, false);

        assertEquals(8, executedSql.size());
        verify(blogRepository).delete(blog);
    }

    private EntityManager buildEntityManagerProxy(List<String> executedSql) {
        Query queryProxy = (Query) Proxy.newProxyInstance(
                Query.class.getClassLoader(),
                new Class[]{Query.class},
                (proxy, method, args) -> {
                    if ("setParameter".equals(method.getName())) {
                        return proxy;
                    }
                    if ("executeUpdate".equals(method.getName())) {
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        return (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class[]{EntityManager.class},
                (proxy, method, args) -> {
                    if ("createNativeQuery".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof String sql) {
                        executedSql.add(sql);
                        return queryProxy;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return null;
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
