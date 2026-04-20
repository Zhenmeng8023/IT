package com.alikeyou.itmodulecircle.support;

import com.alikeyou.itmodulecircle.entity.CircleComment;

public final class CircleCommentVisibilitySupport {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_DELETED = "deleted";

    public static final String LEGACY_STATUS_PENDING = "hidden";
    public static final String LEGACY_STATUS_PUBLISHED = "normal";

    public static final boolean REPLIES_REQUIRE_MODERATION = false;

    private CircleCommentVisibilitySupport() {
    }

    public static boolean isRootPost(CircleComment comment) {
        return comment != null && comment.getParentCommentId() == null;
    }

    public static boolean isReply(CircleComment comment) {
        return comment != null && comment.getParentCommentId() != null;
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return STATUS_PENDING;
        }

        String normalized = status.trim().toLowerCase();
        if ("approved".equals(normalized) || LEGACY_STATUS_PUBLISHED.equals(normalized)) {
            return STATUS_PUBLISHED;
        }
        if (LEGACY_STATUS_PENDING.equals(normalized)) {
            return STATUS_PENDING;
        }
        if ("close".equals(normalized) || "closed".equals(normalized)) {
            return STATUS_DELETED;
        }
        return normalized;
    }

    public static boolean isPublicVisiblePost(CircleComment comment) {
        return isRootPost(comment) && STATUS_PUBLISHED.equals(normalizeStatus(comment.getStatus()));
    }

    public static boolean isPublicVisibleReply(CircleComment comment) {
        return isReply(comment) && STATUS_PUBLISHED.equals(normalizeStatus(comment.getStatus()));
    }

    public static boolean isPublicVisibleComment(CircleComment comment) {
        return isPublicVisiblePost(comment) || isPublicVisibleReply(comment);
    }

    public static String initialNormalizedStatusForNewComment(Long parentCommentId) {
        if (parentCommentId == null) {
            return STATUS_PENDING;
        }
        return REPLIES_REQUIRE_MODERATION ? STATUS_PENDING : STATUS_PUBLISHED;
    }

    public static String compatiblePersistedStatus(String normalizedStatus) {
        if (STATUS_PUBLISHED.equals(normalizedStatus)) {
            return LEGACY_STATUS_PUBLISHED;
        }
        if (STATUS_PENDING.equals(normalizedStatus)) {
            return LEGACY_STATUS_PENDING;
        }
        return normalizedStatus;
    }
}
