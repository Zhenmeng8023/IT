package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectCheckRunRequest;
import com.alikeyou.itmoduleproject.service.ProjectCheckRunService;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project/check")
@Tag(name = "项目检查模块")
public class ProjectCheckController {

    private final ProjectCheckRunService projectCheckRunService;

    public ProjectCheckController(ProjectCheckRunService projectCheckRunService) {
        this.projectCheckRunService = projectCheckRunService;
    }

    @PostMapping("/run")
    @Operation(summary = "运行检查")
    public ResponseEntity<ApiResponse<?>> run(@RequestBody ProjectCheckRunRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(projectCheckRunService.run(request)));
    }

    @GetMapping("/list-by-commit")
    @Operation(summary = "按提交查询检查")
    public ResponseEntity<ApiResponse<?>> listByCommit(@RequestParam Long commitId) {
        return ResponseEntity.ok(ApiResponse.ok(projectCheckRunService.listByCommit(commitId)));
    }

    @GetMapping("/list-by-mr")
    @Operation(summary = "按合并请求查询检查")
    public ResponseEntity<ApiResponse<?>> listByMr(@RequestParam Long mergeRequestId) {
        return ResponseEntity.ok(ApiResponse.ok(projectCheckRunService.listByMergeRequest(mergeRequestId)));
    }
}
