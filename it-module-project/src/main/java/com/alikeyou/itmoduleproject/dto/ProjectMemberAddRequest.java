/**
 * 添加项目成员请求DTO
 * 用于接收添加项目成员的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 添加项目成员请求DTO
 * 用于接收添加项目成员的请求参数
 */
@Data
public class ProjectMemberAddRequest {
    /**
     * 项目ID
     */
    private Long projectId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 角色，对应ProjectMemberRoleEnum枚举
     */
    private String role;
}