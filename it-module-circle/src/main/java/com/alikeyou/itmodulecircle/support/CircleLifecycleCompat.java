package com.alikeyou.itmodulecircle.support;

import com.alikeyou.itmodulecircle.entity.Circle;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CircleLifecycleCompat {

    private static final Set<String> WORKFLOW_TYPES = Set.of("pending", "approved", "close", "rejected");
    private static final Pattern LIFECYCLE_MARKER =
            Pattern.compile("\\[circle-lifecycle:(pending|approved|close|rejected)]", Pattern.CASE_INSENSITIVE);

    private CircleLifecycleCompat() {
    }

    public static String resolveLifecycle(Circle circle) {
        if (circle == null) {
            return "pending";
        }
        return resolveLifecycle(circle.getType(), circle.getDescription());
    }

    public static String resolveLifecycle(String rawType, String description) {
        String normalizedType = normalize(rawType);
        if (WORKFLOW_TYPES.contains(normalizedType)) {
            return normalizedType;
        }

        Matcher matcher = LIFECYCLE_MARKER.matcher(description == null ? "" : description);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        if ("public".equals(normalizedType) || "private".equals(normalizedType) || "official".equals(normalizedType)) {
            return "approved";
        }
        return "pending";
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

    public static boolean isApprovedPublic(Circle circle) {
        return circle != null
                && "approved".equals(resolveLifecycle(circle))
                && "public".equalsIgnoreCase(normalize(circle.getVisibility()));
    }

    public static boolean isWorkflowType(String type) {
        return WORKFLOW_TYPES.contains(normalize(type));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
