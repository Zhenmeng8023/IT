package com.alikeyou.itmoduleblog.controller;

import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmoduleblog.service.ReportService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulelogin.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogControllerPermissionTest {

    @Mock
    private BlogService blogService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportService reportService;

    private BlogController controller;

    @BeforeEach
    void setUp() {
        controller = new BlogController();
        ReflectionTestUtils.setField(controller, "blogService", blogService);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "reportService", reportService);
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
        when(blogService.convertToResponseList(blogs)).thenReturn(responses);

        ResponseEntity<List<BlogResponse>> result = controller.getReportedBlogs();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(responses, result.getBody());
        verify(blogService).getReportedBlogs();
    }
}
