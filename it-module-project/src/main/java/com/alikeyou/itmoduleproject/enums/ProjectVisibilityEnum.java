/**
 * 项目可见性枚举
 * 定义了项目的不同可见性级别
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectVisibilityEnum {
    PUBLIC("public"),
    FRIENDS_ONLY("friends_only"),
    PRIVATE("private");

    private final String value;

    ProjectVisibilityEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}