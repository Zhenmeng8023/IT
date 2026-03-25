/**
 * 项目文件服务接口
 * 定义项目文件相关的业务逻辑方法
 */
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 项目文件服务接口
 * 定义项目文件相关的业务逻辑方法
 */
public interface ProjectFileService {
    /**
     * 上传项目文件
     * @param projectId 项目ID
     * @param file 上传的文件
     * @param isMain 是否为主文件
     * @param version 版本号
     * @param commitMessage 提交信息
     * @param currentUserId 当前用户ID
     * @return 文件信息
     */
    ProjectFileVO uploadFile(Long projectId, MultipartFile file, Boolean isMain, String version, String commitMessage, Long currentUserId);
    
    /**
     * 上传文件新版本
     * @param fileId 文件ID
     * @param file 上传的文件
     * @param version 版本号
     * @param commitMessage 提交信息
     * @param currentUserId 当前用户ID
     * @return 文件信息
     */
    ProjectFileVO uploadNewVersion(Long fileId, MultipartFile file, String version, String commitMessage, Long currentUserId);
    
    /**
     * 获取项目文件列表
     * @param projectId 项目ID
     * @param currentUserId 当前用户ID
     * @return 文件列表
     */
    List<ProjectFileVO> listFiles(Long projectId, Long currentUserId);
    
    /**
     * 获取文件版本列表
     * @param fileId 文件ID
     * @param currentUserId 当前用户ID
     * @return 版本列表
     */
    List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId);
    
    /**
     * 下载文件
     * @param fileId 文件ID
     * @param currentUserId 当前用户ID
     * @return 文件资源
     */
    Resource downloadFile(Long fileId, Long currentUserId);
}