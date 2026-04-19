package com.alikeyou.itmoduleblog.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlogExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoExceptionController())
                .setControllerAdvice(new BlogExceptionHandler())
                .build();
    }

    @Test
    void shouldNormalizeLoginRequiredMessage() throws Exception {
        mockMvc.perform(get("/test/blog/errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录后再操作"));
    }

    @Test
    void shouldNormalizeNotFoundMessageFromBlogException() throws Exception {
        mockMvc.perform(get("/test/blog/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资源不存在或不可访问"));
    }

    @Test
    void shouldNormalizeInvalidParamMessageFromBlogException() throws Exception {
        mockMvc.perform(get("/test/blog/errors/invalid-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数不合法"));
    }

    @RestController
    static class DemoExceptionController {

        @GetMapping("/test/blog/errors/unauthorized")
        public ResponseEntity<Void> unauthorized() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        @GetMapping("/test/blog/errors/not-found")
        public ResponseEntity<Void> notFound() {
            throw new BlogException("博客不存在，ID: 77");
        }

        @GetMapping("/test/blog/errors/invalid-param")
        public ResponseEntity<Void> invalidParam() {
            throw new BlogException("博客 ID 不能为空");
        }
    }
}
