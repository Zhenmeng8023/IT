package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/repo")
@Tag(name = "项目仓库模块")
public class ProjectCodeRepositoryController {

    private final ProjectCodeRepositoryService projectCodeRepositoryService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectCodeRepositoryController(ProjectCodeRepositoryService projectCodeRepositoryService,
                                           CurrentUserProvider currentUserProvider) {
        this.projectCodeRepositoryService = projectCodeRepositoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/init")
    @Operation(summary = "初始化项目仓库")
    public ResponseEntity<ApiResponse<?>> init(@RequestParam Long projectId, HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCodeRepositoryService.initRepository(projectId, currentUserId)));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取项目仓库详情")
    public ResponseEntity<ApiResponse<?>> detail(@RequestParam Long projectId,
                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectCodeRepositoryService.getByProjectId(projectId, currentUserId)));
    }
}
