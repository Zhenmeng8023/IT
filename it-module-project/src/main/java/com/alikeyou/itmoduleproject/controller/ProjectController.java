package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目模块")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectActivityLogService projectActivityLogService;

    @PostMapping
    @Operation(summary = "创建项目")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> createProject(@RequestBody ProjectCreateRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        ProjectDetailVO result = projectService.createProject(request, currentUserId);
        if (result != null) {
            projectActivityLogService.record(result.getId(), currentUserId, "create_project", "project", result.getId(), "创建项目：" + result.getName());
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改项目")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> updateProject(@PathVariable Long id,
                                                                      @RequestBody ProjectUpdateRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        ProjectDetailVO result = projectService.updateProject(id, request, currentUserId);
        projectActivityLogService.record(id, currentUserId, "update_project", "project", id, "编辑项目：" + (result == null ? id : result.getName()));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> getProjectDetail(@PathVariable Long id,
                                                                         HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectDetail(id, currentUserId)));
    }

    @GetMapping("/{id}/contributors")
    @Operation(summary = "项目贡献者列表")
    public ResponseEntity<ApiResponse<List<ProjectMemberVO>>> getProjectContributors(@PathVariable Long id,
                                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.listProjectContributors(id, currentUserId)));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "相关项目推荐")
    public ResponseEntity<ApiResponse<List<ProjectListVO>>> getRelatedProjects(@PathVariable Long id,
                                                                               @RequestParam(defaultValue = "6") int size,
                                                                               HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.listRelatedProjects(id, currentUserId, size)));
    }

    @GetMapping("/page")
    @Operation(summary = "项目分页列表")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> pageProjects(@RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) String status,
                                                                               @RequestParam(required = false) Long authorId,
                                                                               @RequestParam(required = false) String visibility,
                                                                               @RequestParam(required = false) String category,
                                                                               @RequestParam(required = false) String tag,
                                                                               @RequestParam(required = false, defaultValue = "latest") String sortBy,
                                                                               @RequestParam(defaultValue = "1") int page,
                                                                               @RequestParam(defaultValue = "10") int size,
                                                                               HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.pageProjects(
                keyword, status, authorId, visibility, category, tag, sortBy, currentUserId, page, size
        )));
    }

    @GetMapping("/my")
    @Operation(summary = "我的项目")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> myProjects(@RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size,
                                                                             HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.pageMyProjects(currentUserId, page, size)));
    }

    @GetMapping("/participated")
    @Operation(summary = "我参与的项目")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> participatedProjects(@RequestParam(defaultValue = "1") int page,
                                                                                       @RequestParam(defaultValue = "10") int size,
                                                                                       HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectService.pageParticipatedProjects(currentUserId, page, size)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id,
                                                           HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        projectService.deleteProject(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }
}
