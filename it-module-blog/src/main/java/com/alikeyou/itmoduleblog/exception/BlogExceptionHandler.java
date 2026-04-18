package com.alikeyou.itmoduleblog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.alikeyou.itmoduleblog")
public class BlogExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<Map<String, Object>> handleBlogException(BlogException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "BLOG_BAD_REQUEST");
        body.put("message", exception.getMessage());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
