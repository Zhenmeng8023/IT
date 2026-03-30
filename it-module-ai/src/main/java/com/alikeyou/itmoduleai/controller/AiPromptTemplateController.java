package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.service.AiPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/prompt-templates")
@RequiredArgsConstructor
public class AiPromptTemplateController {

    private final AiPromptTemplateService aiPromptTemplateService;

    @GetMapping
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<Page<AiPromptTemplate>> page(Pageable pageable) {
        return ApiResponse.ok(aiPromptTemplateService.page(pageable));
    }

    @GetMapping("/scene/{sceneCode}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiPromptTemplate>> listByScene(@PathVariable String sceneCode) {
        return ApiResponse.ok(aiPromptTemplateService.listByScene(sceneCode));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiPromptTemplate>> listByOwner(@PathVariable Long ownerId) {
        Long effectiveOwnerId = hasAuthority("view:ai:prompt-template") ? ownerId : resolveCurrentUserId();
        return ApiResponse.ok(aiPromptTemplateService.listByOwner(effectiveOwnerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiPromptTemplate> get(@PathVariable Long id) {
        return ApiResponse.ok(aiPromptTemplateService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> save(@RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("保存成功", aiPromptTemplateService.save(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> update(@PathVariable Long id, @RequestBody AiPromptTemplate entity) {
        return ApiResponse.ok("更新成功", aiPromptTemplateService.update(id, entity));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> publish(@PathVariable Long id) {
        return ApiResponse.ok("发布成功", aiPromptTemplateService.publish(id));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('view:ai:prompt-template')")
    public ApiResponse<AiPromptTemplate> disable(@PathVariable Long id) {
        return ApiResponse.ok("停用成功", aiPromptTemplateService.disable(id));
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
