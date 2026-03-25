/**
 * 项目成员状态枚举
 * 定义了项目成员的不同状态
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectMemberStatusEnum {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ProjectMemberStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}