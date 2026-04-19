package com.alikeyou.itmoduleblog.exception;

import com.alikeyou.itmoduleblog.support.BlogMessageNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.alikeyou.itmoduleblog")
public class BlogExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<Map<String, Object>> handleBlogException(BlogException exception) {
        HttpStatus status = BlogMessageNormalizer.resolveStatus(HttpStatus.BAD_REQUEST, exception.getMessage());
        return buildBody(status, exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = BlogMessageNormalizer.resolveStatus(exception.getStatusCode(), exception.getReason());
        return buildBody(status, exception.getReason());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exception) {
        HttpStatus status = BlogMessageNormalizer.resolveStatus(HttpStatus.BAD_REQUEST, exception.getMessage());
        return buildBody(status, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        return buildBody(HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    private ResponseEntity<Map<String, Object>> buildBody(HttpStatus status, String rawMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "BLOG_" + status.name());
        body.put("message", BlogMessageNormalizer.normalize(status, rawMessage));
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
