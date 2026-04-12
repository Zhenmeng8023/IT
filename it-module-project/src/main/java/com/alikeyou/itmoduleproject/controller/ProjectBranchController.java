package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectBranchCreateRequest;
import com.alikeyou.itmoduleproject.service.ProjectBranchService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/branch")
@Tag(name = "项目分支模块")
public class ProjectBranchController {

    private final ProjectBranchService projectBranchService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectBranchController(ProjectBranchService projectBranchService,
                                   CurrentUserProvider currentUserProvider) {
        this.projectBranchService = projectBranchService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/list")
    @Operation(summary = "分支列表")
    public ResponseEntity<ApiResponse<?>> list(@RequestParam Long projectId,
                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectBranchService.listByProjectId(projectId, currentUserId)));
    }

    @PostMapping("/create")
    @Operation(summary = "创建分支")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody ProjectBranchCreateRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectBranchService.create(request, currentUserId)));
    }

    @PutMapping("/{branchId}/protect")
    @Operation(summary = "设置分支保护")
    public ResponseEntity<ApiResponse<?>> protect(@PathVariable Long branchId,
                                                  @RequestParam(required = false) Boolean protectedFlag,
                                                  @RequestParam(required = false) Boolean allowDirectCommitFlag,
                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectBranchService.updateProtection(branchId, protectedFlag, allowDirectCommitFlag, currentUserId)));
    }
}
