package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.ProjectSprint;
import com.alikeyou.itmoduleproject.service.ProjectSprintService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/project/sprint")
@RequiredArgsConstructor
@Tag(name = "项目迭代")
public class ProjectSprintController {

    private final ProjectSprintService projectSprintService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/list")
    @Operation(summary = "迭代列表")
    public ResponseEntity<ApiResponse<List<ProjectSprint>>> list(@RequestParam Long projectId,
                                                                 @RequestParam(required = false) String status,
                                                                 HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectSprintService.listSprints(projectId, status, currentUserId)));
    }

    @GetMapping("/current")
    @Operation(summary = "当前迭代摘要")
    public ResponseEntity<ApiResponse<Map<String, Object>>> current(@RequestParam Long projectId,
                                                                    HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectSprintService.getCurrentSprintSummary(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "创建迭代")
    public ResponseEntity<ApiResponse<ProjectSprint>> create(@RequestBody ProjectSprint request,
                                                             HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectSprintService.createSprint(request, currentUserId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新迭代")
    public ResponseEntity<ApiResponse<ProjectSprint>> update(@PathVariable Long id,
                                                             @RequestBody ProjectSprint request,
                                                             HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectSprintService.updateSprint(id, request, currentUserId)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新迭代状态")
    public ResponseEntity<ApiResponse<ProjectSprint>> changeStatus(@PathVariable Long id,
                                                                   @RequestParam String status,
                                                                   HttpServletRequest httpServletRequest) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok(projectSprintService.changeStatus(id, status, currentUserId)));
    }
}
