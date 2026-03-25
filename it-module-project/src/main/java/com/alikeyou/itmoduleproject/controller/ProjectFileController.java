/**
 * 项目文件控制器
 * 处理项目文件相关的HTTP请求
 */
package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 项目文件控制器
 * 处理项目文件相关的HTTP请求
 */
@RestController
@RequestMapping("/api/project/file")
@RequiredArgsConstructor
@Tag(name = "项目文件模块")
public class ProjectFileController {

    /**
     * 项目文件服务
     */
    private final ProjectFileService projectFileService;
    /**
     * 当前用户提供者
     */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 上传项目文件
     * @param projectId 项目ID
     * @param file 上传的文件
     * @param isMain 是否为主文件
     * @param version 版本号
     * @param commitMessage 提交信息
     * @param request HTTP请求对象
     * @return 文件信息响应
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目文件")
    public ResponseEntity<ApiResponse<ProjectFileVO>> uploadFile(@RequestParam Long projectId,
                                                                 @RequestPart MultipartFile file,
                                                                 @RequestParam(defaultValue = "false") Boolean isMain,
                                                                 @RequestParam(required = false) String version,
                                                                 @RequestParam(required = false) String commitMessage,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.uploadFile(projectId, file, isMain, version, commitMessage, currentUserId)));
    }

    /**
     * 上传文件新版本
     * @param fileId 文件ID
     * @param file 上传的文件
     * @param version 版本号
     * @param commitMessage 提交信息
     * @param request HTTP请求对象
     * @return 文件信息响应
     */
    @PostMapping(value = "/{fileId}/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件新版本")
    public ResponseEntity<ApiResponse<ProjectFileVO>> uploadNewVersion(@PathVariable Long fileId,
                                                                       @RequestPart MultipartFile file,
                                                                       @RequestParam(required = false) String version,
                                                                       @RequestParam(required = false) String commitMessage,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.uploadNewVersion(fileId, file, version, commitMessage, currentUserId)));
    }

    /**
     * 获取项目文件列表
     * @param projectId 项目ID
     * @param request HTTP请求对象
     * @return 文件列表响应
     */
    @GetMapping("/list")
    @Operation(summary = "文件列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVO>>> listFiles(@RequestParam Long projectId,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listFiles(projectId, currentUserId)));
    }

    /**
     * 获取文件版本列表
     * @param fileId 文件ID
     * @param request HTTP请求对象
     * @return 版本列表响应
     */
    @GetMapping("/{fileId}/versions")
    @Operation(summary = "版本列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVersionVO>>> listVersions(@PathVariable Long fileId,
                                                                                HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listVersions(fileId, currentUserId)));
    }

    /**
     * 下载文件
     * @param fileId 文件ID
     * @param request HTTP请求对象
     * @return 文件资源响应
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        Resource resource = projectFileService.downloadFile(fileId, currentUserId);
        String filename = resource.getFilename() == null ? "project-file" : resource.getFilename();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}