package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.vo.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(basePackages = "com.alikeyou.itmoduleproject")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[project-workspace-upload] business exception: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[project-workspace-upload] illegal argument: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[project-workspace-upload] validation exception: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail("请求参数不合法"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("[project-workspace-upload] max upload size exceeded: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail("上传内容过大，请减少单次上传的文件数量或压缩后再上传"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e) {
        log.warn("[project-workspace-upload] multipart exception: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.fail("上传请求解析失败，请减少单次上传的文件数量后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[project-workspace-upload] unhandled exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
    }
}
