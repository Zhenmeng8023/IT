/**
 * 上传文件新版本请求DTO
 * 用于接收上传文件新版本的请求参数
 */
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

/**
 * 上传文件新版本请求DTO
 * 用于接收上传文件新版本的请求参数
 */
@Data
public class ProjectFileVersionUploadRequest {
    /**
     * 文件ID
     */
    private Long fileId;
    /**
     * 版本号
     */
    private String version;
    /**
     * 提交信息
     */
    private String commitMessage;
}