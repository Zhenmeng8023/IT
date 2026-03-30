package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.service.AiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/logs")
@RequiredArgsConstructor
public class AiLogController {

    private final AiLogService aiLogService;

    @PostMapping("/feedback")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiFeedbackLog> saveFeedback(@RequestBody AiFeedbackCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        request.setUserId(resolveCurrentUserId());
        return ApiResponse.ok("反馈成功", aiLogService.saveFeedback(request));
    }

    @GetMapping("/user/{userId}/calls")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AiCallLog>> pageUserCalls(@PathVariable Long userId, Pageable pageable) {
        Long effectiveUserId = hasAuthority("view:ai:log") ? userId : resolveCurrentUserId();
        return ApiResponse.ok(aiLogService.pageUserCallLogs(effectiveUserId, pageable));
    }

    @GetMapping("/session/{sessionId}/calls")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AiCallLog>> pageSessionCalls(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiLogService.pageSessionCallLogs(sessionId, pageable));
    }

    @GetMapping("/call/{callLogId}/retrievals")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiRetrievalLog>> listRetrievals(@PathVariable Long callLogId) {
        return ApiResponse.ok(aiLogService.listRetrievalLogs(callLogId));
    }

    @GetMapping("/message/{messageId}/feedbacks")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiFeedbackLog>> listMessageFeedbacks(@PathVariable Long messageId) {
        return ApiResponse.ok(aiLogService.listMessageFeedbacks(messageId));
    }

    @GetMapping("/user/{userId}/feedbacks")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiFeedbackLog>> listUserFeedbacks(@PathVariable Long userId) {
        Long effectiveUserId = hasAuthority("view:ai:log") ? userId : resolveCurrentUserId();
        return ApiResponse.ok(aiLogService.listUserFeedbacks(effectiveUserId));
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authority == null || authority.isBlank()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority grantedAuthority : authorities) {
            if (grantedAuthority != null && authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录状态已失效");
        }
        Long userId = extractUserId(authentication.getPrincipal());
        if (userId == null) {
            userId = extractUserId(authentication.getDetails());
        }
        if (userId == null) {
            userId = parseLong(authentication.getName());
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前登录用户");
        }
        return userId;
    }

    private Long extractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof CharSequence sequence) {
            return parseLong(sequence.toString());
        }
        if (source instanceof Map<?, ?> map) {
            for (String key : List.of("id", "userId", "uid")) {
                Object value = map.get(key);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        }
        for (String methodName : List.of("getId", "getUserId", "getUid")) {
            try {
                Method method = source.getClass().getMethod(methodName);
                Object value = method.invoke(source);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
