package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.service.ProjectRecommendationService;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.AdminProjectRecommendationPageItemVO;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectRecommendationResultVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects/recommendations")
@RequiredArgsConstructor
public class AdminProjectRecommendationController {

    private final ProjectService projectService;
    private final ProjectRecommendationService projectRecommendationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<AdminProjectRecommendationPageItemVO>>> pageRecommendationTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "6") int recommendSize,
            HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        PageResult<ProjectListVO> projectPage = projectService.pageProjects(
                keyword,
                status,
                null,
                null,
                category,
                null,
                "latest",
                currentUserId,
                page,
                size
        );

        List<AdminProjectRecommendationPageItemVO> rows = projectPage.getList().stream()
                .map(project -> toTaskRow(project, currentUserId, recommendSize))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new PageResult<>(rows, projectPage.getTotal(), projectPage.getPage(), projectPage.getSize())));
    }

    private AdminProjectRecommendationPageItemVO toTaskRow(ProjectListVO project,
                                                           Long currentUserId,
                                                           int recommendSize) {
        AdminProjectRecommendationPageItemVO row = new AdminProjectRecommendationPageItemVO();
        row.setId(project.getId());
        row.setName(project.getName());
        row.setDescription(project.getDescription());
        row.setType(project.getCategory());
        row.setStatus(project.getStatus());
        row.setAuthorName(project.getAuthorName());
        row.setVisibility(project.getVisibility());
        row.setTags(project.getTags());
        row.setCreatedAt(project.getCreatedAt());
        row.setUpdatedAt(project.getUpdatedAt());
        row.setTask(buildTaskSnapshot(project.getId(), currentUserId, recommendSize));
        return row;
    }

    private AdminProjectRecommendationPageItemVO.TaskSnapshot buildTaskSnapshot(Long projectId,
                                                                                Long currentUserId,
                                                                                int recommendSize) {
        ProjectRecommendationResultVO cached = projectRecommendationService.getCachedRecommendation(projectId, currentUserId, recommendSize);
        AdminProjectRecommendationPageItemVO.TaskSnapshot task = new AdminProjectRecommendationPageItemVO.TaskSnapshot();
        if (cached == null) {
            task.setStatus("idle");
            task.setResultTotal(0);
            return task;
        }

        int resultTotal = cached.getTotal() == null ? (cached.getItems() == null ? 0 : cached.getItems().size()) : cached.getTotal();
        task.setStatus(resultTotal > 0 ? "success" : "empty");
        task.setSource(cached.getSource());
        task.setAlgorithmVersion(cached.getAlgorithmVersion());
        task.setGeneratedAt(cached.getGeneratedAt());
        task.setLastRunAt(cached.getGeneratedAt());
        task.setResultTotal(resultTotal);
        task.setErrorMessage("");
        return task;
    }
}
