/**
 * 项目任务状态枚举
 * 定义了任务的不同状态
 */
package com.alikeyou.itmoduleproject.enums;

import java.util.Arrays;

public enum ProjectTaskStatusEnum {
    TODO("todo"),
    IN_PROGRESS("in_progress"),
    DONE("done");

    private final String value;

    ProjectTaskStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}