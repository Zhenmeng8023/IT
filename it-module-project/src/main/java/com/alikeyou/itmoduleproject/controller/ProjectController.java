/**
 * 项目控制器
 * 处理项目相关的HTTP请求
 */
package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 项目控制器
 * 处理项目相关的HTTP请求
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目模块")
public class ProjectController {

    /**
     * 项目服务
     */
    private final ProjectService projectService;
    /**
     * 当前用户提供者
     */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 创建项目
     * @param request 创建项目请求参数
     * @param httpServletRequest HTTP请求对象
     * @return 项目详情响应
     */
    @PostMapping
    @Operation(summary = "创建项目")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> createProject(@RequestBody ProjectCreateRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.createProject(request, currentUserId)));
    }

    /**
     * 修改项目
     * @param id 项目ID
     * @param request 修改项目请求参数
     * @param httpServletRequest HTTP请求对象
     * @return 项目详情响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改项目")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> updateProject(@PathVariable Long id,
                                                                      @RequestBody ProjectUpdateRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.updateProject(id, request, currentUserId)));
    }

    /**
     * 获取项目详情
     * @param id 项目ID
     * @param httpServletRequest HTTP请求对象
     * @return 项目详情响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> getProjectDetail(@PathVariable Long id,
                                                                         HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectDetail(id, currentUserId)));
    }

    /**
     * 获取项目分页列表
     * @param keyword 搜索关键词
     * @param status 项目状态
     * @param authorId 作者ID
     * @param page 当前页码
     * @param size 每页大小
     * @return 项目分页列表响应
     */
    @GetMapping("/page")
    @Operation(summary = "项目分页列表")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> pageProjects(@RequestParam(required = false) String keyword,
                                                                                @RequestParam(required = false) String status,
                                                                                @RequestParam(required = false) Long authorId,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.pageProjects(keyword, status, authorId, page, size)));
    }

    /**
     * 获取我的项目列表
     * @param page 当前页码
     * @param size 每页大小
     * @param httpServletRequest HTTP请求对象
     * @return 项目分页列表响应
     */
    @GetMapping("/my")
    @Operation(summary = "我的项目")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> myProjects(@RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size,
                                                                             HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserId(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.pageMyProjects(currentUserId, page, size)));
    }

    /**
     * 删除项目
     * @param id 项目ID
     * @param httpServletRequest HTTP请求对象
     * @return 删除成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id,
                                                           HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserId(httpServletRequest);
        projectService.deleteProject(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}