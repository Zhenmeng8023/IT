/**
 * 更新项目任务请求DTO
 * 用于接收更新项目任务的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新项目任务请求DTO
 * 用于接收更新项目任务的请求参数
 */
@Data
public class ProjectTaskUpdateRequest {
    /**
     * 任务标题
     */
    private String title;
    /**
     * 任务描述
     */
    private String description;
    /**
     * 任务优先级，对应ProjectTaskPriorityEnum枚举
     */
    private String priority;
    /**
     * 任务负责人ID
     */
    private Long assigneeId;
    /**
     * 任务状态，对应ProjectTaskStatusEnum枚举
     */
    private String status;
    /**
     * 任务截止日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;
}