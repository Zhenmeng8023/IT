package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectStarService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectStarStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project/star")
@RequiredArgsConstructor
@Tag(name = "项目收藏")
public class ProjectStarController {

    private final ProjectStarService projectStarService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "收藏项目")
    public ResponseEntity<ApiResponse<ProjectStarStatusVO>> starProject(@RequestParam Long projectId,
                                                                        HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStarService.starProject(projectId, currentUserId)));
    }

    @DeleteMapping
    @Operation(summary = "取消收藏项目")
    public ResponseEntity<ApiResponse<ProjectStarStatusVO>> unstarProject(@RequestParam Long projectId,
                                                                          HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStarService.unstarProject(projectId, currentUserId)));
    }

    @GetMapping("/my")
    @Operation(summary = "我的收藏项目")
    public ResponseEntity<ApiResponse<PageResult<ProjectListVO>>> myStarredProjects(@RequestParam(defaultValue = "1") int page,
                                                                                     @RequestParam(defaultValue = "10") int size,
                                                                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectStarService.pageMyStarredProjects(currentUserId, page, size)));
    }
}
