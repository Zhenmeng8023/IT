
package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestAuditRequest;
import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestCreateRequest;
import com.alikeyou.itmoduleproject.service.ProjectJoinRequestService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectJoinRequestVO;
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
@Tag(name = "项目加入申请")
public class ProjectJoinRequestController {

    private final ProjectJoinRequestService projectJoinRequestService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{projectId}/join-requests")
    @Operation(summary = "提交加入申请")
    public ResponseEntity<ApiResponse<ProjectJoinRequestVO>> submitJoinRequest(
            @PathVariable Long projectId,
            @RequestBody(required = false) ProjectJoinRequestCreateRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectJoinRequestService.submitJoinRequest(projectId, requestBody, currentUserId)));
    }

    @GetMapping("/{projectId}/join-requests")
    @Operation(summary = "加入申请列表")
    public ResponseEntity<ApiResponse<List<ProjectJoinRequestVO>>> listJoinRequests(
            @PathVariable Long projectId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectJoinRequestService.listJoinRequests(projectId, currentUserId)));
    }

    @GetMapping("/{projectId}/join-requests/my-status")
    @Operation(summary = "我的申请状态")
    public ResponseEntity<ApiResponse<ProjectJoinRequestVO>> getMyJoinRequestStatus(
            @PathVariable Long projectId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectJoinRequestService.getMyJoinRequestStatus(projectId, currentUserId)));
    }

    @PutMapping("/join-requests/{requestId}/audit")
    @Operation(summary = "审批加入申请")
    public ResponseEntity<ApiResponse<ProjectJoinRequestVO>> auditJoinRequest(
            @PathVariable Long requestId,
            @RequestBody ProjectJoinRequestAuditRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectJoinRequestService.auditJoinRequest(requestId, requestBody, currentUserId)));
    }

    @PostMapping("/join-requests/{requestId}/cancel")
    @Operation(summary = "取消加入申请")
    public ResponseEntity<ApiResponse<Void>> cancelJoinRequest(
            @PathVariable Long requestId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectJoinRequestService.cancelJoinRequest(requestId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("取消成功", null));
    }
}
