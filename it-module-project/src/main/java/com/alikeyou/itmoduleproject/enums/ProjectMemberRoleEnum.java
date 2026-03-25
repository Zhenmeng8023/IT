/**
 * 项目成员角色枚举
 * 定义了项目中成员的不同角色类型
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectMemberRoleEnum {
    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member"),
    VIEWER("viewer");

    private final String value;

    ProjectMemberRoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}