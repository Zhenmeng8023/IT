package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectWorkspaceCommitRequest;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/project/workspace")
@Tag(name = "项目工作区模块")
public class ProjectWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkspaceController.class);

    private final ProjectWorkspaceService projectWorkspaceService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectWorkspaceController(ProjectWorkspaceService projectWorkspaceService,
                                      CurrentUserProvider currentUserProvider) {
        this.projectWorkspaceService = projectWorkspaceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/current")
    @Operation(summary = "当前工作区")
    public ResponseEntity<ApiResponse<?>> current(@RequestParam Long projectId,
                                                  @RequestParam Long branchId,
                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectWorkspaceService.getCurrentWorkspace(projectId, branchId, currentUserId)));
    }

    @GetMapping("/items")
    @Operation(summary = "工作区文件列表")
    public ResponseEntity<ApiResponse<?>> items(@RequestParam Long projectId,
                                                @RequestParam Long branchId,
                                                HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectWorkspaceService.listItems(projectId, branchId, currentUserId)));
    }

    @PostMapping(value = "/stage-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "暂存文件到工作区")
    public ResponseEntity<ApiResponse<?>> stageFile(@RequestParam Long projectId,
                                                    @RequestParam Long branchId,
                                                    @RequestParam String canonicalPath,
                                                    @RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        log.info("[project-workspace-upload] stage-file begin projectId={} branchId={} userId={} canonicalPath={} file={}",
                projectId, branchId, currentUserId, canonicalPath, describeFile(file));
        Object result = projectWorkspaceService.stageFile(projectId, branchId, currentUserId, canonicalPath, file);
        log.info("[project-workspace-upload] stage-file ok projectId={} branchId={} userId={} canonicalPath={}",
                projectId, branchId, currentUserId, canonicalPath);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping(value = "/stage-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量暂存文件到工作区")
    public ResponseEntity<ApiResponse<?>> stageBatch(@RequestParam Long projectId,
                                                     @RequestParam Long branchId,
                                                     @RequestParam(value = "targetDir", required = false) String targetDir,
                                                     @RequestParam("files") List<MultipartFile> files,
                                                     @RequestParam(value = "relativePaths", required = false) List<String> relativePaths,
                                                     HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        log.info("[project-workspace-upload] stage-batch begin projectId={} branchId={} userId={} targetDir={} fileCount={} relativePathCount={} files={}",
                projectId, branchId, currentUserId, targetDir, sizeOf(files), sizeOf(relativePaths), describeFiles(files));
        Object result = projectWorkspaceService.stageFiles(projectId, branchId, currentUserId, targetDir, files, relativePaths);
        log.info("[project-workspace-upload] stage-batch ok projectId={} branchId={} userId={} result={}",
                projectId, branchId, currentUserId, result);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping(value = "/stage-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传 ZIP 并暂存到工作区")
    public ResponseEntity<ApiResponse<?>> stageZip(@RequestParam Long projectId,
                                                   @RequestParam Long branchId,
                                                   @RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectWorkspaceService.stageZip(projectId, branchId, currentUserId, file)));
    }

    @PostMapping("/stage-delete")
    @Operation(summary = "暂存删除路径")
    public ResponseEntity<ApiResponse<?>> stageDelete(@RequestParam Long projectId,
                                                      @RequestParam Long branchId,
                                                      @RequestParam String canonicalPath,
                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectWorkspaceService.stageDelete(projectId, branchId, currentUserId, canonicalPath)));
    }

    @PostMapping("/commit")
    @Operation(summary = "提交工作区")
    public ResponseEntity<ApiResponse<?>> commit(@RequestBody ProjectWorkspaceCommitRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectWorkspaceService.commit(request.getProjectId(), request.getBranchId(), currentUserId, request.getMessage())));
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String describeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return "[]";
        }
        return files.stream()
                .limit(20)
                .map(this::describeFile)
                .toList()
                .toString();
    }

    private String describeFile(MultipartFile file) {
        if (file == null) {
            return "null";
        }
        return "{name=" + file.getOriginalFilename()
                + ", size=" + file.getSize()
                + ", empty=" + file.isEmpty()
                + ", contentType=" + file.getContentType()
                + "}";
    }
}
