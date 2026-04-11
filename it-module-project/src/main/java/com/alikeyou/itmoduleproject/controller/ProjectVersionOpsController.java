package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectReleaseCreateFromCommitRequest;
import com.alikeyou.itmoduleproject.service.ProjectVersionOpsService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/version")
@Tag(name = "项目版本运营模块")
public class ProjectVersionOpsController {

    private final ProjectVersionOpsService projectVersionOpsService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectVersionOpsController(ProjectVersionOpsService projectVersionOpsService,
                                       CurrentUserProvider currentUserProvider) {
        this.projectVersionOpsService = projectVersionOpsService;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/milestone/{milestoneId}/bind-commit")
    @Operation(summary = "把里程碑绑定到指定提交")
    public ResponseEntity<ApiResponse<?>> bindMilestone(@PathVariable Long milestoneId,
                                                        @RequestParam Long commitId,
                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectVersionOpsService.bindMilestoneCommit(milestoneId, commitId, currentUserId)));
    }

    @PostMapping("/release/from-commit")
    @Operation(summary = "从提交创建发布记录")
    public ResponseEntity<ApiResponse<?>> createReleaseFromCommit(@RequestBody ProjectReleaseCreateFromCommitRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectVersionOpsService.createReleaseFromCommit(request, currentUserId)));
    }
}
