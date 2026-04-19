package com.alikeyou.itmodulecircle.support;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.List;
import java.util.regex.Pattern;

public final class CircleMessageNormalizer {

    public static final String LOGIN_REQUIRED = "请先登录后再操作";
    public static final String PERMISSION_DENIED = "无权限执行该操作";
    public static final String RESOURCE_NOT_FOUND = "资源不存在或不可访问";
    public static final String INVALID_PARAM = "请求参数不合法";
    public static final String INTERNAL_ERROR = "服务器内部错误";
    public static final String REQUEST_FAILED = "请求处理失败";
    public static final String OPERATION_SUCCESS = "操作成功";

    private static final Pattern ID_DETAIL_PATTERN =
            Pattern.compile("[，,]?\\s*ID\\s*[:：]\\s*[^，。,;；\\s]+", Pattern.CASE_INSENSITIVE);

    private static final List<String> INVALID_PARAM_KEYWORDS = List.of(
            "不能为空", "必须", "无效", "格式错误", "列表不能为空", "请求体格式错误", "不一致", "仅支持"
    );
    private static final List<String> AUTH_KEYWORDS = List.of("未登录", "登录");
    private static final List<String> FORBIDDEN_KEYWORDS = List.of("无权限", "权限", "仅作者", "仅本人", "仅圈主", "圈子管理员");
    private static final List<String> NOT_FOUND_KEYWORDS = List.of("不存在", "不可访问");

    private CircleMessageNormalizer() {
    }

    public static HttpStatus resolveStatus(HttpStatusCode sourceStatus, String rawMessage) {
        HttpStatus baseStatus = toHttpStatus(sourceStatus);
        String message = safe(rawMessage);
        if (baseStatus == HttpStatus.BAD_REQUEST) {
            if (isTechnicalMessage(message)) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            if (containsAny(message, AUTH_KEYWORDS)) {
                return HttpStatus.UNAUTHORIZED;
            }
            if (containsAny(message, FORBIDDEN_KEYWORDS)) {
                return HttpStatus.FORBIDDEN;
            }
            if (containsAny(message, NOT_FOUND_KEYWORDS)) {
                return HttpStatus.NOT_FOUND;
            }
        }
        return baseStatus;
    }

    public static String normalize(HttpStatusCode sourceStatus, String rawMessage) {
        HttpStatus resolvedStatus = resolveStatus(sourceStatus, rawMessage);
        return normalize(resolvedStatus, rawMessage);
    }

    public static String normalize(HttpStatus resolvedStatus, String rawMessage) {
        if (resolvedStatus == HttpStatus.UNAUTHORIZED) {
            return LOGIN_REQUIRED;
        }
        if (resolvedStatus == HttpStatus.FORBIDDEN) {
            return PERMISSION_DENIED;
        }
        if (resolvedStatus == HttpStatus.NOT_FOUND) {
            return RESOURCE_NOT_FOUND;
        }
        if (resolvedStatus.is5xxServerError()) {
            return INTERNAL_ERROR;
        }

        String sanitized = sanitize(rawMessage);
        if (sanitized.isBlank()) {
            return resolvedStatus == HttpStatus.BAD_REQUEST ? INVALID_PARAM : REQUEST_FAILED;
        }
        if (resolvedStatus == HttpStatus.BAD_REQUEST && containsAny(sanitized, INVALID_PARAM_KEYWORDS)) {
            return INVALID_PARAM;
        }
        if (sanitized.contains("失败:")) {
            return REQUEST_FAILED;
        }
        if (isTechnicalMessage(sanitized)) {
            return REQUEST_FAILED;
        }
        return sanitized;
    }

    public static HttpStatus toHttpStatus(HttpStatusCode statusCode) {
        HttpStatus resolved = statusCode == null ? null : HttpStatus.resolve(statusCode.value());
        return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
    }

    public static String normalizeSuccess(String successMessage) {
        return safe(successMessage).isBlank() ? OPERATION_SUCCESS : successMessage.trim();
    }

    public static String sanitize(String rawMessage) {
        String message = safe(rawMessage).trim();
        if (message.isEmpty()) {
            return "";
        }
        message = ID_DETAIL_PATTERN.matcher(message).replaceAll("");
        message = message.replaceAll("[，,]\\s*$", "");
        return message.trim();
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    private static boolean containsAny(String text, List<String> tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTechnicalMessage(String text) {
        String normalized = safe(text).trim().toLowerCase();
        return normalized.contains("exception")
                || normalized.contains("nullpointer")
                || normalized.contains("sql")
                || normalized.contains("hibernate")
                || normalized.contains("cannot ")
                || normalized.contains("failed")
                || normalized.startsWith("java.");
    }
}
