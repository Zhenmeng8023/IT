/**
 * 更新项目成员角色请求DTO
 * 用于接收更新项目成员角色的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 更新项目成员角色请求DTO
 * 用于接收更新项目成员角色的请求参数
 */
@Data
public class ProjectMemberRoleUpdateRequest {
    /**
     * 成员ID
     */
    private Long memberId;
    /**
     * 新角色，对应ProjectMemberRoleEnum枚举
     */
    private String role;
}