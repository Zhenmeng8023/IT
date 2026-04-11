package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectFileBatchDownloadRequest;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectDownloadRecordService;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/project/file")
@RequiredArgsConstructor
@Tag(name = "项目文件模块")
public class ProjectFileController {

    private final ProjectFileService projectFileService;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectActivityLogService projectActivityLogService;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectDownloadRecordService projectDownloadRecordService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目文件")
    public ResponseEntity<ApiResponse<ProjectWorkspaceItemVO>> uploadFile(@RequestParam Long projectId,
                                                                          @RequestParam(value = "branchId", required = false) Long branchId,
                                                                          @RequestParam(value = "canonicalPath", required = false) String canonicalPath,
                                                                          @RequestParam("file") MultipartFile file,
                                                                         HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectWorkspaceItemVO result = projectFileService.uploadFile(projectId, branchId, canonicalPath, file, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item",
                result == null ? null : result.getId(),
                "文件已加入工作区：" + (result == null ? "" : result.getCanonicalPath()));
        return ResponseEntity.ok(ApiResponse.ok("文件已加入工作区", result));
    }

    @PostMapping(value = "/upload/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目 ZIP 压缩包并自动解压入库")
    public ResponseEntity<ApiResponse<List<ProjectWorkspaceItemVO>>> uploadZip(@RequestParam Long projectId,
                                                                               @RequestParam(value = "branchId", required = false) Long branchId,
                                                                               @RequestParam("file") MultipartFile file,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        List<ProjectWorkspaceItemVO> result = projectFileService.uploadZip(projectId, branchId, file, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item", null,
                "ZIP 文件已加入工作区，共 " + (result == null ? 0 : result.size()) + " 个");
        return ResponseEntity.ok(ApiResponse.ok("ZIP 文件已加入工作区", result));
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量上传项目文件")
    public ResponseEntity<ApiResponse<List<ProjectWorkspaceItemVO>>> uploadFiles(@RequestParam Long projectId,
                                                                                 @RequestParam(value = "branchId", required = false) Long branchId,
                                                                                 @RequestParam(value = "targetDir", required = false) String targetDir,
                                                                                 @RequestParam("files") List<MultipartFile> files,
                                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        List<ProjectWorkspaceItemVO> result = projectFileService.uploadFiles(projectId, branchId, targetDir, files, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item", null,
                "批量文件已加入工作区，共 " + (result == null ? 0 : result.size()) + " 个");
        return ResponseEntity.ok(ApiResponse.ok("批量文件已加入工作区", result));
    }

    @PostMapping(value = "/{fileId}/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件新版本")
    public ResponseEntity<ApiResponse<ProjectWorkspaceItemVO>> uploadNewVersion(@PathVariable Long fileId,
                                                                                @RequestParam(value = "branchId", required = false) Long branchId,
                                                                                @RequestParam("file") MultipartFile file,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectWorkspaceItemVO result = projectFileService.uploadNewVersion(fileId, branchId, file, currentUserId);
        ProjectFile projectFile = projectFileRepository.findById(fileId).orElse(null);
        if (projectFile != null) {
            projectActivityLogService.record(projectFile.getProjectId(), currentUserId, "stage_file", "workspace_item",
                    result == null ? null : result.getId(),
                    "新版本已加入工作区：" + (result == null ? "" : result.getCanonicalPath()));
        }
        return ResponseEntity.ok(ApiResponse.ok("新版本已加入工作区", result));
    }

    @GetMapping("/list")
    @Operation(summary = "文件列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVO>>> listFiles(@RequestParam Long projectId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listFiles(projectId, currentUserId)));
    }

    @GetMapping("/{fileId}/versions")
    @Operation(summary = "版本列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVersionVO>>> listVersions(@PathVariable Long fileId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listVersions(fileId, currentUserId)));
    }

    @GetMapping("/preview/{fileId}")
    @Operation(summary = "预览文件")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Resource resource = projectFileService.previewFile(fileId, currentUserId);
        String filename = resource.getFilename() == null ? "project-file" : resource.getFilename();
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable Long fileId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Resource resource = projectFileService.downloadFile(fileId, currentUserId);
        ProjectFile projectFile = projectFileRepository.findById(fileId).orElse(null);
        if (projectFile != null) {
            projectDownloadRecordService.recordDownload(
                    projectFile.getProjectId(),
                    projectFile.getId(),
                    null,
                    currentUserId,
                    resolveClientIp(request),
                    request == null ? null : request.getHeader("User-Agent")
            );
        }
        String filename = resource.getFilename() == null ? "project-file" : resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/download/batch")
    @Operation(summary = "批量下载文件")
    public ResponseEntity<Resource> downloadBatch(@RequestBody(required = false) ProjectFileBatchDownloadRequest body,
                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Long projectId = body == null ? null : body.getProjectId();
        List<Long> fileIds = body == null ? null : body.getFileIds();
        Resource resource = projectFileService.downloadFiles(projectId, fileIds, currentUserId);
        if (projectId != null) {
            List<ProjectFile> files;
            if (fileIds == null || fileIds.isEmpty()) {
                files = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId);
            } else {
                List<Long> ids = fileIds.stream().filter(Objects::nonNull).distinct().toList();
                files = projectFileRepository.findByProjectIdAndIdInOrderByUploadTimeDesc(projectId, ids);
            }
            for (ProjectFile item : files) {
                if (item == null || isFolderRecord(item) || !StringUtils.hasText(item.getFilePath())) {
                    continue;
                }
                projectDownloadRecordService.recordDownload(
                        projectId,
                        item.getId(),
                        null,
                        currentUserId,
                        resolveClientIp(request),
                        request == null ? null : request.getHeader("User-Agent")
                );
            }
        }
        String filename = resource.getFilename() == null ? "project-files.zip" : resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }

    @PutMapping("/{fileId}/main")
    @Operation(summary = "设为主文件")
    public ResponseEntity<ApiResponse<ProjectFileVO>> setMainFile(@PathVariable Long fileId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectFileVO result = projectFileService.setMainFile(fileId, currentUserId);
        if (result != null) {
            projectActivityLogService.record(result.getProjectId(), currentUserId, "set_main_file", "file", result.getId(), "设主文件：" + result.getFileName());
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectFile file = projectFileRepository.findById(fileId).orElse(null);
        Long projectId = file == null ? null : file.getProjectId();
        String fileName = file == null ? "" : file.getFileName();
        projectFileService.deleteFile(fileId, currentUserId);
        if (projectId != null) {
            projectActivityLogService.record(projectId, currentUserId, "delete_file", "file", fileId, "删除文件：" + fileName);
        }
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] keys = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String key : keys) {
            String value = request.getHeader(key);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value.trim())) {
                int index = value.indexOf(',');
                return index >= 0 ? value.substring(0, index).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isFolderRecord(ProjectFile projectFile) {
        return projectFile != null && "folder".equalsIgnoreCase(String.valueOf(projectFile.getFileType()));
    }
}
