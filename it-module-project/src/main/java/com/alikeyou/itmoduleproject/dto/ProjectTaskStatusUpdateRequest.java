/**
 * 更新项目任务状态请求DTO
 * 用于接收更新项目任务状态的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 更新项目任务状态请求DTO
 * 用于接收更新项目任务状态的请求参数
 */
@Data
public class ProjectTaskStatusUpdateRequest {
    /**
     * 新状态，对应ProjectTaskStatusEnum枚举
     */
    private String status;
}