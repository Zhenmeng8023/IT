package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.service.ProjectMilestoneService;
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
@RequestMapping("/api/project/milestone")
@RequiredArgsConstructor
@Tag(name = "项目里程碑")
public class ProjectMilestoneController {

    private final ProjectMilestoneService projectMilestoneService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/list")
    @Operation(summary = "里程碑列表")
    public ResponseEntity<ApiResponse<List<ProjectMilestone>>> list(@RequestParam Long projectId,
                                                                    @RequestParam(required = false) String status,
                                                                    HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMilestoneService.listMilestones(projectId, status, currentUserId)));
    }

    @GetMapping("/overview")
    @Operation(summary = "里程碑概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(@RequestParam Long projectId,
                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMilestoneService.getOverview(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "创建里程碑")
    public ResponseEntity<ApiResponse<ProjectMilestone>> create(@RequestBody ProjectMilestone request,
                                                                HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMilestoneService.createMilestone(request, currentUserId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新里程碑")
    public ResponseEntity<ApiResponse<ProjectMilestone>> update(@PathVariable Long id,
                                                                @RequestBody ProjectMilestone request,
                                                                HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMilestoneService.updateMilestone(id, request, currentUserId)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新里程碑状态")
    public ResponseEntity<ApiResponse<ProjectMilestone>> changeStatus(@PathVariable Long id,
                                                                      @RequestParam String status,
                                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectMilestoneService.changeStatus(id, status, currentUserId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除里程碑")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id,
                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        projectMilestoneService.deleteMilestone(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}
