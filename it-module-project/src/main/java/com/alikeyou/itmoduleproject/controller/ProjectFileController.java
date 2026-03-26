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

@RestController
@RequestMapping("/api/project/file")
@RequiredArgsConstructor
@Tag(name = "项目文件模块")
public class ProjectFileController {

    private final ProjectFileService projectFileService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目文件")
    public ResponseEntity<ApiResponse<ProjectFileVO>> uploadFile(@RequestParam Long projectId,
                                                                 @RequestPart MultipartFile file,
                                                                 @RequestParam(defaultValue = "false") Boolean isMain,
                                                                 @RequestParam(required = false) String version,
                                                                 @RequestParam(required = false) String commitMessage,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.uploadFile(projectId, file, isMain, version, commitMessage, currentUserId)));
    }

    @PostMapping(value = "/{fileId}/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件新版本")
    public ResponseEntity<ApiResponse<ProjectFileVO>> uploadNewVersion(@PathVariable Long fileId,
                                                                       @RequestPart MultipartFile file,
                                                                       @RequestParam(required = false) String version,
                                                                       @RequestParam(required = false) String commitMessage,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.uploadNewVersion(fileId, file, version, commitMessage, currentUserId)));
    }

    @GetMapping("/list")
    @Operation(summary = "文件列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVO>>> listFiles(@RequestParam Long projectId,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listFiles(projectId, currentUserId)));
    }

    @GetMapping("/{fileId}/versions")
    @Operation(summary = "版本列表")
    public ResponseEntity<ApiResponse<List<ProjectFileVersionVO>>> listVersions(@PathVariable Long fileId,
                                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listVersions(fileId, currentUserId)));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Resource resource = projectFileService.downloadFile(fileId, currentUserId);
        String filename = resource.getFilename() == null ? "project-file" : resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping("/{fileId}/main")
    @Operation(summary = "设为主文件")
    public ResponseEntity<ApiResponse<ProjectFileVO>> setMainFile(@PathVariable Long fileId,
                                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.setMainFile(fileId, currentUserId)));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId,
                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectFileService.deleteFile(fileId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}
