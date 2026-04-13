package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class AiCurrentUserProvider {

    public Long requireCurrentUserId() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userId;
    }

    public Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Long userId = extractUserId(authentication.getPrincipal());
            if (userId == null) {
                userId = extractUserId(authentication.getDetails());
            }
            if (userId == null) {
                userId = parseLong(authentication.getName());
            }
            if (userId != null) {
                return userId;
            }
        }
        return LoginConstant.getUserId();
    }

    public boolean hasAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
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

    public boolean isAdminAiViewer() {
        return hasAuthority("view:ai:log")
                || hasAuthority("view:ai:model-admin")
                || hasAuthority("view:ai:prompt-template")
                || hasAuthority("view:admin:dashboard");
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
            for (String key : List.of("id", "userId", "uid", "user_id")) {
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
        if (raw == null || raw.isBlank() || "anonymousUser".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
