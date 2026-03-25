/**
 * 创建项目请求DTO
 * 用于接收创建项目的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 创建项目请求DTO
 * 用于接收创建项目的请求参数
 */
@Data
public class ProjectCreateRequest {
    /**
     * 项目名称
     */
    private String name;
    /**
     * 项目描述
     */
    private String description;
    /**
     * 项目分类
     */
    private String category;
    /**
     * 项目状态，对应ProjectStatusEnum枚举
     */
    private String status;
    /**
     * 项目标签，JSON格式
     */
    private String tags;
    /**
     * 模板ID
     */
    private Long templateId;
    /**
     * 项目可见性，对应ProjectVisibilityEnum枚举
     */
    private String visibility;
}