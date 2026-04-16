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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/project/file")
@RequiredArgsConstructor
@Tag(name = "Project File")
public class ProjectFileController {

    private final ProjectFileService projectFileService;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectActivityLogService projectActivityLogService;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectDownloadRecordService projectDownloadRecordService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file to workspace")
    public ResponseEntity<ApiResponse<ProjectWorkspaceItemVO>> uploadFile(@RequestParam Long projectId,
                                                                          @RequestParam("branchId") Long branchId,
                                                                          @RequestParam(value = "canonicalPath", required = false) String canonicalPath,
                                                                          @RequestParam("file") MultipartFile file,
                                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectWorkspaceItemVO result = projectFileService.uploadFile(projectId, branchId, canonicalPath, file, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item",
                result == null ? null : result.getId(),
                "File staged: " + (result == null ? "" : result.getCanonicalPath()));
        return ResponseEntity.ok(ApiResponse.ok("File staged", result));
    }

    @PostMapping(value = "/upload/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ZIP package")
    public ResponseEntity<ApiResponse<List<ProjectWorkspaceItemVO>>> uploadZip(@RequestParam Long projectId,
                                                                                @RequestParam("branchId") Long branchId,
                                                                                @RequestParam("file") MultipartFile file,
                                                                                HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        List<ProjectWorkspaceItemVO> result = projectFileService.uploadZip(projectId, branchId, file, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item", null,
                "ZIP staged, item count: " + (result == null ? 0 : result.size()));
        return ResponseEntity.ok(ApiResponse.ok("ZIP staged", result));
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Batch upload files")
    public ResponseEntity<ApiResponse<List<ProjectWorkspaceItemVO>>> uploadFiles(@RequestParam Long projectId,
                                                                                  @RequestParam("branchId") Long branchId,
                                                                                  @RequestParam(value = "targetDir", required = false) String targetDir,
                                                                                  @RequestParam("files") List<MultipartFile> files,
                                                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        List<ProjectWorkspaceItemVO> result = projectFileService.uploadFiles(projectId, branchId, targetDir, files, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "stage_file", "workspace_item", null,
                "Batch files staged, count: " + (result == null ? 0 : result.size()));
        return ResponseEntity.ok(ApiResponse.ok("Batch files staged", result));
    }

    @PostMapping(value = "/{fileId}/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload new file version")
    public ResponseEntity<ApiResponse<ProjectWorkspaceItemVO>> uploadNewVersion(@PathVariable Long fileId,
                                                                                 @RequestParam("branchId") Long branchId,
                                                                                 @RequestParam("file") MultipartFile file,
                                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectWorkspaceItemVO result = projectFileService.uploadNewVersion(fileId, branchId, file, currentUserId);
        ProjectFile projectFile = projectFileRepository.findById(fileId).orElse(null);
        if (projectFile != null) {
            projectActivityLogService.record(projectFile.getProjectId(), currentUserId, "stage_file", "workspace_item",
                    result == null ? null : result.getId(),
                    "New version staged: " + (result == null ? "" : result.getCanonicalPath()));
        }
        return ResponseEntity.ok(ApiResponse.ok("New version staged", result));
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public ResponseEntity<ApiResponse<List<ProjectFileVO>>> listFiles(@RequestParam Long projectId,
                                                                       @RequestParam(value = "branchId", required = false) Long branchId,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listFiles(projectId, branchId, currentUserId)));
    }

    @GetMapping("/{fileId}/versions")
    @Operation(summary = "List versions")
    public ResponseEntity<ApiResponse<List<ProjectFileVersionVO>>> listVersions(@PathVariable Long fileId,
                                                                                 @RequestParam(value = "branchId", required = false) Long branchId,
                                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectFileService.listVersions(fileId, branchId, currentUserId)));
    }

    @GetMapping("/preview/{fileId}")
    @Operation(summary = "Preview file")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId,
                                            @RequestParam(value = "branchId", required = false) Long branchId,
                                            HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Resource resource = projectFileService.previewFile(fileId, branchId, currentUserId);
        String filename = resource.getFilename() == null ? "project-file" : resource.getFilename();
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Download file")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             @RequestParam(value = "branchId", required = false) Long branchId,
                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Resource resource = projectFileService.downloadFile(fileId, branchId, currentUserId);
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
    @Operation(summary = "Batch download")
    public ResponseEntity<Resource> downloadBatch(@RequestBody(required = false) ProjectFileBatchDownloadRequest body,
                                                  @RequestParam(value = "branchId", required = false) Long branchId,
                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Long projectId = body == null ? null : body.getProjectId();
        List<Long> fileIds = body == null ? null : body.getFileIds();
        Resource resource = projectFileService.downloadFiles(projectId, branchId, fileIds, currentUserId);

        if (projectId != null) {
            List<ProjectFileVO> visibleFiles = projectFileService.listFiles(projectId, branchId, currentUserId);
            Set<Long> selected = fileIds == null
                    ? Set.of()
                    : fileIds.stream().filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            for (ProjectFileVO item : visibleFiles) {
                if (item == null || !StringUtils.hasText(item.getFilePath())) {
                    continue;
                }
                if (!selected.isEmpty() && !selected.contains(item.getId())) {
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
    @Operation(summary = "Set main file (default branch only)")
    public ResponseEntity<ApiResponse<ProjectFileVO>> setMainFile(@PathVariable Long fileId,
                                                                  @RequestParam("branchId") Long branchId,
                                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectFileVO result = projectFileService.setMainFile(fileId, branchId, currentUserId);
        if (result != null) {
            projectActivityLogService.record(result.getProjectId(), currentUserId, "set_main_file", "file", result.getId(),
                    "Set main file: " + result.getFileName());
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete file (stage delete)")
    public ResponseEntity<ApiResponse<ProjectWorkspaceItemVO>> deleteFile(@PathVariable Long fileId,
                                                                          @RequestParam("branchId") Long branchId,
                                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectFile file = projectFileRepository.findById(fileId).orElse(null);
        Long projectId = file == null ? null : file.getProjectId();
        String fileName = file == null ? "" : file.getFileName();
        ProjectWorkspaceItemVO result = projectFileService.deleteFile(fileId, branchId, currentUserId);
        if (projectId != null) {
            String canonicalPath = result == null ? null : result.getCanonicalPath();
            projectActivityLogService.record(projectId, currentUserId, "stage_delete_file", "workspace_item",
                    result == null ? null : result.getId(),
                    "Delete staged: " + (StringUtils.hasText(canonicalPath) ? canonicalPath : fileName));
        }
        return ResponseEntity.ok(ApiResponse.ok("Delete request staged. Please commit in the target branch.", result));
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
}
