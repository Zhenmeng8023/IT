package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectDocCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectDocUpdateRequest;
import com.alikeyou.itmoduleproject.service.ProjectDocService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectDocListItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVO;
import com.alikeyou.itmoduleproject.vo.ProjectDocVersionVO;
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
@Tag(name = "项目文档中心")
public class ProjectDocController {

    private final ProjectDocService projectDocService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{projectId}/docs")
    @Operation(summary = "项目文档列表")
    public ResponseEntity<ApiResponse<List<ProjectDocListItemVO>>> listDocs(@PathVariable Long projectId,
                                                                            @RequestParam(required = false) String type,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) String visibility,
                                                                            @RequestParam(required = false) String isPrimary,
                                                                            HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.listDocs(projectId, type, keyword, status, visibility, isPrimary, currentUserId)));
    }

    @GetMapping("/{projectId}/docs/sidebar")
    @Operation(summary = "项目文档侧边栏列表")
    public ResponseEntity<ApiResponse<List<ProjectDocListItemVO>>> listSidebarDocs(@PathVariable Long projectId,
                                                                                   HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.listSidebarDocs(projectId, currentUserId)));
    }

    @GetMapping("/{projectId}/docs/primary-readme")
    @Operation(summary = "获取项目主 README 文档")
    public ResponseEntity<ApiResponse<ProjectDocVO>> getPrimaryReadme(@PathVariable Long projectId,
                                                                      HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.getPrimaryReadmeDoc(projectId, currentUserId)));
    }

    @PostMapping("/{projectId}/docs")
    @Operation(summary = "创建项目文档")
    public ResponseEntity<ApiResponse<ProjectDocVO>> createDoc(@PathVariable Long projectId,
                                                               @RequestBody ProjectDocCreateRequest body,
                                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.createDoc(projectId, body, currentUserId)));
    }

    @GetMapping("/docs/{docId}")
    @Operation(summary = "获取项目文档详情")
    public ResponseEntity<ApiResponse<ProjectDocVO>> getDoc(@PathVariable Long docId,
                                                            HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.getDoc(docId, currentUserId)));
    }

    @PutMapping("/docs/{docId}")
    @Operation(summary = "更新项目文档")
    public ResponseEntity<ApiResponse<ProjectDocVO>> updateDoc(@PathVariable Long docId,
                                                               @RequestBody ProjectDocUpdateRequest body,
                                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.updateDoc(docId, body, currentUserId)));
    }

    @PutMapping("/docs/{docId}/primary")
    @Operation(summary = "设置主 README 文档")
    public ResponseEntity<ApiResponse<ProjectDocVO>> setPrimaryDoc(@PathVariable Long docId,
                                                                   HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.setPrimaryDoc(docId, currentUserId)));
    }

    @DeleteMapping("/docs/{docId}")
    @Operation(summary = "删除项目文档")
    public ResponseEntity<ApiResponse<Object>> deleteDoc(@PathVariable Long docId,
                                                         HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectDocService.deleteDoc(docId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @GetMapping("/docs/{docId}/versions")
    @Operation(summary = "项目文档版本列表")
    public ResponseEntity<ApiResponse<List<ProjectDocVersionVO>>> listVersions(@PathVariable Long docId,
                                                                               HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.listVersions(docId, currentUserId)));
    }

    @GetMapping("/docs/{docId}/versions/{versionNo}")
    @Operation(summary = "项目文档版本详情")
    public ResponseEntity<ApiResponse<ProjectDocVersionVO>> getVersion(@PathVariable Long docId,
                                                                       @PathVariable Integer versionNo,
                                                                       HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.getVersion(docId, versionNo, currentUserId)));
    }

    @PostMapping("/docs/{docId}/rollback/{versionNo}")
    @Operation(summary = "回滚项目文档版本")
    public ResponseEntity<ApiResponse<ProjectDocVO>> rollback(@PathVariable Long docId,
                                                              @PathVariable Integer versionNo,
                                                              HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectDocService.rollback(docId, versionNo, currentUserId)));
    }
}
