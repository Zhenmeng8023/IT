package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.entity.ProjectReleaseFile;
import com.alikeyou.itmoduleproject.service.ProjectReleaseService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project/release")
@RequiredArgsConstructor
@Tag(name = "项目发布")
public class ProjectReleaseController {

    private final ProjectReleaseService projectReleaseService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/list")
    @Operation(summary = "发布列表")
    public ResponseEntity<ApiResponse<List<ProjectRelease>>> list(@RequestParam Long projectId,
                                                                  @RequestParam(required = false) String status,
                                                                  HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.listReleases(projectId, status, currentUserId)));
    }

    @GetMapping("/detail")
    @Operation(summary = "发布详情")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam Long id,
                                                                   HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.getReleaseDetail(id, currentUserId)));
    }

    @GetMapping("/latest")
    @Operation(summary = "最新发布摘要")
    public ResponseEntity<ApiResponse<Map<String, Object>>> latest(@RequestParam Long projectId,
                                                                   HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.getLatestReleaseSummary(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "创建发布")
    public ResponseEntity<ApiResponse<ProjectRelease>> create(@RequestBody ProjectRelease request,
                                                              HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.createRelease(request, currentUserId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新发布")
    public ResponseEntity<ApiResponse<ProjectRelease>> update(@PathVariable Long id,
                                                              @RequestBody ProjectRelease request,
                                                              HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.updateRelease(id, request, currentUserId)));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布版本")
    public ResponseEntity<ApiResponse<ProjectRelease>> publish(@PathVariable Long id,
                                                               HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.publishRelease(id, currentUserId)));
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "归档版本")
    public ResponseEntity<ApiResponse<ProjectRelease>> archive(@PathVariable Long id,
                                                               HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.archiveRelease(id, currentUserId)));
    }

    @PostMapping("/{id}/bind-files")
    @Operation(summary = "绑定项目文件到发布")
    public ResponseEntity<ApiResponse<List<ProjectReleaseFile>>> bindFiles(@PathVariable Long id,
                                                                           @RequestBody List<Long> projectFileIds,
                                                                           HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectReleaseService.bindProjectFiles(id, projectFileIds, currentUserId)));
    }

    @DeleteMapping("/{id}/file/{releaseFileId}")
    @Operation(summary = "移除发布文件")
    public ResponseEntity<ApiResponse<Object>> removeFile(@PathVariable Long id,
                                                          @PathVariable Long releaseFileId,
                                                          HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        projectReleaseService.removeReleaseFile(id, releaseFileId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("移除成功", null));
    }
}
