/**
 * 上传项目文件请求DTO
 * 用于接收上传项目文件的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 上传项目文件请求DTO
 * 用于接收上传项目文件的请求参数
 */
@Data
public class ProjectFileUploadRequest {
    /**
     * 项目ID
     */
    private Long projectId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件类型
     */
    private String fileType;
    /**
     * 是否为主文件
     */
    private Boolean isMain;
    /**
     * 版本号
     */
    private String version;
    /**
     * 提交信息
     */
    private String commitMessage;
}