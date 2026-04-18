package com.alikeyou.itmodulesystem.config;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulesystem.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLogAutoRecordFilter extends OncePerRequestFilter {

    private static final String[] EXCLUDED_PATH_PREFIXES = new String[]{
            "/api/system/audit",
            "/api/admin/logs",
            "/error"
    };

    private final AuditLogService auditLogService;

    public AuditLogAutoRecordFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (!shouldAudit(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long begin = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            saveAuditLog(request, response, begin);
        }
    }

    private boolean shouldAudit(String method, String path) {
        if (method == null || path == null) {
            return false;
        }
        String upperMethod = method.toUpperCase(Locale.ROOT);
        if ("GET".equals(upperMethod) || "OPTIONS".equals(upperMethod) || "HEAD".equals(upperMethod)) {
            return false;
        }
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private void saveAuditLog(HttpServletRequest request, HttpServletResponse response, long beginMillis) {
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            int statusCode = response.getStatus();
            long cost = Math.max(0, System.currentTimeMillis() - beginMillis);

            Long userId = resolveUserId(request);
            String username = resolveUsername(userId);

            Map<String, Object> details = new HashMap<>();
            details.put("operator", username);
            details.put("module", resolveModule(path));
            details.put("type", resolveType(method, path, statusCode));
            details.put("result", statusCode >= 400 ? "failed" : "success");
            details.put("statusCode", statusCode);
            details.put("costMs", cost);
            details.put("method", method);
            details.put("path", path);
            details.put("time", Instant.now().toString());

            auditLogService.logAction(
                    method + " " + path,
                    userId,
                    resolveModule(path),
                    resolveTargetId(path),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    details
            );
        } catch (Exception ignored) {
            // Never break business flow because of audit logging.
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object direct = request.getAttribute("currentUserId");
        Long fromAttr = asLong(direct);
        if (fromAttr != null) {
            return fromAttr;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Long fromSession = asLong(session.getAttribute(LoginConstant.USER_ID));
            if (fromSession != null) {
                return fromSession;
            }
        }

        return LoginConstant.getUserId();
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            String fallback = LoginConstant.getUsername();
            return fallback == null ? "" : fallback;
        }
        String fallback = LoginConstant.getUsername();
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return String.valueOf(userId);
    }

    private String resolveModule(String path) {
        if (path == null || path.isBlank()) {
            return "system";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String[] parts = normalized.split("/");
        if (parts.length >= 2 && "api".equalsIgnoreCase(parts[0])) {
            return parts[1].toLowerCase(Locale.ROOT);
        }
        return parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "system";
    }

    private String resolveType(String method, String path, int statusCode) {
        String lowerPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
        String lowerMethod = method == null ? "" : method.toLowerCase(Locale.ROOT);
        if (statusCode >= 500) {
            return "error";
        }
        if (lowerPath.contains("login") || lowerPath.contains("logout") || lowerPath.contains("password")) {
            return "security";
        }
        if ("delete".equals(lowerMethod) || "put".equals(lowerMethod) || "post".equals(lowerMethod) || "patch".equals(lowerMethod)) {
            return "user";
        }
        return "system";
    }

    private Long resolveTargetId(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            Long candidate = asLong(parts[i]);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
