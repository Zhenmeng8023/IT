package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.service.ProjectMergeRequestService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/mr")
@Tag(name = "项目合并请求模块")
public class ProjectMergeRequestController {

    private final ProjectMergeRequestService projectMergeRequestService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectMergeRequestController(ProjectMergeRequestService projectMergeRequestService,
                                         CurrentUserProvider currentUserProvider) {
        this.projectMergeRequestService = projectMergeRequestService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/create")
    @Operation(summary = "创建合并请求")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody ProjectMergeRequestCreateRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.create(request, currentUserId)));
    }

    @GetMapping("/list")
    @Operation(summary = "合并请求列表")
    public ResponseEntity<ApiResponse<?>> list(@RequestParam Long projectId,
                                               @RequestParam(required = false) String status,
                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.list(projectId, status, currentUserId)));
    }

    @PostMapping("/{mergeRequestId}/review")
    @Operation(summary = "提交评审")
    public ResponseEntity<ApiResponse<?>> review(@PathVariable Long mergeRequestId,
                                                 @RequestBody ProjectReviewSubmitRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.review(mergeRequestId, request, currentUserId)));
    }

    @GetMapping("/{mergeRequestId}/merge-check")
    @Operation(summary = "Merge pre-check")
    public ResponseEntity<ApiResponse<?>> checkMerge(@PathVariable Long mergeRequestId,
                                                     HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.checkMerge(mergeRequestId, currentUserId)));
    }

    @GetMapping("/{mergeRequestId}/merge-check/latest")
    @Operation(summary = "Get latest merge conflict details")
    public ResponseEntity<ApiResponse<?>> latestMergeCheck(@PathVariable Long mergeRequestId,
                                                           HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.latestMergeCheck(mergeRequestId, currentUserId)));
    }

    @PostMapping("/{mergeRequestId}/merge-check/recheck")
    @Operation(summary = "Re-run merge conflict detection")
    public ResponseEntity<ApiResponse<?>> recheckMerge(@PathVariable Long mergeRequestId,
                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.recheckMerge(mergeRequestId, currentUserId)));
    }

    @GetMapping("/{mergeRequestId}/pre-merge-check")
    @Operation(summary = "Pre-merge gate check")
    public ResponseEntity<ApiResponse<?>> preMergeCheck(@PathVariable Long mergeRequestId,
                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.preMergeCheck(mergeRequestId, currentUserId)));
    }

    @PostMapping("/{mergeRequestId}/merge")
    @Operation(summary = "合并请求")
    public ResponseEntity<ApiResponse<?>> merge(@PathVariable Long mergeRequestId,
                                                HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMergeRequestService.merge(mergeRequestId, currentUserId)));
    }
}
