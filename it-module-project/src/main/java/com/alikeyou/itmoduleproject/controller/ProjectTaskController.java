package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/task")
@RequiredArgsConstructor
@Tag(name = "项目任务模块")
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public ResponseEntity<ApiResponse<List<ProjectTaskVO>>> listTasks(@RequestParam Long projectId,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) String priority,
                                                                      @RequestParam(required = false) Long assigneeId,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listTasks(projectId, status, priority, assigneeId, currentUserId)));
    }

    @GetMapping("/my")
    @Operation(summary = "我的任务")
    public ResponseEntity<ApiResponse<List<ProjectTaskVO>>> myTasks(@RequestParam Long projectId,
                                                                    HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listMyTasks(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "创建任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> createTask(@RequestBody ProjectTaskCreateRequest requestBody,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.createTask(requestBody, currentUserId)));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "修改任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTask(@PathVariable Long taskId,
                                                                 @RequestBody ProjectTaskUpdateRequest requestBody,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTask(taskId, requestBody, currentUserId)));
    }

    @PutMapping("/{taskId}/status")
    @Operation(summary = "修改任务状态")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTaskStatus(@PathVariable Long taskId,
                                                                       @RequestBody ProjectTaskStatusUpdateRequest requestBody,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTaskStatus(taskId, requestBody, currentUserId)));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId,
                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTaskService.deleteTask(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}
