package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectInvitationCreateRequest;
import com.alikeyou.itmoduleproject.service.ProjectInvitationService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/invitations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "项目邀请")
public class ProjectInvitationController {

    private final ProjectInvitationService projectInvitationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "发送项目邀请")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> createInvitation(@RequestBody ProjectInvitationCreateRequest requestBody,
                                                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInvitationService.createInvitation(requestBody, currentUserId)));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "项目邀请列表")
    public ResponseEntity<ApiResponse<List<ProjectInvitationVO>>> listProjectInvitations(@PathVariable Long projectId,
                                                                                         @RequestParam(required = false) String status,
                                                                                         HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInvitationService.listProjectInvitations(projectId, status, currentUserId)));
    }

    @GetMapping("/my/pending")
    @Operation(summary = "我的待处理邀请")
    public ResponseEntity<ApiResponse<List<ProjectInvitationVO>>> listMyPendingInvitations(HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInvitationService.listMyPendingInvitations(currentUserId)));
    }

    @PostMapping("/{invitationId}/accept")
    @Operation(summary = "接受邀请")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> acceptInvitation(@PathVariable Long invitationId,
                                                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInvitationService.acceptInvitation(invitationId, currentUserId)));
    }

    @PostMapping("/{invitationId}/reject")
    @Operation(summary = "拒绝邀请")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> rejectInvitation(@PathVariable Long invitationId,
                                                                             HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInvitationService.rejectInvitation(invitationId, currentUserId)));
    }

    @PostMapping("/{invitationId}/cancel")
    @Operation(summary = "撤销邀请")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(@PathVariable Long invitationId,
                                                              HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectInvitationService.cancelInvitation(invitationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("撤销成功", null));
    }
}
