package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectActivityPositionVO;
import com.alikeyou.itmoduleproject.vo.ProjectActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "项目活动流")
public class ProjectActivityController {

    private final ProjectActivityLogService projectActivityLogService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{projectId}/activities")
    @Operation(summary = "项目活动流分页列表")
    public ResponseEntity<ApiResponse<PageResult<ProjectActivityVO>>> pageActivities(
            @PathVariable Long projectId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        PageResult<ProjectActivityVO> result = projectActivityLogService.pageActivities(
                projectId,
                action,
                targetType,
                operatorId,
                startTime,
                endTime,
                page == null || page < 1 ? 1 : page,
                size == null || size < 1 ? 20 : size,
                currentUserId
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{projectId}/activities/{activityId}/position")
    @Operation(summary = "获取活动流在分页中的位置")
    public ResponseEntity<ApiResponse<ProjectActivityPositionVO>> getActivityPosition(
            @PathVariable Long projectId,
            @PathVariable Long activityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectActivityPositionVO result = projectActivityLogService.getActivityPosition(
                projectId,
                activityId,
                action,
                targetType,
                operatorId,
                startTime,
                endTime,
                size == null || size < 1 ? 10 : size,
                currentUserId
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
