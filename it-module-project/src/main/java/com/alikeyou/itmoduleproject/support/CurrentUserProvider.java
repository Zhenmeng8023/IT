package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CurrentUserProvider {

    private static final List<String> ID_KEYS = List.of("userId", "user_id", "uid", "id", "sub");
    private static final List<String> ID_METHODS = List.of("getUserId", "getId", "getUid");
    private static final List<String> ID_FIELDS = List.of("userId", "id", "uid");

    private final UserInfoLiteRepository userInfoLiteRepository;

    public CurrentUserProvider(UserInfoLiteRepository userInfoLiteRepository) {
        this.userInfoLiteRepository = userInfoLiteRepository;
    }

    public Long getCurrentUserIdRequired(HttpServletRequest request) {
        Long currentUserId = getCurrentUserIdOrNull(request);
        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        return currentUserId;
    }

    public Long getCurrentUserIdOrNull(HttpServletRequest request) {
        Long fromRequest = extractFromRequest(request);
        if (fromRequest != null) {
            return fromRequest;
        }

        Long fromSecurityContext = extractFromSecurityContext();
        if (fromSecurityContext != null) {
            return fromSecurityContext;
        }

        return extractFromLegacyHeader(request);
    }

    public Long getCurrentUserId(HttpServletRequest request) {
        return getCurrentUserIdRequired(request);
    }

    private Long extractFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        for (String key : List.of("currentUserId", "userId", "uid")) {
            Long value = tryExtract(request.getAttribute(key));
            if (value != null) {
                return value;
            }
        }

        Principal principal = request.getUserPrincipal();
        Long fromPrincipal = tryExtract(principal == null ? null : principal.getName());
        if (fromPrincipal != null) {
            return fromPrincipal;
        }

        return null;
    }

    private Long extractFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Long fromPrincipal = tryExtract(authentication.getPrincipal());
        if (fromPrincipal != null) {
            return fromPrincipal;
        }

        Long fromDetails = tryExtract(authentication.getDetails());
        if (fromDetails != null) {
            return fromDetails;
        }

        return tryExtract(authentication.getName());
    }

    private Long extractFromLegacyHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return tryExtract(request.getHeader("X-User-Id"));
    }

    private Long tryExtract(Object source) {
        if (source == null) {
            return null;
        }

        if (source instanceof Number number) {
            return number.longValue();
        }

        if (source instanceof CharSequence sequence) {
            return parseUserIdOrUsername(sequence.toString());
        }

        if (source instanceof Map<?, ?> map) {
            for (String key : ID_KEYS) {
                Long extracted = tryExtract(map.get(key));
                if (extracted != null) {
                    return extracted;
                }
            }
            return null;
        }

        for (String methodName : ID_METHODS) {
            Method method = ReflectionUtils.findMethod(source.getClass(), methodName);
            if (method != null) {
                try {
                    ReflectionUtils.makeAccessible(method);
                    Long extracted = tryExtract(ReflectionUtils.invokeMethod(method, source));
                    if (extracted != null) {
                        return extracted;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        for (String fieldName : ID_FIELDS) {
            Field field = ReflectionUtils.findField(source.getClass(), fieldName);
            if (field != null) {
                try {
                    ReflectionUtils.makeAccessible(field);
                    Long extracted = tryExtract(field.get(source));
                    if (extracted != null) {
                        return extracted;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private Long parseUserIdOrUsername(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        if ("anonymousUser".equalsIgnoreCase(trimmed)) {
            return null;
        }

        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return resolveUserIdByUsername(trimmed);
        }
    }

    private Long resolveUserIdByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        Optional<UserInfoLite> optional = userInfoLiteRepository.findByUsername(username.trim());
        return optional.map(UserInfoLite::getId).orElse(null);
    }
}
