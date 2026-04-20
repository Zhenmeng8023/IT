package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmoduleblog.service.ViewLogService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BlogControllerPermissionTest {

    @Mock
    private BlogService blogService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportService reportService;
    @Mock
    private ViewLogService viewLogService;

    private BlogController controller;

    @BeforeEach
    void setUp() {
        controller = new BlogController();
        ReflectionTestUtils.setField(controller, "blogService", blogService);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "reportService", reportService);
        ReflectionTestUtils.setField(controller, "viewLogService", viewLogService);
    }

    @AfterEach
    void tearDown() {
        LoginConstant.clearUserInfo();
    }

    @Test
    void getReportedBlogsShouldRequireLogin() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.getReportedBlogs());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void getReportedBlogsShouldForbidNormalUser() {
        LoginConstant.setUserId(100L);
        LoginConstant.setRoleId(4);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.getReportedBlogs());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getReportedBlogsShouldAllowAdminReviewer() {
        LoginConstant.setUserId(1L);
        LoginConstant.setRoleId(1);
        List<com.alikeyou.itmoduleblog.entity.Blog> blogs = List.of();
        List<BlogResponse> responses = List.of();
        when(blogService.getReportedBlogs()).thenReturn(blogs);
        when(blogService.convertToSecureResponseList(blogs, 1L, true)).thenReturn(responses);

        ResponseEntity<List<BlogResponse>> result = controller.getReportedBlogs();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(responses, result.getBody());
        verify(blogService).getReportedBlogs();
    }

    @Test
    void getBlogByIdShouldOnlyIncrementViewCountWhenViewLogAccepted() {
        Blog blog = new Blog();
        blog.setId(55L);
        blog.setStatus("published");
        BlogResponse response = new BlogResponse();
        response.setId(55L);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        doReturn(null).when(request).getHeader("X-Forwarded-For");
        doReturn(null).when(request).getHeader("X-Real-IP");
        doReturn("JUnit").when(request).getHeader("User-Agent");
        when(blogService.getBlogByIdVisible(55L, null, false)).thenReturn(Optional.of(blog));
        when(viewLogService.recordBlogView(eq(55L), eq(null), anyString(), eq("JUnit"))).thenReturn(false);
        when(blogService.convertToSecureResponse(blog, null, false)).thenReturn(response);

        ResponseEntity<BlogResponse> result = controller.getBlogById(55L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(blogService, never()).incrementViewCount(55L);
    }

    @Test
    void getBlogByIdShouldIncrementViewCountWhenViewLogAccepted() {
        Blog blog = new Blog();
        blog.setId(56L);
        blog.setStatus("published");
        BlogResponse response = new BlogResponse();
        response.setId(56L);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        doReturn(null).when(request).getHeader("X-Forwarded-For");
        doReturn(null).when(request).getHeader("X-Real-IP");
        doReturn("JUnit").when(request).getHeader("User-Agent");
        when(blogService.getBlogByIdVisible(56L, null, false)).thenReturn(Optional.of(blog));
        when(viewLogService.recordBlogView(eq(56L), eq(null), anyString(), eq("JUnit"))).thenReturn(true);
        when(blogService.convertToSecureResponse(blog, null, false)).thenReturn(response);

        ResponseEntity<BlogResponse> result = controller.getBlogById(56L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(blogService).incrementViewCount(56L);
    }

    @Test
    void downloadBlogShouldForbidWhenSecureResponseCannotDownload() {
        LoginConstant.setUserId(8L);
        LoginConstant.setRoleId(4);

        Blog blog = new Blog();
        blog.setId(77L);
        blog.setStatus("published");
        BlogResponse response = new BlogResponse();
        response.setCanDownload(Boolean.FALSE);

        when(blogService.getBlogByIdVisible(77L, 8L, false)).thenReturn(Optional.of(blog));
        when(blogService.convertToSecureResponse(blog, 8L, false)).thenReturn(response);

        ResponseEntity<Void> result = controller.downloadBlog(77L);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verify(blogService, never()).incrementDownloadCount(77L);
    }
}
