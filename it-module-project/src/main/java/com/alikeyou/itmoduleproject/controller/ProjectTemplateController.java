package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectTemplateApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateSaveRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTemplateUpdateRequest;
import com.alikeyou.itmoduleproject.service.ProjectTemplateService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateSourceVO;
import com.alikeyou.itmoduleproject.vo.ProjectTemplateVO;
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
@Tag(name = "项目模板")
public class ProjectTemplateController {

    private final ProjectTemplateService projectTemplateService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/templates")
    @Operation(summary = "模板列表")
    public ResponseEntity<ApiResponse<List<ProjectTemplateVO>>> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean mineOnly,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.listTemplates(keyword, mineOnly, currentUserId)));
    }

    @GetMapping("/templates/{templateId}")
    @Operation(summary = "模板详情")
    public ResponseEntity<ApiResponse<ProjectTemplateDetailVO>> getTemplateDetail(
            @PathVariable Long templateId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.getTemplateDetail(templateId, currentUserId)));
    }

    @GetMapping("/{projectId}/template-source")
    @Operation(summary = "获取保存模板时可选择的项目源内容")
    public ResponseEntity<ApiResponse<ProjectTemplateSourceVO>> getTemplateSource(
            @PathVariable Long projectId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.getTemplateSource(projectId, currentUserId)));
    }

    @PostMapping("/templates")
    @Operation(summary = "创建模板")
    public ResponseEntity<ApiResponse<ProjectTemplateVO>> createTemplate(
            @RequestBody ProjectTemplateCreateRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.createTemplate(requestBody, currentUserId)));
    }

    @PutMapping("/templates/{templateId}")
    @Operation(summary = "编辑模板")
    public ResponseEntity<ApiResponse<ProjectTemplateVO>> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody ProjectTemplateUpdateRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.updateTemplate(templateId, requestBody, currentUserId)));
    }

    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "删除模板")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long templateId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectTemplateService.deleteTemplate(templateId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @PostMapping("/templates/{templateId}/apply")
    @Operation(summary = "套用模板创建项目")
    public ResponseEntity<ApiResponse<ProjectDetailVO>> applyTemplate(
            @PathVariable Long templateId,
            @RequestBody(required = false) ProjectTemplateApplyRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.applyTemplate(templateId, requestBody, currentUserId)));
    }

    @PostMapping("/{projectId}/save-as-template")
    @Operation(summary = "保存当前项目为模板")
    public ResponseEntity<ApiResponse<ProjectTemplateVO>> saveProjectAsTemplate(
            @PathVariable Long projectId,
            @RequestBody ProjectTemplateSaveRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectTemplateService.saveProjectAsTemplate(projectId, requestBody, currentUserId)));
    }
}
