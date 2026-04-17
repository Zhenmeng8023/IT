package com.alikeyou.itmoduleai.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum AiStructuredApplyTarget {
    DISPLAY_TEXT("display.text"),
    PROJECT_DETAIL_SUMMARY("project.detail.summary"),
    PROJECT_DETAIL_TASKS("project.detail.tasks"),
    PROJECT_DETAIL_RISKS("project.detail.risks"),
    PROJECT_DETAIL_NEXT_STEPS("project.detail.next-steps"),
    BLOG_POLISH("blog.polish"),
    BLOG_SUMMARY("blog.summary"),
    BLOG_TAGS("blog.tags"),
    BLOG_REJECT_TAGS("blog.reject-tags"),
    KNOWLEDGE_BASE_EXPLAIN("knowledge.base.explain"),
    PAGE_EXPLAIN("page.explain");

    private final String code;

    AiStructuredApplyTarget(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AiStructuredApplyTarget fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (AiStructuredApplyTarget item : values()) {
            if (item.code.equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}
