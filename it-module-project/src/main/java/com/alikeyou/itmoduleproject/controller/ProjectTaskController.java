/**
 * 项目任务控制器
 * 处理项目任务相关的HTTP请求
 */
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

/**
 * 项目任务控制器
 * 处理项目任务相关的HTTP请求
 */
@RestController
@RequestMapping("/api/project/task")
@RequiredArgsConstructor
@Tag(name = "项目任务模块")
public class ProjectTaskController {

    /**
     * 项目任务服务
     */
    private final ProjectTaskService projectTaskService;
    /**
     * 当前用户提供者
     */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 获取项目任务列表
     * @param projectId 项目ID
     * @param request HTTP请求对象
     * @return 任务列表响应
     */
    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public ResponseEntity<ApiResponse<List<ProjectTaskVO>>> listTasks(@RequestParam Long projectId,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.listTasks(projectId, currentUserId)));
    }

    /**
     * 创建项目任务
     * @param requestBody 创建任务请求参数
     * @param request HTTP请求对象
     * @return 任务信息响应
     */
    @PostMapping
    @Operation(summary = "创建任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> createTask(@RequestBody ProjectTaskCreateRequest requestBody,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.createTask(requestBody, currentUserId)));
    }

    /**
     * 修改项目任务
     * @param taskId 任务ID
     * @param requestBody 修改任务请求参数
     * @param request HTTP请求对象
     * @return 任务信息响应
     */
    @PutMapping("/{taskId}")
    @Operation(summary = "修改任务")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTask(@PathVariable Long taskId,
                                                                 @RequestBody ProjectTaskUpdateRequest requestBody,
                                                                 HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTask(taskId, requestBody, currentUserId)));
    }

    /**
     * 修改任务状态
     * @param taskId 任务ID
     * @param requestBody 修改状态请求参数
     * @param request HTTP请求对象
     * @return 任务信息响应
     */
    @PutMapping("/{taskId}/status")
    @Operation(summary = "修改任务状态")
    public ResponseEntity<ApiResponse<ProjectTaskVO>> updateTaskStatus(@PathVariable Long taskId,
                                                                       @RequestBody ProjectTaskStatusUpdateRequest requestBody,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTaskService.updateTaskStatus(taskId, requestBody, currentUserId)));
    }

    /**
     * 删除项目任务
     * @param taskId 任务ID
     * @param request HTTP请求对象
     * @return 删除成功响应
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId,
                                                        HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        projectTaskService.deleteTask(taskId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}