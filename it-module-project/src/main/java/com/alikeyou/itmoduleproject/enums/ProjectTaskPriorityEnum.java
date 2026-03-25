/**
 * 项目任务优先级枚举
 * 定义了任务的不同优先级级别
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectTaskPriorityEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    private final String value;

    ProjectTaskPriorityEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}