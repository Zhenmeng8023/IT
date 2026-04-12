package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectCommitService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/commit")
@Tag(name = "项目提交模块")
public class ProjectCommitController {

    private final ProjectCommitService projectCommitService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectCommitController(ProjectCommitService projectCommitService,
                                   CurrentUserProvider currentUserProvider) {
        this.projectCommitService = projectCommitService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/list")
    @Operation(summary = "提交列表")
    public ResponseEntity<ApiResponse<?>> list(@RequestParam Long projectId,
                                               @RequestParam Long branchId,
                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCommitService.listByBranch(projectId, branchId, currentUserId)));
    }

    @GetMapping("/detail")
    @Operation(summary = "提交详情")
    public ResponseEntity<ApiResponse<?>> detail(@RequestParam Long commitId,
                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCommitService.detail(commitId, currentUserId)));
    }

    @GetMapping("/compare")
    @Operation(summary = "提交比较")
    public ResponseEntity<ApiResponse<?>> compare(@RequestParam Long fromCommitId,
                                                  @RequestParam Long toCommitId,
                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCommitService.compare(fromCommitId, toCommitId, currentUserId)));
    }

    @PostMapping("/rollback-to/{commitId}")
    @Operation(summary = "回退到指定提交快照")
    public ResponseEntity<ApiResponse<?>> rollbackTo(@PathVariable Long commitId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCommitService.rollbackToCommit(commitId, currentUserId)));
    }
}
