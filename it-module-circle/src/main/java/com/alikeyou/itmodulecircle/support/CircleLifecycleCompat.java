package com.alikeyou.itmodulecircle.support;

import com.alikeyou.itmodulecircle.entity.Circle;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CircleLifecycleCompat {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String CLOSED = "close";
    public static final String REJECTED = "rejected";
    public static final String COMPAT_PENDING = PENDING;
    public static final String COMPAT_NORMAL = "normal";
    public static final String COMPAT_CLOSED = "closed";
    public static final String COMPAT_VIOLATION = "violation";
    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_PRIVATE = "private";
    public static final String VISIBILITY_APPROVAL = "approval";

    private static final Set<String> WORKFLOW_TYPES = Set.of(PENDING, APPROVED, CLOSED, REJECTED);
    private static final Pattern LIFECYCLE_MARKER =
            Pattern.compile("\\[circle-lifecycle:(pending|approved|close|rejected)]", Pattern.CASE_INSENSITIVE);

    private CircleLifecycleCompat() {
    }

    public static String getLifecycleStatus(Circle circle) {
        if (circle == null) {
            return PENDING;
        }
        return getLifecycleStatus(circle.getType(), circle.getDescription());
    }

    public static String getLifecycleStatus(String rawType, String description) {
        String normalizedType = normalize(rawType);
        if (WORKFLOW_TYPES.contains(normalizedType)) {
            return normalizedType;
        }

        Matcher matcher = LIFECYCLE_MARKER.matcher(description == null ? "" : description);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        if (VISIBILITY_PUBLIC.equals(normalizedType)
                || VISIBILITY_PRIVATE.equals(normalizedType)
                || "official".equals(normalizedType)) {
            return APPROVED;
        }
        return PENDING;
    }

    public static String resolveLifecycle(Circle circle) {
        return getLifecycleStatus(circle);
    }

    public static String resolveLifecycle(String rawType, String description) {
        return getLifecycleStatus(rawType, description);
    }

    public static String stripLifecycleMarker(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }
        String sanitized = LIFECYCLE_MARKER.matcher(description).replaceAll("").trim();
        return sanitized.replaceFirst("^[\\r\\n]+", "").trim();
    }

    public static String applyLifecycleMarker(String description, String lifecycle) {
        String normalizedLifecycle = normalize(lifecycle);
        if (!WORKFLOW_TYPES.contains(normalizedLifecycle)) {
            return stripLifecycleMarker(description);
        }

        String sanitized = stripLifecycleMarker(description);
        String marker = "[circle-lifecycle:" + normalizedLifecycle + "]";
        if (sanitized == null || sanitized.isBlank()) {
            return marker;
        }
        return marker + "\n" + sanitized;
    }

    public static boolean isApprovedCircle(Circle circle) {
        return APPROVED.equals(getLifecycleStatus(circle));
    }

    public static boolean isPendingCircle(Circle circle) {
        return PENDING.equals(getLifecycleStatus(circle));
    }

    public static boolean isRejectedCircle(Circle circle) {
        return REJECTED.equals(getLifecycleStatus(circle));
    }

    public static boolean isClosedCircle(Circle circle) {
        return CLOSED.equals(getLifecycleStatus(circle));
    }

    public static boolean isPublicVisibleCircle(Circle circle) {
        return circle != null && isApprovedCircle(circle) && isPublicVisibility(circle.getVisibility());
    }

    public static boolean isPublicVisibility(String visibility) {
        return VISIBILITY_PUBLIC.equals(normalize(visibility));
    }

    public static boolean isApprovedPublic(Circle circle) {
        return isPublicVisibleCircle(circle);
    }

    public static String normalizeLifecycleFilter(String candidate, boolean strict) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }

        String normalized = normalize(candidate);
        if (COMPAT_NORMAL.equals(normalized)) {
            return APPROVED;
        }
        if (COMPAT_CLOSED.equals(normalized)) {
            return CLOSED;
        }
        if (COMPAT_VIOLATION.equals(normalized)) {
            return REJECTED;
        }
        if (WORKFLOW_TYPES.contains(normalized)) {
            return normalized;
        }
        if (strict) {
            throw new IllegalArgumentException("无效状态筛选值，仅支持 pending/approved/close/rejected/normal/closed/violation");
        }
        return null;
    }

    public static String toCompatibilityStatus(String lifecycle) {
        String normalized = normalize(lifecycle);
        if (APPROVED.equals(normalized)) {
            return COMPAT_NORMAL;
        }
        if (CLOSED.equals(normalized)) {
            return COMPAT_CLOSED;
        }
        if (REJECTED.equals(normalized)) {
            return COMPAT_VIOLATION;
        }
        return COMPAT_PENDING;
    }

    public static boolean isWorkflowType(String type) {
        return WORKFLOW_TYPES.contains(normalize(type));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
