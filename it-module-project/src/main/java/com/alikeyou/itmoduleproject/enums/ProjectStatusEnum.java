/**
 * 项目状态枚举
 * 定义了项目的不同状态
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectStatusEnum {
    DRAFT("draft"),
    PENDING("pending"),
    PUBLISHED("published"),
    REJECTED("rejected"),
    ARCHIVED("archived");

    private final String value;

    ProjectStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}